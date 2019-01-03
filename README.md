# Checker Framework Gradle Plugin

This plugin runs custom typecheckers from the
[Checker Framework](https://checkerframework.org)
during the build process.

The plugin works by modifying the arguments to `JavaCompile`
tasks in the Gradle build so that the typechecker is run
as part of the compilation process. By default, this plugin
adds the typecheckers you specify to all `JavaCompile` tasks
in your project.

### Adding dependencies

To use the Checker Framework in your Gradle build, use this
plugin by modifying your `build.gradle` file:

```groovy
plugins {
    ...

    // Checker Framework build logic
    id 'org.checkerframework' version '0.1.0'
}
```

You will also need to add a dependency on the Checker Framework
itself:

```groovy
dependencies {
    ...

    annotationProcessor 'org.checkerframework:checker:2.+'
    annotationProcessor 'org.checkerframework:jdk8:2.+'
}
```

You can use a local version of the Checker Framework by
declaring a local dependency instead of a dependency on
the Maven artifact. See
https://docs.gradle.org/current/userguide/declaring_dependencies.html.

### Specifying a checker

After adding dependencies, you must specify the typechecker(s) you want to run. For
example, to run the
 [Nullness Checker](https://checkerframework.org/manual/#nullness-checker):
 
```groovy
checkerframework.addChecker('org.checkerframework.checker.nullness.NullnessChecker')
```

If you call `checkerframework.addChecker` multiple times, then multiple
checkers will be run during compilation.

### Customizing which tasks the checker should run on

By default, the custom typechecker is run on the `:compileJava` task.

You can manually specify one or more other tasks by calling the 
`checkerframework.addTask` method:

```groovy
checkerframework.addTask('myCustomCompileTask')
```

If there are any calls to `checkerframework.addTask`, then the custom
typecheckers are not run on the `compileJava` task unless you explicitly
specify it.

### Passing arguments to a typechecker

Typecheckers run as part of the standard invocation of `javac`,
not as a separate step. You can therefore pass arguments to the
typechecker by adding them to the compile options of the
appropriate tasks. See the documentation for the `JavaCompile`
task (https://docs.gradle.org/current/dsl/org.gradle.api.tasks.compile.JavaCompile.html)
and for the `CompileOptions` object
(https://docs.gradle.org/current/dsl/org.gradle.api.tasks.compile.CompileOptions.html).

In a future release, passing arguments only to tasks during
which the typechecker is run will be supported.
