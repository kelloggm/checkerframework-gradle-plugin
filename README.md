# Checker Framework Gradle Plugin

This plugin provides re-usable build logic for programmers who
would like to use the 
[Checker Framework](https://checkerframework.org)
to run custom typecheckers during their build process.

The plugin works by modifying the arguments to `JavaCompile`
tasks in the Gradle build so that the typechecker is run
as part of the compilation process. By default, this plugin
adds any typecheckers you specify to the `:compileJava` task
that the `'java'` plugin provides.

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

After adding dependencies, you can use the DSL provided by this
plugin to specify the typechecker(s) you want to run. For
example, to run the
 [Nullness Checker](https://checkerframework.org/manual/#nullness-checker):
 
 ```groovy
 checkerframework.addChecker('org.checkerframework.checker.nullness.NullnessChecker')
```

You can specify as many checkers as you like by calling this
method multiple times. Each checker you specify will run as
part of the compilation target.

### Customizing which tasks the checker should run on

Instead of only running on the default `:compileJava` task,
you can manually specify tasks by calling the 
`checkerframework.addTask` method:

```groovy
checkerframework.addTask('myCustomCompileTask')
```

You can specify one or more tasks this way. If you do,
the checkers you specify will only be run on those tasks.