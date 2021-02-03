package org.checkerframework.gradle.plugin

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

final class CheckerFrameworkExtensionSpec extends Specification {
  @Rule TemporaryFolder testProjectDir = new TemporaryFolder()
  def buildFile
  private class JavaCode {
    private static def FAILS_INDEX_CHECKER =
      """
      import org.checkerframework.checker.index.qual.NonNegative;
      import org.checkerframework.checker.index.qual.Positive;

      public class FailsIndexChecker {
        public static void main(String[] args) {
          @NonNegative int t = 0;
          @NonNegative int i = 0;
          @Positive int s = t + i; // not valid
          System.out.println("t + i = " + s);
        }
      }
      """.stripIndent().trim()
    private static def FAILS_NULLNESS_CHECKER =
      """
      import org.checkerframework.checker.nullness.qual.NonNull;
      import org.checkerframework.checker.nullness.qual.Nullable;

      public class FailsNullnessChecker {
        public static void main(String[] args) {
          @Nullable String x = null;
          System.out.println("X = " + takesNonNull(x));
        }

        static String takesNonNull(@NonNull String s) {
          return s;
        }
      }
      """.stripIndent().trim()
  }
  private class JavaClassSuccessOutput {
    private static def FAILS_INDEX_CHECKER = "t + i = 0"
    private static def FAILS_NULLNESS_CHECKER = "X = null"
  }
  private class JavaClassErrorOutput {
    private static def FAILS_NULLNESS_CHECKER = "FailsNullnessChecker.java:7: error: [argument.type.incompatible]"
    private static def FAILS_INDEX_CHECKER = "FailsIndexChecker.java:8: error: [assignment.type.incompatible]"
  }

  def "setup"() {
    buildFile = testProjectDir.newFile("build.gradle")
  }

  def "Project configured to use the Nullness Checker"() {
    given: "a project that applies the plugin without any configuration and can run the FailsNullnessChecker class"
    buildFile << """
      ${buildFileThatRunsClass("FailsNullnessChecker")}

      checkerFramework {
        checkers = ["org.checkerframework.checker.nullness.NullnessChecker"]
      }
    """.stripIndent()

    and: "The source code contains a class that fails the NullnessChecker"
    def javaSrcDir = testProjectDir.newFolder("src", "main", "java")
    new File(javaSrcDir, "FailsNullnessChecker.java") << JavaCode.FAILS_NULLNESS_CHECKER

    when: "the project is built, trying to run the Java class but failing"
    def result = GradleRunner.create()
      .withProjectDir(testProjectDir.root)
      .withArguments("run")
      .withPluginClasspath()
      .buildAndFail()

    then: "the error message explains why the code did not compile"
    result.output.contains(JavaClassErrorOutput.FAILS_NULLNESS_CHECKER)
  }

  def "Project configured to use the Nullness Checker does not use the Index Checker"() {
    given: "a project that applies the plugin without any configuration and can run the FailsIndexChecker class"
    buildFile << """
      ${buildFileThatRunsClass("FailsIndexChecker")}

      checkerFramework {
        checkers = ["org.checkerframework.checker.nullness.NullnessChecker"]
      }
    """.stripIndent()

    and: "The source code contains a class that fails the Index Checker"
    def javaSrcDir = testProjectDir.newFolder("src", "main", "java")
    new File(javaSrcDir, "FailsIndexChecker.java") << JavaCode.FAILS_INDEX_CHECKER

    when: "the project is built, running the Java class"
    def result = GradleRunner.create()
      .withProjectDir(testProjectDir.root)
      .withArguments("run")
      .withPluginClasspath()
      .build()

    then: "the build should succeed because only the Nullness Checker should be enabled"
    result.task(":run").outcome == TaskOutcome.SUCCESS

    and: "the Java class actually ran"
    result.output.contains(JavaClassSuccessOutput.FAILS_INDEX_CHECKER)
  }

  def "Project configured to use the Index Checker fails to compile FailsIndexChecker"() {
    given: "a project that applies the plugin configuring the Index Checker and can run the FailsIndexChecker class"
    buildFile << """
      ${buildFileThatRunsClass("FailsIndexChecker")}

      checkerFramework {
        checkers = ["org.checkerframework.checker.index.IndexChecker"]
      }
      """

    and: "The source code contains a class that fails the IndexChecker"
    def javaSrcDir = testProjectDir.newFolder("src", "main", "java")
    new File(javaSrcDir, "FailsIndexChecker.java") << JavaCode.FAILS_INDEX_CHECKER

    when: "the project is built, trying to run the Java class but failing"
    def result = GradleRunner.create()
      .withProjectDir(testProjectDir.root)
      .withArguments("run")
      .withPluginClasspath()
      .buildAndFail()

    then: "the error message explains why the code did not compile"
    result.output.contains(JavaClassErrorOutput.FAILS_INDEX_CHECKER)
  }

  def "Project configured to use the Index Checker can compile and run FailsNullnessChecker"() {
    given: "a project that applies the plugin configuring the Index Checker and can run the FailsNullnessChecker class"
    buildFile << """
      ${buildFileThatRunsClass("FailsNullnessChecker")}

      checkerFramework {
        checkers = ["org.checkerframework.checker.index.IndexChecker"]
      }
    """

    and: "The source code contains a class that fails the NullnessChecker but not the IndexChecker"
    def javaSrcDir = testProjectDir.newFolder("src", "main", "java")
    new File(javaSrcDir, "FailsNullnessChecker.java") << JavaCode.FAILS_NULLNESS_CHECKER

    when: "the project is built, running the Java class"
    def result = GradleRunner.create()
      .withProjectDir(testProjectDir.root)
      .withArguments("run")
      .withPluginClasspath()
      .build()

    then: "the build should succeed because only the Index Checker should be enabled"
    result.task(":run").outcome == TaskOutcome.SUCCESS

    and: "the Java class actually ran"
    result.output.contains(JavaClassSuccessOutput.FAILS_NULLNESS_CHECKER)
  }

  def "Project configured to use both Index Checker and Nullness Checker rejects both kinds of errors"() {
    given: "a project that applies the plugin configuring both nullness and Index Checker"
    buildFile << """
      ${buildFileThatRunsClass("FailsNullnessChecker")}

      checkerFramework {
        checkers = ["org.checkerframework.checker.index.IndexChecker",
                    "org.checkerframework.checker.nullness.NullnessChecker"]
      }
      """.stripIndent()

    and: "The source code contains classes that fails both checkers"
    def javaSrcDir = testProjectDir.newFolder("src", "main", "java")
    new File(javaSrcDir, "FailsNullnessChecker.java") << JavaCode.FAILS_NULLNESS_CHECKER
    new File(javaSrcDir, "FailsIndexChecker.java") << JavaCode.FAILS_INDEX_CHECKER

    when: "the project is built, trying to run the Java class but failing"
    def result = GradleRunner.create()
      .withProjectDir(testProjectDir.root)
      .withArguments("run")
      .withPluginClasspath()
      .buildAndFail()

    then: "the error message explains why the classes did not compile"
    result.output.contains(JavaClassErrorOutput.FAILS_INDEX_CHECKER) ||
      result.output.contains(JavaClassErrorOutput.FAILS_NULLNESS_CHECKER)
  }

  def "Project configured to use no checkers compiles source that would fail Nullness Checker and Index Checker"() {
    given: "a project that applies the plugin configuring no checkers"
    buildFile << """
      ${buildFileThatRunsClass("FailsNullnessChecker")}

      checkerFramework {
        checkers = []
      }
      """.stripIndent()

    and: "The source code contains classes that fails both checkers"
    def javaSrcDir = testProjectDir.newFolder("src", "main", "java")
    new File(javaSrcDir, "FailsNullnessChecker.java") << JavaCode.FAILS_NULLNESS_CHECKER
    new File(javaSrcDir, "FailsIndexChecker.java") << JavaCode.FAILS_INDEX_CHECKER

    when: "the project is built, running the Java class"
    def result = GradleRunner.create()
      .withProjectDir(testProjectDir.root)
      .withArguments("run")
      .withPluginClasspath()
      .build()

    then: "the build should succeed because the Nullness Checker should be enabled"
    result.task(":run").outcome == TaskOutcome.SUCCESS

    and: "the Java class actually ran"
    result.output.contains(JavaClassSuccessOutput.FAILS_NULLNESS_CHECKER)
  }

  private static def buildFileThatRunsClass(String className) {
    """
    plugins {
      id "java"
      id "application"
      id "org.checkerframework"
    }

    repositories {
      maven {
        url "${getClass().getResource("/maven/").toURI()}"
      }
    }

    mainClassName = "${className}"
    """.stripIndent()
  }
}
