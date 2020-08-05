package org.checkerframework.gradle.plugin

import org.gradle.api.Task
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.internal.artifacts.dependencies.DefaultSelfResolvingDependency
import org.gradle.util.GradleVersion

import java.util.jar.JarFile

import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.compile.AbstractCompile

import io.freefair.gradle.plugins.lombok.LombokPlugin

final class CheckerFrameworkPlugin implements Plugin<Project> {
  // Handles pre-3.0 and 3.0+, "com.android.base" was added in AGP 3.0
  private static final def ANDROID_IDS = [
    "com.android.application",
    "com.android.feature",
    "com.android.instantapp",
    "com.android.library",
    "com.android.test"]
  // Checker Framework configurations and dependencies

  // Whenever this line is changed, you need to change all occurrences in README.md.
  private final static def LIBRARY_VERSION = "3.6.0"

  private final static def ANNOTATED_JDK_NAME_JDK8 = "jdk8"
  private final static def ANNOTATED_JDK_CONFIGURATION = "checkerFrameworkAnnotatedJDK"
  private final static def ANNOTATED_JDK_CONFIGURATION_DESCRIPTION = "A copy of JDK classes with Checker Framework type qualifiers inserted."
  private final static def CONFIGURATION = "checkerFramework"
  private final static def CONFIGURATION_DESCRIPTION = "The Checker Framework: custom pluggable types for Java."
  private final static def JAVA_COMPILE_CONFIGURATION = "compileOnly"
  private final static def TEST_COMPILE_CONFIGURATION = "testCompileOnly"
  private final static def CHECKER_DEPENDENCY = "org.checkerframework:checker:${LIBRARY_VERSION}"
  private final static def CHECKER_QUAL_DEPENDENCY = "org.checkerframework:checker-qual:${LIBRARY_VERSION}"

  private final static Logger LOG = Logging.getLogger(CheckerFrameworkPlugin)

  private final static String checkerFrameworkManifestCreationTaskName = 'createCheckerFrameworkManifest'

  /**
   * Which subfolder of /build/ to put the Checker Framework manifest in.
   */
  private final static def manifestLocation = "/checkerframework/"

  /**
   * Configure each task in {@code project} with the given {@code taskType}.
   * <p>
   * We prefer to configure with {@link
   * org.gradle.api.tasks.TaskCollection#configureEach(org.gradle.api.Action)}
   * rather than {@link
   * org.gradle.api.tasks.TaskCollection#all(org.gradle.api.Action)}, but {@code
   * configureEach} is only available on Gradle 4.9 and newer, so this method
   * dynamically picks the better candidate based on the current Gradle version.
   * <p>
   * See also: <a href="https://docs.gradle.org/current/userguide/task_configuration_avoidance.html">
   * Gradle documentation: Task Configuration Avoidance</a>
   */
  private static <S extends Task> void configureTasks(Project project, Class<S> taskType, Action<? super S> configure) {
    // TODO: why does lazy configuration fail on Java 8 JVMs? https://github.com/typetools/checker-framework/pull/3557
    if (GradleVersion.current() < GradleVersion.version("4.9")
            || !JavaVersion.current().isJava9Compatible()) {
      project.tasks.withType(taskType).all configure
    } else {
      project.tasks.withType(taskType).configureEach configure
    }
  }

  @Override void apply(Project project) {
    // Either get an existing CF config, or create a new one if none exists
    CheckerFrameworkExtension userConfig = project.extensions.findByType(CheckerFrameworkExtension.class)?:
            project.extensions.create("checkerFramework", CheckerFrameworkExtension)
    boolean applied = false
    (ANDROID_IDS + "java").each { id ->
      project.pluginManager.withPlugin(id) {
        if (!applied) {
          LOG.info('Found plugin {}, applying checker compiler options.', id)
          configureProject(project, userConfig)
          applyToProject(project, userConfig)
          applied = true
        }
      }
    }

    if (!applied) {
      // Ensure that dependencies and configurations are available, even if no Java/Android plugins were found,
      // to support configuration in a super project with many Java/Android subprojects.
      configureProject(project, userConfig)
    }

    project.afterEvaluate {
      if (!applied) LOG.warn('No android or java plugins found in the project {}, checker compiler options will not be applied.', project.name)
    }
  }

  private static handleLombokPlugin(Project project, CheckerFrameworkExtension userConfig) {
    project.getPlugins().withType(io.freefair.gradle.plugins.lombok.LombokPlugin.class, new Action<LombokPlugin>() {
      void execute(LombokPlugin lombokPlugin) {

        // Ensure that the lombok config is set to emit @Generated annotations.
        lombokPlugin.configureForJacoco()
        // If config generation is enabled, automatically produce an appropriate config file.
        // The object construction checker (https://github.com/kelloggm/object-construction-checker),
        // if run, will not issue warnings on incorrect Lombok builders if this check fails.
        // As of 9/11/2019, that was the only known checker that relies on this behavior.
        def generateLombokConfig = lombokPlugin.generateLombokConfig.get()
        if (generateLombokConfig.isEnabled()) {
          generateLombokConfig.generateLombokConfig()
        } else if (userConfig.checkers.contains(
                'org.checkerframework.checker.objectconstruction.ObjectConstructionChecker')) {
          LOG.warn("The Object Construction Checker was enabled, but Lombok config generation is disabled. " +
                  "Ensure that your lombok.config file contains 'lombok.addLombokGeneratedAnnotation = true'," +
                  "or all Object Construction Checker warnings related to misuse of Lombok builders will" +
                  "be disabled.")
        }

        project.afterEvaluate {

          if (skipCheckerFramework(project, userConfig)) {
            return
          }

          def delombokTasks = project.getTasks().findAll { task ->
            task.name.contains("delombok")
          }

          if (delombokTasks.size() != 0) {

            // change every compile task so that it:
            // 1. depends on delombok, and
            // 2. uses the delombok'd source code as its source set

            project.tasks.withType(AbstractCompile).all { compile ->

              // find the right delombok task
              def delombokTask = delombokTasks.find { task ->

                if (task.name.endsWith("delombok")) {
                  // special-case the main compile task because its just named "compileJava"
                  // without anything else
                  compile.name.equals("compileJava")
                } else {
                  // "delombok" is 8 characters.
                  compile.name.contains(task.name.substring(8))
                }
              }

              // delombokTask can still be null; for example, if the code contains a compileScala task
              // Also, if we're skipping test tasks, don't bother delombok'ing them.
              if (delombokTask != null && !compile.getSource().isEmpty()
                      && !(userConfig.excludeTests && delombokTask.name.toLowerCase().contains("test"))) {

                // The lombok plugin's default formatting is pretty-printing, without the @Generated annotations
                // that we need to recognize lombok'd code.
                delombokTask.format.put('generated', 'generate')

                if (userConfig.suppressLombokWarnings) {
                  // Also re-add suppress warnings annotations so that we don't get warnings from generated
                  // code.
                  delombokTask.format.put('suppressWarnings', 'generate')
                }

                compile.dependsOn(delombokTask)
                compile.setSource(delombokTask.target.getAsFile().get())
              }
            }
          }
        }
        // lombok-generated code will always cause these warnings, because their default formatting is wrong
        // and can't be changed
        def swKeys = userConfig.extraJavacArgs.find { option -> option.startsWith("-AsuppressWarnings")}
        if (swKeys == null) {
          userConfig.extraJavacArgs += "-AsuppressWarnings=type.anno.before.modifier"
        } else {
          userConfig.extraJavacArgs += swKeys + ",type.anno.before.modifier"
        }
      }
    })
  }

  private static configureProject(Project project, CheckerFrameworkExtension userConfig) {

    // Create a map of the correct configurations with dependencies
    def dependencyMap = [
            [name: "${ANNOTATED_JDK_CONFIGURATION}", descripion: "${ANNOTATED_JDK_CONFIGURATION_DESCRIPTION}"]: "org.checkerframework:${ANNOTATED_JDK_NAME_JDK8}:${LIBRARY_VERSION}",
            [name: "${CONFIGURATION}", descripion: "${ANNOTATED_JDK_CONFIGURATION_DESCRIPTION}"]              : "${CHECKER_DEPENDENCY}",
            [name: "${JAVA_COMPILE_CONFIGURATION}", descripion: "${CONFIGURATION_DESCRIPTION}"]               : "${CHECKER_QUAL_DEPENDENCY}",
            [name: "${TEST_COMPILE_CONFIGURATION}", descripion: "${CONFIGURATION_DESCRIPTION}"]               : "${CHECKER_QUAL_DEPENDENCY}",
            [name: "errorProneJavac", descripion: "the Error Prone Java compiler"]                            : "com.google.errorprone:javac:9+181-r4173-1"
    ]

    // Add the configurations, if they don't exist, so that users can add to them.
    dependencyMap.each { configuration, dependency ->
      if (!project.configurations.find { it.name == "$configuration.name".toString() }) {
        project.configurations.create(configuration.name) { files ->
          files.description = configuration.descripion
          files.visible = false
        }
      }
    }

    // Immediately before resolving dependencies, add the dependencies to the relevant
    // configurations.
    project.getGradle().addListener(new DependencyResolutionListener() {
      @Override
      void beforeResolve(ResolvableDependencies resolvableDependencies) {
        dependencyMap.each { configuration, dependency ->
          def depGroup = dependency.tokenize(':')[0]
          def depName = dependency.tokenize(':')[1]
          // Only add the dependency if it isn't already present, to avoid overwriting user configuration.
          if (project.configurations."$configuration.name".dependencies.matching({
            if (it instanceof DefaultExternalModuleDependency) {
              it.name.equals(depName) && it.group.equals(depGroup)
            } else if (it instanceof DefaultSelfResolvingDependency) {
              it.getFiles().any { file ->
                file.toString().endsWith(depName + ".jar")
              }
            } else {
              // not sure what to do in the default case...
              false
            }
          }).isEmpty()) {
            project.configurations."$configuration.name".dependencies.add(
                    project.dependencies.create(dependency))
          }
        }

        handleLombokPlugin(project, userConfig)
        // Only attempt to add each dependency once.
        project.getGradle().removeListener(this)
      }

      @Override
      void afterResolve(ResolvableDependencies resolvableDependencies) {}
    })

    configureTasks(project, AbstractCompile, { AbstractCompile compile ->
      def ext = compile.extensions.findByName("checkerFramework")
      if (ext == null) {
        LOG.info("Adding checkerFramework extension to task {}", compile.name)
        compile.extensions.create("checkerFramework", CheckerFrameworkTaskExtension)
      } else if (ext instanceof CheckerFrameworkTaskExtension) {
        LOG.debug("Task {} in project {} already has checkerFramework added to it;" +
                " make sure you're applying the org.checkerframework plugin after the Java plugin", compile.name,
                compile.project)
      } else {
        throw new IllegalStateException("Task " + compile.name + " in project " + compile.project +
            " already has a checkerFramework extension, but it's of an incorrect type " + ext.class)
      }
    })
  }

  /**
   * Always call this method rather than using the skipCheckerFramework property directly.
   * Allows the user to set the property from the command line instead of in the build file,
   * if desired.
   */
  private static boolean skipCheckerFramework(Project project, CheckerFrameworkExtension userConfig) {
    if (project.hasProperty('skipCheckerFramework')) {
      userConfig.skipCheckerFramework = project['skipCheckerFramework'] != "false"
    }
    return userConfig.skipCheckerFramework
  }

  private static applyToProject(Project project, CheckerFrameworkExtension userConfig) {

    // Apply checker to project
    project.afterEvaluate {

      if (skipCheckerFramework(project, userConfig)) {
        LOG.info("skipping the Checker Framework because skipCheckerFramework property is set")
        return
      }

      JavaVersion javaSourceVersion =
              project.extensions.findByName('android')?.compileOptions?.sourceCompatibility ?:
                      project.property('sourceCompatibility')

      // Check Java version.
      if (!javaSourceVersion.isJava8Compatible()) {
        throw new IllegalStateException("The Checker Framework does not support Java versions before 8.")
      }

      JavaVersion jvmVersion = JavaVersion.current();

      // Decide whether to use ErrorProne Javac once configurations have been populated.
      boolean needErrorProneJavac = false
      boolean needJdk8Jar = false

      // The version string computation has side-effects that need to happen even for Java 11,
      // so it can't be guarded by the isJava8 call below.
      def versionString
      try {
        def actualCFDependencySet = project.configurations.checkerFramework.getAllDependencies()
                .matching({ dep ->
                  dep.getName().equals("checker") && dep.getGroup().equals("org.checkerframework")
                })
        if (actualCFDependencySet.size() == 0) {
          if (userConfig.skipVersionCheck) {
            versionString = LIBRARY_VERSION
          } else {
            // This line has a side-effect that's necessary for the checker to
            // be invoked, sometimes? I don't really understand why.
            versionString = new JarFile(project.configurations.checkerFramework.asPath).getManifest().getMainAttributes().getValue('Implementation-Version')
          }
        } else {
          // The call to iterator.next() is safe because we added this dependency above if it
          // wasn't specified by the user.
          versionString = actualCFDependencySet.iterator().next().getVersion()
        }
      } catch (Exception e) {
        versionString = LIBRARY_VERSION
        LOG.warn("Unable to determine Checker Framework version. Assuming default is being used: {}", versionString, e.toString())
      }

      if (javaSourceVersion.java8 && jvmVersion.isJava8()) {
        try {
          // The array accesses are safe because all CF version strings have at least two . in them.
          def majorVersion = versionString.tokenize(".")[0].toInteger()
          def minorVersion = versionString.tokenize(".")[1].toInteger()
          needErrorProneJavac = majorVersion >= 3 || (majorVersion == 2 && minorVersion >= 11)
          needJdk8Jar = majorVersion < 3 || (majorVersion == 3 && minorVersion <= 3)
        } catch (Exception e) {
          // if for any reason it's not possible to figure out the actual CF version, assume
          // errorprone javac is required
          needErrorProneJavac = true
          LOG.warn("Defaulting to ErrorProne Javac, because on a Java 8 JVM and" +
                  " cannot determine exact Checker Framework version.", e)
        }
      }

      // To keep the plugin idempotent, check if the task already exists.
      def createManifestTask = project.tasks.findByName(checkerFrameworkManifestCreationTaskName)
      if (createManifestTask == null) {
        createManifestTask = project.task(checkerFrameworkManifestCreationTaskName, type: CreateManifestTask)
        createManifestTask.checkers = userConfig.checkers
      }

      configureTasks(project, AbstractCompile, { AbstractCompile compile ->
        if (compile.extensions.checkerFramework.skipCheckerFramework) {
          LOG.info("skipping the Checker Framework for task " + compile.name +
              " because skipCheckerFramework property is set")
          return
        }
        if(userConfig.excludeTests && compile.name.toLowerCase().contains("test")) {
          LOG.info("skipping the Checker Framework for task {} because excludeTests property is set", compile.name)
          return
        }
        if (compile.hasProperty('options')) {
          compile.options.annotationProcessorPath = compile.options.annotationProcessorPath == null ?
              project.configurations.checkerFramework :
              project.configurations.checkerFramework.plus(compile.options.annotationProcessorPath)
          // Check whether to use the Error Prone javac
          if (needErrorProneJavac) {
            compile.options.forkOptions.jvmArgs += [
              "-Xbootclasspath/p:${project.configurations.errorProneJavac.asPath}".toString()
            ]
          }

          // When running on Java 9+ code, the Checker Framework needs reflective access
          // to some JDK classes. Pass the arguments that make that possible.
          if (jvmVersion.isJava9Compatible()) {
            compile.options.forkOptions.jvmArgs += [
                    "--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED"
            ]
          }
          if (jvmVersion.isJava8() && javaSourceVersion.isJava8() && needJdk8Jar) {
            compile.options.compilerArgs += [
                    "-Xbootclasspath/p:${project.configurations.checkerFrameworkAnnotatedJDK.asPath}".toString()
            ]
          }
          if (!userConfig.checkers.empty) {

            // If the user has already specified a processor manually, then
            // auto-discovery won't work. Instead, augment the list of specified
            // processors.
            int processorArgLocation = compile.options.compilerArgs.indexOf('-processor')
            if (processorArgLocation != -1) {
              String oldProcessors = compile.options.compilerArgs.get(processorArgLocation + 1)
              String newProcessors = userConfig.checkers.join(",")
              compile.options.compilerArgs.set(processorArgLocation + 1, oldProcessors + ',' + newProcessors)
            } else {
              compile.dependsOn(createManifestTask)
              // Add the manifest file to the annotation processor path, so that the javac
              // annotation processor discovery mechanism finds the checkers to use and
              // runs them alongside any other auto-discovered annotation processors.
              // Using the -processor flag to specify checkers would make the plugin incompatible
              // with other auto-discovered annotation processors, because the plugin uses auto-discovery.
              // We should warn about this: https://github.com/kelloggm/checkerframework-gradle-plugin/issues/50
              compile.options.annotationProcessorPath = compile.options.annotationProcessorPath.plus(project.files("${project.buildDir}" + manifestLocation))
            }
          }

        userConfig.extraJavacArgs.forEach({option -> compile.options.compilerArgs << option})

        ANDROID_IDS.each { id ->
          project.plugins.withId(id) {
            compile.options.bootstrapClasspath = project.files(System.getProperty("sun.boot.class.path")) + compile.options.bootstrapClasspath
            }
          }
        compile.options.fork = true
        }
      })
    }
  }
}
