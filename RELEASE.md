## Making a release of the Checker Framework Gradle plugin

This document describes how to make a release of the Checker Framework
Gradle plugin, which this repository contains.

### Prequisites

* Access to the publishing credentials, which should be copied
to your `~/.gradle/gradle.properties` file. To gain access to the
credentials used for publishing, contact one of the maintainers
privately. The credentials are stored in the same place as the
credentials used to make a standard Checker Framework release,
in the `gradle.properties` file.

### Release process

1. Choose a new version string. Please try to respect 
[semantic versioning](https://semver.org/). Do not augment the major
version without explicit approval from all the maintainers.
2. Change the version of the plugin. Search for the current plugin version
in the directory containing this file, and replace all instances of it
with the new version string.
3. Run `./gradlew publishPlugins` from the top-level project directory
(which should also contain this file).