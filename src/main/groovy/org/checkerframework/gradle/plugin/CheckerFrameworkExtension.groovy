package org.checkerframework.gradle.plugin

class CheckerFrameworkExtension {
  // Which checkers will be run.  Each element is a fully-qualified class name,
  // such as "org.checkerframework.checker.nullness.NullnessChecker".
  List<String> checkers = []

  // A list of extra options to pass directly to javac when running typecheckers
  List<String> extraJavacArgs = []

  Boolean excludeTests = false

  // If you encounter "zip file too large" errors, you can set this flag to avoid
  // the standard version check which unzips a jar to look at its manifest.
  Boolean skipVersionCheck = false

  // If true, generate @SuppressWarnings("all") annotations on Lombok-generated code,
  // which is Lombok's default but could permit unsoundness from the Checker Framework.
  // For an example, see https://github.com/kelloggm/checkerframework-gradle-plugin/issues/85.
  Boolean suppressLombokWarnings = true
}
