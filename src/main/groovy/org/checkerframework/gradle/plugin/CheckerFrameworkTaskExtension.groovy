package org.checkerframework.gradle.plugin

/**
 * A Gradle extension for Checker Framework configuration for compile tasks.
 */
class CheckerFrameworkTaskExtension {
  /**
   * If the Checker Framework should be skipped for this compile task.
   */
  Boolean skipCheckerFramework = false
}
