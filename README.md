# Gradle Checker Framework Plugin

[![License](https://img.shields.io/badge/license-apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
![Build Status](https://github.com/kelloggm/checkerframework-gradle-plugin/actions/workflows/gradle.yml/badge.svg)

This plugin configures `JavaCompile` tasks to use the [Checker Framework](https://checkerframework.org) for pluggable type-checking.

## Download

Add the following to your `build.gradle` file:

```groovy
plugins {
    // Checker Framework pluggable type-checking
    id 'org.checkerframework' version '0.6.26'
}

apply plugin: 'org.checkerframework'
```

The `org.checkerframework` plugin modifies existing Java
compilation tasks. You should apply it *after*
whatever plugins introduce your Java compilation tasks (usually the `java`
or `java-library` plugin for non-Android builds).

## Configuration

### Configuring which checkers to use

The `checkerFramework.checkers` property lists which checkers will be run.

For example, using Groovy syntax in a `build.gradle` file:

```groovy
checkerFramework {
  checkers = [
    'org.checkerframework.checker.nullness.NullnessChecker',
    'org.checkerframework.checker.units.UnitsChecker'
  ]
}
```

The same example, using Kotlin syntax in a `build.gradle.kts` file:

```kotlin
// In Kotlin, you need to import CheckerFrameworkExtension explicitly:
import org.checkerframework.gradle.plugin.CheckerFrameworkExtension

configure<CheckerFrameworkExtension> {
    checkers = listOf(
        "org.checkerframework.checker.nullness.NullnessChecker",
        "org.checkerframework.checker.units.UnitsChecker"
    )
}
```

For a list of checkers, see the [Checker Framework Manual](https://checkerframework.org/manual/#introduction).

### Providing checker-specific options to the compiler

You can set the `checkerFramework.extraJavacArgs` property in order to pass additional options to the compiler when running
a typechecker.

For example, to use a stub file:

```groovy
checkerFramework {
  extraJavacArgs = [
    '-Werror',
    '-Astubs=/path/to/my/stub/file.astub'
  ]
}
```

### Configuring third-party checkers

To use a third-party typechecker (i.e. one that is not distributed with the Checker Framework),
add a dependency to the `checkerFramework` dependency configuration.

For example, to use the [Glacier](http://mcoblenz.github.io/Glacier/) immutability checker:

```groovy
dependencies {
  ...
  checkerFramework 'edu.cmu.cs.glacier:glacier:0.1'
}
```

You should also use a `checkerFramework` dependency for anything needed by a checker you
are running. For example, if you are using the
[Subtyping Checker](https://checkerframework.org/manual/#subtyping-checker) with
custom type qualifiers, you should add a `checkerFramework` dependency referring to
the definitions of the custom qualifiers.

### Specifying a Checker Framework version

Version 0.6.26 of this plugin uses Checker Framework version 3.33.0 by default.
Anytime you upgrade to a newer version of this plugin,
it might use a different version of the Checker Framework.

You can use a Checker Framework
[version](https://github.com/typetools/checker-framework/releases) that is
different than this plugin's default.  For example, if you want to use Checker
Framework version 3.4.0, then you should add the following text to
`build.gradle`, after `apply plugin: 'org.checkerframework'`:

```groovy
dependencies {
  compileOnly 'org.checkerframework:checker-qual:3.4.0'
  testCompileOnly 'org.checkerframework:checker-qual:3.4.0'
  checkerFramework 'org.checkerframework:checker:3.4.0'
}
```

You can also use a locally-built version of the Checker Framework:

```groovy
// To use a locally-built Checker Framework, run gradle with "-PcfLocal".
if (project.hasProperty("cfLocal")) {
  def cfHome = String.valueOf(System.getenv("CHECKERFRAMEWORK"))
  dependencies {
    compileOnly files(cfHome + "/checker/dist/checker-qual.jar")
    testCompileOnly files(cfHome + "/checker/dist/checker-qual.jar")
    checkerFramework files(cfHome + "/checker/dist/checker.jar")
  }
}
```

### Incremental compilation

By default, the plugin assumes that all checkers are "isolating incremental annotation processors"
according to the Gradle terminology
[here](https://docs.gradle.org/current/userguide/java_plugin.html#sec:incremental_annotation_processing).
This assumption speeds up builds by enabling incremental compilation, but is unsafe: Gradle's
documentation warns that annotation processors that use internal Javac APIs may crash, because
Gradle wraps some of those APIs. The Checker Framework does use internal Javac APIs, so you
might encounter such a crash, which would appear as a `ClassCastException` referencing some
internal Javac class. If you encounter such a crash, you can disable incremental
compilation in your build using the following code in your `checkerFramework` configuration block:

```groovy
  checkerFramework {
    incrementalize = false
  }
```

### Per-Task Configuration

You can also use a `checkerFramework` block to configure individual tasks. This
can be useful for skipping the Checker Framework on generated code:

```build.gradle
tasks.withType(JavaCompile).configureEach {
  // Don't run the checker on generated code.
  if (name.equals("compileMainGeneratedDataTemplateJava")
      || name.equals("compileMainGeneratedRestJava")) {
    checkerFramework {
      skipCheckerFramework = true
    }
  }
}
```

Currently, the only supported option is `skipCheckerFramework`.

### Other options

* You can disable the Checker Framework temporarily (e.g. when testing something unrelated)
 either in your build file or from the command line. In your build file:

  ```groovy
  checkerFramework {
    skipCheckerFramework = true
  }
  ```

  From the command line, add `-PskipCheckerFramework` to your gradle invocation.
  This property can also take an argument:
  anything other than `false` results in the Checker Framework being skipped.

* By default, the plugin applies the selected checkers to all `JavaCompile` targets,
  including test targets such as `testCompileJava`.

  Here is how to prevent checkers from being applied to test targets:

  ```groovy
  checkerFramework {
    excludeTests = true
  }
  ```

  The check for test targets is entirely syntactic: this option will not apply the checkers
  to any task whose name includes "test", ignoring case.

* If you encounter errors of the form `zip file name too long` when configuring your
Gradle project, you can use the following code to skip this plugin's version check,
which reads the manifest file of the version of the Checker Framework you are actually
using:

  ```groovy
  checkerFramework {
    skipVersionCheck = true
  }
  ```


### Multi-project builds

In a project with subprojects, you should apply the project to each Java
subproject (and to the top-level project, in the unlikely case that it is a Java
project).  Here are two approaches.

**Approach 1:**
All Checker Framework configuration (the `checkerFramework` block and any
`dependencies`) remains in the top-level `build.gradle` file.  Put it in a
`subprojects` block (or an `allprojects` block in the unlikely case that the
top-level project is a Java project).  For example:

```groovy
plugins {
  id 'org.checkerframework' version '0.6.26' apply false
}

subprojects { subproject ->
  apply plugin: 'org.checkerframework'

  checkerFramework {
    checkers = ['org.checkerframework.checker.index.IndexChecker']
  }
  dependencies {
    checkerFramework 'org.checkerframework:checker:3.33.0'
    implementation 'org.checkerframework:checker-qual:3.33.0'
  }
}
```

**Approach 2:**
Apply the plugin in the `build.gradle` in each subproject as if it
were a stand-alone project. You must do this if you require different configuration
for different subprojects (for instance, if you want to run different checkers).

### Incompatibility with Error Prone 2.3.4 and earlier

[Error Prone](https://errorprone.info/)
uses the Checker Framework's dataflow analysis library.
Unfortunately, Error Prone version 2.3.4 and earlier uses an old version of the library, so you
cannot use both Error Prone and the current Checker Framework (because each
one depends on a different version of the library).

You can resolve this by:
 * upgrading to Error Prone version 2.4.0 or later, or
 * using a switch that causes your build to use either
   Error Prone or the Checker Framework, but not both.

Here is an example of the latter approach:

```
plugins {
  id "net.ltgt.errorprone" version "1.1.1" apply false
  // To do Checker Framework pluggable type-checking (and disable Error Prone), run:
  // ./gradlew compileJava -PuseCheckerFramework=true
  id 'org.checkerframework' version '0.6.26' apply false
}

if (!project.hasProperty("useCheckerFramework")) {
    ext.useCheckerFramework = "false"
}
if ("true".equals(project.ext.useCheckerFramework)) {
  apply plugin: 'org.checkerframework'
} else {
  apply plugin: 'net.ltgt.errorprone'
}

def errorProneVersion = "2.3.4"
def checkerFrameworkVersion = "3.33.0"

dependencies {
  if ("true".equals(project.ext.useCheckerFramework)) {
    checkerFramework 'org.checkerframework:checker:' + checkerFrameworkVersion
    checkerFramework 'org.checkerframework:checker-qual:' + checkerFrameworkVersion
  } else {
    errorprone group: 'com.google.errorprone', name: 'error_prone_core', version: errorProneVersion
  }
}

if ("true".equals(project.ext.useCheckerFramework)) {
  checkerFramework {
    checkers = [
      'org.checkerframework.checker.interning.InterningChecker',
      'org.checkerframework.checker.signature.SignatureChecker'
    ]
  }
} else {
  // Configuration for the Error Prone linter.
  tasks.withType(JavaCompile).each { t ->
    if (!t.name.equals("compileTestInputJava") && !t.name.startsWith("checkTypes")) {
      t.toolChain ErrorProneToolChain.create(project)
      t.options.compilerArgs += [
        '-Xep:StringSplitter:OFF',
        '-Xep:ReferenceEquality:OFF' // use Interning Checker instead
      ]
    }
  }
}
```

## Java 9+ compatibility

When running the plugin on a Java 9+ project that uses modules,
you may need to add annotations to the module path. First add
`requires org.checkerframework.checker.qual;` to your `module-info.java`.  The Checker
Framework inserts inferred annotations into bytecode even if none appear in source code,
so you must do this even if you write no annotations in your code.

Then, add this line to the `checkerFramework` block to add the `checker-qual.jar`
artifact (which only contains annotations) to the module path:

```
checkerFramework {
  extraJavacArgs = [
    '--module-path', compileOnly.asPath
  ]
}
```

## Lombok compatibility

This plugin automatically interacts with
the [Lombok Gradle Plugin](https://plugins.gradle.org/plugin/io.freefair.lombok)
to delombok your source code before it is passed to the Checker Framework
for typechecking. This plugin does not support any other use of Lombok.

For the Checker Framework to work properly on delombok'd source code,
you must include the following key in your project's `lombok.config` file:

```
lombok.addLombokGeneratedAnnotation = true
```

By default, Lombok suppresses all warnings in the code it generates. If you
want to typecheck the code that Lombok generates, use the `suppressLombokWarnings`
configuration key:

```
checkerFramework {
  suppressLombokWarnings = false
}
```

Note that doing so will cause *all* tools (including Javac itself) to begin issuing
warnings in the code that Lombok generates.

## Using a locally-built plugin

You can build the plugin locally rather than downloading it from Maven Central.

To build the plugin from source, run `./gradlew build`.

If you want to use a locally-built version of the plugin, you can publish the plugin to your
local Maven repository by running `./gradlew publishToMavenLocal`. Then, add the following to
the `settings.gradle` file in the Gradle project that you want to use the plugin:

```
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}
```

### JDK 8 vs JDK 9+ implementation details

The plugin attempts to automatically configure the Checker Framework on both Java 8 and Java 9+ JVMs,
following the [best practices in the Checker Framework manual](https://checkerframework.org/manual/#javac).
In particular:
* If both the JVM and target versions are 8, it applies the Java 8 annotated JDK.
* If the JVM version is 9+ and the target version is 8 (and the Checker Framework
version is >= 2.11.0), use the Error Prone javac compiler.
* If the JVM version is 9+, use the `--add-opens` option to `javac`.

## Credits

This project started as a fork of [an abandoned plugin built by jaredsburrows](https://github.com/jaredsburrows/gradle-checker-framework-plugin).
[![Twitter Follow](https://img.shields.io/twitter/follow/jaredsburrows.svg?style=social)](https://twitter.com/jaredsburrows)


## License

    Copyright (C) 2017 Jared Burrows, 2018-2020 Martin Kellogg

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
