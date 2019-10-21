# Gradle Checker Framework Plugin

[![License](https://img.shields.io/badge/license-apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Build Status](https://travis-ci.com/kelloggm/checkerframework-gradle-plugin.svg?branch=master)](https://travis-ci.com/kelloggm/checkerframework-gradle-plugin)

This plugin configures `JavaCompile` tasks to use the [Checker Framework](https://checkerframework.org) for pluggable type-checking.

## Download

Add the following to your `build.gradle` file:

```groovy
plugins {
    // Checker Framework pluggable type-checking
    id 'org.checkerframework' version '0.4.6'
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

For example:

```groovy
checkerFramework {
  checkers = [
    'org.checkerframework.checker.nullness.NullnessChecker',
    'org.checkerframework.checker.units.UnitsChecker'
  ]
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

### Specifying a Checker Framework version

This plugin uses Checker Framework version 2.11.0 by default.
Anytime you upgrade to a newer version of this plugin,
it might use a different version of the Checker Framework.

If you wish to use a specific Checker Framework
[version](https://github.com/typetools/checker-framework/releases),
add text like the following to `build.gradle`, after `apply plugin: 'org.checkerframework'`:

```groovy
dependencies {
  compileOnly 'org.checkerframework:checker-qual:2.8.0'
  testCompileOnly 'org.checkerframework:checker-qual:2.8.0'
  checkerFramework 'org.checkerframework:checker:2.8.0'
  checkerFrameworkAnnotatedJDK 'org.checkerframework:jdk8:2.8.0'
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
    checkerFrameworkAnnotatedJDK files(cfHome + "/checker/dist/jdk8.jar")
  }
}
```

### Other options

* By default, the plugin applies the selected checkers to all `JavaCompile` targets.

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

By default, checkers are run on all subprojects of the project to which the plugin
is applied.

In most projects with subprojects, the top-level project is not a Java
project.  You should not configure such a non-Java project.  Instead, move
all Checker Framework configuration (the `checkerFramework` block and any
`dependencies`) into a `subprojects` block. For example:

```groovy
subprojects { subproject ->
  checkerFramework {
    checkers = ['org.checkerframework.checker.index.IndexChecker']
  }
  dependencies {
    checkerFramework 'org.checkerframework:checker:2.11.0'
    implementation 'org.checkerframework:checker-qual:2.11.0'
  }
}
```

To not apply the plugin to all subprojects, set the `applyToSubprojects`
flag to `false`:

```groovy
checkerFramework {
  applyToSubprojects = false
}
```

Then, apply the plugin to the `build.gradle` in each subproject where you
do want to run the checker.


### Incompatibility with Error Prone

[Error Prone](https://errorprone.info/)
uses the Checker Framework's dataflow analysis library.
Unfortunately, Error Prone uses an old version of the library, so you
cannot use both Error Prone and the current Checker Framework (because each
one depends on a different version of the library).

You can resolve this via a switch that causes your build to use either
Error Prone or the Checker Framework, but not both.
Here is how to change the above instructions:


```
plugins {
  id "net.ltgt.errorprone-base" version "0.0.16" apply false
  // To do Checker Framework pluggable type-checking (and disable Error Prone), run:
  // ./gradlew compileJava -PuseCheckerFramework=true
  id 'org.checkerframework' version '0.4.6' apply false
}

if (!project.hasProperty("useCheckerFramework")) {
    ext.useCheckerFramework = "false"
}
if ("true".equals(project.ext.useCheckerFramework)) {
  apply plugin: 'org.checkerframework'
} else {
  apply plugin: 'net.ltgt.errorprone-base'
}


def checkerFrameworkVersion = "2.11.0"

dependencies {
  if ("true".equals(project.ext.useCheckerFramework)) {
    checkerFramework 'org.checkerframework:checker:' + checkerFrameworkVersion
    checkerFramework 'org.checkerframework:jdk8:' + checkerFrameworkVersion
    checkerFramework 'org.checkerframework:checker-qual:' + checkerFrameworkVersion
  } else {
    errorprone group: 'com.google.errorprone', name: 'error_prone_core', version: '2.3.3'
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

When using a Checker Framework version that uses the Java 9+ compiler API
(version 2.11.0 and above), the plugin chooses an appropriate compiler.

 * When executing on a Java 9+ JVM, this plugin uses the host Java compiler.
 * When executing on a Java 8 JVM, this plugin uses a Java 9 compiler
   (in particular, the Error Prone Java compiler from
   [com.google.errorprone:javac](https://mvnrepository.com/artifact/com.google.errorprone/javac)).

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

## Using a locally-built plugin

You can build the plugin locally rather than downloading it from Maven Central.

To build the plugin from source, run `./gradlew build`.

If you want to use a locally-built version of the plugin, you can publish the plugin to your
local Maven repository by running `./gradlew publish`. In the `build.gradle` file for each
project for which you want to use the locally-built plugin, make sure that `mavenLocal()`
is the first entry in the `repositories` block within the `buildscript` block. A full example
will look like this:

```groovy
buildscript {
  repositories {
    mavenLocal()
  }

  dependencies {
    classpath 'gradle.plugin.org.checkerframework:checkerframework-gradle-plugin:0.4.6-SNAPSHOT'
  }
}

apply plugin: 'org.checkerframework'
```

## Credits

This project started as a fork of [a plugin built by jaredsburrows](https://github.com/jaredsburrows/gradle-checker-framework-plugin).
[![Twitter Follow](https://img.shields.io/twitter/follow/jaredsburrows.svg?style=social)](https://twitter.com/jaredsburrows)


## License

    Copyright (C) 2017 Jared Burrows, 2019 Martin Kellogg

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
