# Checker Framework Gradle Plugin

This plugin runs custom typecheckers from the
[Checker Framework](https://checkerframework.org)
during the build process.

The plugin works by modifying the arguments to `JavaCompile`
tasks in the Gradle build so that the typechecker is run
as part of the compilation process. By default, this plugin
adds any typecheckers you specify to the `:compileJava` task
that the `java` plugin provides.

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
