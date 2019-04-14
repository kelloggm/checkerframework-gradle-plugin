package org.checkerframework.gradle.plugin;

import org.gradle.api.Named;
import org.gradle.api.Project;
import org.gradle.api.artifacts.DependencyResolutionListener;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.process.CommandLineArgumentProvider;

import java.util.Arrays;

/**
 * An Extension is a way to define a DSL for a plugin.
 *
 * <p>The Checker Framework's DSL allows users to specify,
 * in their build script, things like which checkers to
 * run and on what JavaCompile tasks.
 */
public class CheckerFrameworkPluginExtension {

    private static final Logger LOGGER = Logging.getLogger(CheckerFrameworkPluginExtension.class);

    /*
     * The typecheckers to execute, separated by commas. The CF plugin
     * won't do anything unless this property is set to a non-empty
     * value.
     */
    final Property<String> checkers;

    /*
     * The tasks to modify so that they run the CF.
     */
    final ListProperty<String> tasks;

    public CheckerFrameworkPluginExtension(final Project project) {
        checkers = project.getObjects().property(String.class);

        // Must set to empty String to avoid NPE later.
        checkers.set("");

        tasks = project.getObjects().listProperty(String.class);
    }

    /**
     * Adds a task to the list of tasks that the Checker Framework
     * will be executed as part of. If this list is empty when the
     * Checker Framework is executed, the plugin will automatically
     * apply the CF to all the tasks with the JavaCompile type.
     *
     * <p>You can call this method repeatedly to add multiple tasks.
     *
     * @param taskName the name of the task to modify
     */
    public void addTask(final String taskName) {
        tasks.add(taskName);
    }

    /**
     * Add a new typechecker to the list of checkers that should be executed.
     *
     * <p>You can call this method repeatedly to add multiple typecheckers.
     *
     * @param newChecker the fully-qualified name of the new typechecker,
     *                   e.g. "org.extension.checker.index.IndexChecker".
     */
    public void addChecker(final String newChecker) {
        String currentCheckers = checkers.get();
        if ("".equals(currentCheckers)) {
            checkers.set(newChecker);
        } else {
            checkers.set(currentCheckers + "," + newChecker);
        }
    }

    public void addCompilerMessagesChecker() {
        addChecker("org.checkerframework.checker.compilermsgs.CompilerMessagesChecker");
    }

    public void addFenumChecker() {
        addChecker("org.checkerframework.checker.fenum.FenumChecker");
    }

    public void addFormatterChecker() {
        addChecker("org.checkerframework.checker.formatter.FormatterChecker");
    }

    public void addGuiEffectChecker() {
        addChecker("org.checkerframework.checker.guieffect.GuiEffectChecker");
    }

    public void addI18nChecker() {
        addChecker("org.checkerframework.checker.i18n.I18nChecker");
    }

    public void addI18nFormatterChecker() {
        addChecker("org.checkerframework.checker.i18nformatter.I18nFormatterChecker");
    }

    public void addIndexChecker() {
        addChecker("org.checkerframework.checker.index.IndexChecker");
    }

    public void addInitializationChecker() {
        addChecker("org.checkerframework.checker.initialization.InitializationChecker");
    }

    public void addInterningChecker() {
        addChecker("org.checkerframework.checker.interning.InterningChecker");
    }

    public void addLocalizableKeyChecker() {
        addChecker("org.checkerframework.checker.i18n.LocalizableKeyChecker");
    }

    public void addLockChecker() {
        addChecker("org.checkerframework.checker.lock.LockChecker");
    }

    public void addNullnessChecker() {
        addChecker("org.checkerframework.checker.nullness.NullnessChecker");
    }

    public void addNullnessRawnessChecker() {
        addChecker("org.checkerframework.checker.nullness.NullnessRawnessChecker");
    }

    public void addOptionalChecker() {
        addChecker("org.checkerframework.checker.optional.OptionalChecker");
    }

    public void addPropertyKeyChecker() {
        addChecker("org.checkerframework.checker.propkey.PropertyKeyChecker");
    }

    public void addRegexChecker() {
        addChecker("org.checkerframework.checker.regex.RegexChecker");
    }

    public void addSignatureChecker() {
        addChecker("org.checkerframework.checker.signature.SignatureChecker");
    }

    public void addSignednessChecker() {
        addChecker("org.checkerframework.checker.signedness.SignednessChecker");
    }

    public void addTaintingChecker() {
        addChecker("org.checkerframework.checker.tainting.TaintingChecker");
    }

    public void addUnitsChecker() {
        addChecker("org.checkerframework.checker.units.UnitsChecker");
    }

    /**
     * Automatically add dependencies on the CF.
     */
    private void configureDependencies(final Project project) {
        DependencySet annnotationProcessorDeps
                = project.getConfigurations().getByName("annnotationProcessor").getDependencies();
        project.getGradle().addListener(new DependencyResolutionListener() {
            @Override
            public void beforeResolve(ResolvableDependencies resolvableDependencies) {
                annnotationProcessorDeps.add(project.getDependencies().create("org.checkerframework:checker:2.+"));
                annnotationProcessorDeps.add(project.getDependencies().create("org.checkerframework:jdk8:2.+"));
                project.getGradle().removeListener(this);
            }

            @Override
            public void afterResolve(ResolvableDependencies resolvableDependencies) {}
        });
    }


    /**
     * Applies the Checker Framework to the given task.
     * Configuration options will be provided on a task extension named 'extension'.
     * The CF will be run as an agent during the execution of the task.
     *
     * @param task the task to apply the CF to.
     */
    public void applyTo(final JavaCompile task) {
        final String taskName = task.getName();
        LOGGER.debug("Applying The Checker Framework to " + taskName);
        task.getOptions().getCompilerArgumentProviders().add(new CheckerFrameworkAgent(this));
    }

    private static class CheckerFrameworkAgent implements CommandLineArgumentProvider, Named {

        private final CheckerFrameworkPluginExtension extension;

        public CheckerFrameworkAgent(final CheckerFrameworkPluginExtension extension) {
            this.extension = extension;
        }

        /**
         * Necessary so that Gradle's incremental compilation will recompile if
         * the CF extension changes (for instance, if different typecheckers are
         * specified).
         */
        @Nested
        public CheckerFrameworkPluginExtension getExtension() {
            return extension;
        }

        @Override
        public Iterable<String> asArguments() {
            // TODO: jdk, other CF options
            return Arrays.asList("-processor", extension.checkers.get(),
                    "-Xbootclasspath/p:${configurations.annotationProcessor.asPath}");
        }

        @Override
        public String getName() {
            return "checkerframeworkAgent";
        }
    }
}
