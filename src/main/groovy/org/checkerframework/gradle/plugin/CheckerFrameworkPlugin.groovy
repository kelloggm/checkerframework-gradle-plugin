package org.checkerframework.gradle.plugin

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

  // Whenever this line is changed, you need to change the corresponding text in README.md.
  private final static def LIBRARY_VERSION = "2.8.2"

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

  @Override void apply(Project project) {
    CheckerFrameworkExtension userConfig = project.extensions.create("checkerFramework", CheckerFrameworkExtension)
    boolean applied = false
    (ANDROID_IDS + "java").each { id ->
      project.pluginManager.withPlugin(id) {
        LOG.info('Found plugin {}, applying checker compiler options.', id)
        configureProject(project, userConfig)
        if (!applied) applied = true
      }
    }

    project.getPlugins().withType(io.freefair.gradle.plugins.lombok.LombokPlugin.class, new Action<LombokPlugin>() {
      void execute(LombokPlugin lombokPlugin) {

        // Ensure that the lombok config is set to emit @Generated annotations
        lombokPlugin.configureForJacoco()
        lombokPlugin.generateLombokConfig.get().generateLombokConfig()

        project.gradle.projectsEvaluated {

          def delombokTasks = project.getTasks().findAll { task ->
            task.name.startsWith("delombok")
          }

          if (delombokTasks.size() != 0) {

            // change every compile task so that it:
            // 1. depends on delombok, and
            // 2. uses the delombok'd source code as its source set

            project.tasks.withType(AbstractCompile).all { compile ->

              // find the right delombok task
              def delombokTask = delombokTasks.find { task ->
                if (task.name.equals("delombok")) {
                  // special-case the main compile task because its just named "compileJava"
                  // without anything else
                  compile.name.equals("compileJava")
                } else {
                  // "delombok" is 8 characters.
                  compile.name.contains(task.name.substring(8))
                }
              }

              // the lombok plugin's default formatting is pretty-printing, without the @Generated annotations
              // that we need to recognize lombok'd code
              delombokTask.format.put('generated', 'generate')

              compile.dependsOn(delombokTask)
              compile.setSource(delombokTask.target.getAsFile().get())
            }
          }
        }
        // lombok-generated code will always causes these warnings, because their default formatting is wrong
        // and can't be changed
        userConfig.extraJavacArgs += "-AsuppressWarnings=type.anno.before.modifier"
      }
    })

    project.gradle.projectsEvaluated {
      if (!applied) LOG.warn('No android or java plugins found, checker compiler options will not be applied.')
    }
  }

  private static configureProject(Project project, CheckerFrameworkExtension userConfig) {
    JavaVersion javaVersion =
        project.extensions.findByName('android')?.compileOptions?.sourceCompatibility ?:
        project.property('sourceCompatibility')

    // Check Java version.
    def jdkVersion
    if (javaVersion.java7) {
      throw new IllegalStateException("The Checker Framework does not support Java 7.")
    } else if (javaVersion.java8) {
      jdkVersion = ANNOTATED_JDK_NAME_JDK8
    } else {
      // Use Java 8, even if the user requested a newer version of Java.
      // Undo this hack when newer annotated JDKs are released by the Checker Framework team.
      jdkVersion = ANNOTATED_JDK_NAME_JDK8
    }

    // Create a map of the correct configurations with dependencies
    def dependencyMap = [
      [name: "${ANNOTATED_JDK_CONFIGURATION}", descripion: "${ANNOTATED_JDK_CONFIGURATION_DESCRIPTION}"]: "org.checkerframework:${jdkVersion}:${LIBRARY_VERSION}",
      [name: "${CONFIGURATION}", descripion: "${ANNOTATED_JDK_CONFIGURATION_DESCRIPTION}"]              : "${CHECKER_DEPENDENCY}",
      [name: "${JAVA_COMPILE_CONFIGURATION}", descripion: "${CONFIGURATION_DESCRIPTION}"]               : "${CHECKER_QUAL_DEPENDENCY}",
      [name: "${TEST_COMPILE_CONFIGURATION}", descripion: "${CONFIGURATION_DESCRIPTION}"]               : "${CHECKER_QUAL_DEPENDENCY}",
      [name: "errorProneJavac", descripion: "the Error Prone Java compiler"]                            : "com.google.errorprone:javac:9+181-r4173-1"
    ]

    // Now, apply the dependencies to project
    dependencyMap.each { configuration, dependency ->
      // User could have an existing configuration, the plugin will add to it
      if (project.configurations.find { it.name == "$configuration.name".toString() }) {
        project.configurations."$configuration.name".dependencies.add(
          project.dependencies.create(dependency))
      } else {
        // If the user does not have the configuration, the plugin will create it
        project.configurations.create(configuration.name) { files ->
          files.description = configuration.descripion
          files.visible = false
          files.defaultDependencies { dependencies ->
            dependencies.add(project.dependencies.create(dependency))
          }
        }
      }
    }

    // Apply checker to project
    project.gradle.projectsEvaluated {

      // Decide whether to use ErrorProne Javac once configurations have been populated.
      def actualCFDependencySet = project.configurations.checkerFramework.getAllDependencies()
              .matching({dep ->
        dep.getName().equals("checker") && dep.getGroup().equals("org.checkerframework")})

      def CFVersionString
      if (actualCFDependencySet.size() == 0) {
        CFVersionString = new JarFile(project.configurations.checkerFramework.asPath).getManifest().getMainAttributes().getValue('Implementation-Version')
      } else {
        // The call to iterator.next() is safe because we added this dependency above if it
        // wasn't specified by the user.
        CFVersionString = actualCFDependencySet.iterator().next().getVersion()
      }
      // The array access is safe because all CF version strings have at least one . in them.
      def isCFThreePlus = CFVersionString.tokenize(".")[0].toInteger() >= 3

      boolean needErrorProneJavac = javaVersion.java8 && isCFThreePlus


      project.tasks.withType(AbstractCompile).all { compile ->
        if (compile.hasProperty('options') && (!userConfig.excludeTests || !compile.name.toLowerCase().contains("test"))) {
          // Check whether to use the Error Prone javac
          compile.options.annotationProcessorPath = project.configurations.checkerFramework
          if (needErrorProneJavac) {
            compile.options.forkOptions.jvmArgs += [
              "-Xbootclasspath/p:${project.configurations.errorProneJavac.asPath}".toString()
            ]
          }
          compile.options.compilerArgs = [
            "-Xbootclasspath/p:${project.configurations.checkerFrameworkAnnotatedJDK.asPath}".toString()
          ]
          if (!userConfig.checkers.empty) {
            compile.options.compilerArgs << "-processor" << userConfig.checkers.join(",")
          }

        userConfig.extraJavacArgs.forEach({option -> compile.options.compilerArgs << option})

        ANDROID_IDS.each { id ->
          project.plugins.withId(id) {
            options.bootClasspath = System.getProperty("sun.boot.class.path") + ":" + options.bootClasspath
            }
          }
        options.fork = true
        }
      }
    }
  }
}
