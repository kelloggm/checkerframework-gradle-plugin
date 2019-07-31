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

  // If true, apply the checker options to all gradle subprojects by default,
  // to reduce configuration boilerplate for large projects. Set to false to disable
  // the application of checkers to subprojects automatically. If you need to apply
  // different typecheckers to different subprojects, add the checkers in
  // the subproject's build file rather than the parent's build file.
  Boolean applyToSubprojects = true
}
