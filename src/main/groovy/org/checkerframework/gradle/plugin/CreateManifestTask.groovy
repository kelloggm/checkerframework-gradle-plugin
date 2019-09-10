package org.checkerframework.gradle.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * This task generates a manifest file in the style of
 * https://checkerframework.org/manual/#checker-auto-discovery
 * Once the plugin places this directory containing it on the annotation processor path,
 * javac can find the checkers via annotation processor discovery.
 */
class CreateManifestTask extends DefaultTask {

    private final static String buildDirectory = "checkerframework/META-INF/services/"
    private final static String manifestFileName = "javax.annotation.processing.Processor"

    @Input
    String[] checkers = []

    /**
     * Creates a manifest file listing all the checkers to run.
     */
    @TaskAction
    def generateManifest() {
        def manifestDir = project.mkdir "${project.buildDir}/${buildDirectory}"
        def manifestFile = project.file("${manifestDir.absolutePath}/${manifestFileName}")
        if (!manifestFile.createNewFile()) {
            manifestFile.delete()
            manifestFile.createNewFile()
        }
        manifestFile << this.checkers.join('\n')
    }

    /**
     * Required so that this can incrementalized correctly, and so that `gradle clean build` behaves
     * the same as `gradle clean ; gradle build`.
     */
    @OutputFile
    def getManifestLocation() {
        return "${project.buildDir}/${buildDirectory}/${manifestFileName}"
    }
}
