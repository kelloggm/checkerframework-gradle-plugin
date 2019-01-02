package org.checkerframework.gradle.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.compile.JavaCompile;

import java.util.List;

/**
 * The main plugin class.
 */
public class CheckerFrameworkPlugin implements Plugin<Project> {

    public void apply(final Project project) {
        CheckerFrameworkPluginExtension extension =
                project.getExtensions().create("checkerframework", CheckerFrameworkPluginExtension.class, project);

        applyToTasks(project, extension);
    }

    /**
     * Applies the CF agent to all tasks named by the extension.
     *
     * If the extension doesn't name any tasks, by default all
     * tasks of type JavaCompile are applied to.
     *
     * @param extension the extension to apply the CF with
     */
    private void applyToTasks(final Project project, final CheckerFrameworkPluginExtension extension) {
        List<String> taskNames = extension.tasks.getOrNull();
        TaskCollection<JavaCompile> tasks = project.getTasks().withType(JavaCompile.class);

        if (taskNames != null) {
            tasks.removeIf(task -> !taskNames.contains(task.getName()));
        }

        tasks.configureEach(task -> extension.applyTo(task));
    }
}

