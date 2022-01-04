package org.checkerframework.gradle.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * This task generates a manifest file in the style of
 * https://checkerframework.org/manual/#checker-auto-discovery.
 * Once the plugin places this directory containing it on the annotation processor path,
 * javac can find the checkers via annotation processor discovery.
 */
class CreateManifestTask extends DefaultTask {

    private final static String manifestDirectoryName = "checkerframework/META-INF/services/"
    private final static String manifestFileName = "javax.annotation.processing.Processor"

    // Mark the checkers as incremental annotation processors for Gradle
    private final static String gradleManifestDirectoryName = "checkerframework/META-INF/gradle/"
    private final static String gradleManifestFileName = "incremental.annotation.processors"

    @Input
    String[] checkers = []

    @Input
    boolean incrementalize = true

    /**
     * Creates a manifest file listing all the checkers to run.
     */
    @TaskAction
    def generateManifest() {
        def manifestDir = project.mkdir "${project.buildDir}/${manifestDirectoryName}"
        def manifestFile = project.file("${manifestDir.absolutePath}/${manifestFileName}")
        if (!manifestFile.createNewFile()) {
            manifestFile.delete()
            manifestFile.createNewFile()
        }
        manifestFile << this.checkers.join('\n')

        if (incrementalize) {
            def gradleManifestDir = project.mkdir "${project.buildDir}/${gradleManifestDirectoryName}"
            def gradleManifestFile = project.file("${gradleManifestDir.absolutePath}/${gradleManifestFileName}")
            if (!gradleManifestFile.createNewFile()) {
                gradleManifestFile.delete()
                gradleManifestFile.createNewFile()
            }
            // The following code treats all checkers as isolating annotation processors.
            // I think this is the right decision: checkers should be reasoning about
            // each method in isolation. But, we provide a way to disable incremental
            // compilation in the event that this decision is breaking for some particular
            // checker.
            def gradleManifestFileContents = ""
            // do a join, but add ",ISOLATING" after each entry
            for (int i = 0; i < this.checkers.length; i++) {
                gradleManifestFileContents += checkers[i] + ",ISOLATING"
                if (i != checkers.length - 1) {
                    gradleManifestFileContents += "\n"
                }
            }

            gradleManifestFile << gradleManifestFileContents
        }
    }

    /**
     * Required so that this can incrementalized correctly, and so that `gradle clean build` behaves
     * the same as `gradle clean ; gradle build`.
     */
    @OutputFile
    def getManifestLocation() {
        return "${project.buildDir}/${manifestDirectoryName}/${manifestFileName}"
    }

    /**
     * Required so that this can incrementalized correctly, and so that `gradle clean build` behaves
     * the same as `gradle clean ; gradle build`.
     */
    @OutputFile
    def getGradleManifestLocation() {
        return "${project.buildDir}/${gradleManifestDirectoryName}/${gradleManifestFileName}"
    }
}
