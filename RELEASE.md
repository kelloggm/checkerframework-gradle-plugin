## Making and merging pull requests

The Checker Framework Gradle plugin (which this repository contains)
uses a continuous-delivery style of releases: any time that the
behavior of the plugin changes, a new release is cut.

### Making a pull request

All pull requests should be made from a feature branch. Never push
directly to `master`.

When you make a pull request that will change any behavior
of the plugin (including bug fixes and new features), update the version string on 
your feature branch using these steps:
1. Make the changes you wish to merge into the plugin.
2. Choose a new version string. Please try to respect
[semantic versioning](https://semver.org/). Do not augment the major
version without explicit approval from all the maintainers.
3. Change the version of the plugin. Search for the current plugin version
in the directory containing this file, and replace all instances of it
with the new version string. Commit the result.
4. Push your feature branch and make a pull request against the `master` branch.

### Merging a pull request

#### Prequisites

* Copy the publishing credentials to your `~/.gradle/gradle.properties` file.
The credentials are stored in the same place as the credentials used to make
a Checker Framework release, in a `gradle.properties` file.
To gain access to the credentials, contact one of the maintainers privately.

#### Release process

1. Review the pull request. If it is approved, proceed.
2. Ensure that the
[Travis build](https://travis-ci.com/kelloggm/checkerframework-gradle-plugin/branches)
is passing on the pull request branch.
3. Squash and merge the pull request.
4. On your local machine, check out the `master` branch and run `git pull origin master`.
5. Run `./gradlew publishPlugins` from the top-level project directory
(which should also contain this file).

### Updating the Checker Framework version

The default version of the Checker Framework used by the plugin
should be updated after every Checker Framework release. 
The Checker Framework is typically released monthly.
Updating the Checker Framework should
be done like any other pull request. To update the plugin to
use the [latest version](https://github.com/typetools/checker-framework/blob/master/changelog.txt)
of the Checker Framework:
   * Run: `(cd src/test/resources/maven/org/checkerframework && update.sh NEW_CF_VERSION_NUMBER)`
   * Replace the old version number anywhere it still appears in this repository.
