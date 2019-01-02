package org.checkerframework.gradle.plugin

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

/**
 * A simple test that should succeed in running the nullness checker on a simple Java class.
 */
class CheckerFrameworkSuccessExpectedSmokeTest extends Specification {
    @Rule TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile, pojoFile, pojoDir

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        buildFile << """
            plugins {
                id 'java'
                id 'org.checkerframework'
            }
            repositories {
                jcenter()
            }
            dependencies {
                annotationProcessor 'org.checkerframework:checker:2.+'
                compile 'org.checkerframework:checker-qual:2.+'
            }
        """

        pojoDir = testProjectDir.newFolder('src', 'main', 'java')
        pojoFile = testProjectDir.newFile('src/main/java/Pojo.java')

        pojoFile << """
            public class Pojo {
                public String method() {
                    return "not a null";
                }
            }
        """
    }

    def "can successfully typecheck simple Java POJO with nullness checker"() {
        buildFile << """
              checkerframework.addChecker('org.checkerframework.checker.nullness.NullnessChecker')
        """

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('compileJava')
                .withPluginClasspath()
                .build()

        then:
        result.task(":compileJava").outcome == SUCCESS
    }
}
