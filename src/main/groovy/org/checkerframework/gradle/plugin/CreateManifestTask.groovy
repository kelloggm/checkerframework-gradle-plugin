package org.checkerframework.gradle.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class CreateManifestTask extends DefaultTask {

    @Input
    String[] checkers = []

    /**
     * Creates a manifest file listing all the checkers to run.
     */
    @TaskAction
    def generateManifest() {
        def manifestDir = project.mkdir "${project.buildDir}/checkerframework/META-INF/services/"
        def manifestFile = project.file(manifestDir.absolutePath + "/javax.annotation.processing.Processor")
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
        return "${project.buildDir}/checkerframework/META-INF/services/javax.annotation.processing.Processor"

    }
}
