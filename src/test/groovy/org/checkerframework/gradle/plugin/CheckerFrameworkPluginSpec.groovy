package org.checkerframework.gradle.plugin

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import spock.lang.Unroll
import test.BaseSpecification

final class CheckerFrameworkPluginSpec extends BaseSpecification {
  static final List<String> TESTED_GRADLE_VERSIONS = [
    '3.4',
    '4.0',
    '4.8',
    '4.9',
    '4.10',
    '5.0',
    '5.5',
    '6.0',
    '6.4',
    '6.5',
  ]

  @Unroll def "java project running licenseReport using with gradle #gradleVersion"() {
    given:
    buildFile <<
      """
        plugins {
          id "java"
          id "org.checkerframework"
        }

        repositories {
          maven {
            url "${getClass().getResource("/maven/").toURI()}"
          }
        }
      """.stripIndent().trim()

    when:
    GradleRunner.create()
      .withGradleVersion(gradleVersion)
      .withProjectDir(testProjectDir.root)
      .withPluginClasspath()
      .build()

    then:
    noExceptionThrown()

    where:
    gradleVersion << TESTED_GRADLE_VERSIONS
  }

  @Unroll
  def 'without relevant plugin, compiler settings are not applied using version #gradleVersion'() {
    given:
    buildFile <<
      """
        plugins {
          id 'org.checkerframework'
        }

        repositories {
          maven {
            url "${getClass().getResource("/maven/").toURI()}"
          }
        }
      """.stripIndent().trim()

    when:
    BuildResult result = GradleRunner.create()
      .withGradleVersion(gradleVersion)
      .withProjectDir(testProjectDir.root)
      .withPluginClasspath()
      .build()

    then:
    result.output.contains('checker compiler options will not be applied')

    where:
    gradleVersion << TESTED_GRADLE_VERSIONS
  }

  @Unroll
  def 'with relevant plugin, compiler settings are applied using version #gradleVersion'() {
    given:
    buildFile <<
      """
        plugins {
          id 'java'
          id 'org.checkerframework'
        }

        repositories {
          maven {
            url "${getClass().getResource("/maven/").toURI()}"
          }
        }
      """.stripIndent().trim()

    when:
    BuildResult result = GradleRunner.create()
      .withGradleVersion(gradleVersion)
      .withProjectDir(testProjectDir.root)
      .withPluginClasspath()
      .withArguments('--info')
      .build()

    then:
    result.output.contains('applying checker compiler options')

    where:
    gradleVersion << TESTED_GRADLE_VERSIONS
  }

  @Unroll
  def 'with relevant plugin loaded subsequently, compiler settings are applied with gradle #gradleVersion'() {
    given:
    buildFile <<
      """
        plugins {
          id 'org.checkerframework'
          id 'java'
        }

        repositories {
          maven {
            url "${getClass().getResource("/maven/").toURI()}"
          }
        }
      """.stripIndent().trim()

    when:
    BuildResult result = GradleRunner.create()
      .withGradleVersion(gradleVersion)
      .withProjectDir(testProjectDir.root)
      .withPluginClasspath()
      .withArguments('--info')
      .build()

    then:
    result.output.contains('applying checker compiler options')

    where:
    gradleVersion << TESTED_GRADLE_VERSIONS
  }

  @Unroll
  def 'with resolved configuration dependencies, compilation settings are still applied with gradle #gradleVersion'() {
    given:
    buildFile <<
      """
        plugins {
          id 'java'
          id 'org.checkerframework'
          id 'application'
        }

        repositories {
          maven {
            url "${getClass().getResource("/maven/").toURI()}"
          }
        }
        // Trigger resolution of compile classpath
        println project.sourceSets.main.compileClasspath as List
      """.stripIndent().trim()

    when:
    BuildResult result = GradleRunner.create()
      .withGradleVersion(gradleVersion)
      .withProjectDir(testProjectDir.root)
      .withPluginClasspath()
      .withArguments('--info')
      .build()

    then:
    result.output.contains('applying checker compiler options')

    where:
    gradleVersion << TESTED_GRADLE_VERSIONS
  }

  @Unroll
  def 'skipCheckerFramework can be used to skip checking individual tasks with gradle #gradleVersion'() {
    given:
    buildFile << """
        plugins {
          id 'java'
          id 'org.checkerframework'
          id 'application'
        }

        repositories {
          maven {
            url "${getClass().getResource("/maven/").toURI()}"
          }
        }

        tasks.withType(JavaCompile).all {
          configure {
            checkerFramework {
              skipCheckerFramework = true
            }
          }
        }
      """.stripIndent().trim()

    when:
    BuildResult result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir.root)
        .withPluginClasspath()
        .withArguments('--info')
        .build()

    then:
    result.
        output.
        contains('skipping the Checker Framework for task compileJava because skipCheckerFramework property is set')

    where:
    gradleVersion << TESTED_GRADLE_VERSIONS
  }

  @Unroll
  def 'plugin warns when applying org.checkerframework after java plugin with gradle #gradleVersion'() {
    given:
    buildFile << """
        plugins {
          id 'org.checkerframework'
          id 'java'
        }

        repositories {
          maven {
            url "${getClass().getResource("/maven/").toURI()}"
          }
        }
      """.stripIndent().trim()

    when:
    BuildResult result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir.root)
        .withPluginClasspath()
        .withArguments('--debug')
        .build()

    then:
    result.
        output.
        contains("make sure you're applying the org.checkerframework plugin after the Java plugin")

    where:
    // This particular behavior only shows up on Gradle 6.4+
    gradleVersion << TESTED_GRADLE_VERSIONS.stream().filter({
      GradleVersion.version(it) >= GradleVersion.version("6.4")
    })
  }
}
