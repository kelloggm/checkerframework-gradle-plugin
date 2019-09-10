## Making and merging pull requests

The Checker Framework Gradle plugin (which this repository contains)
uses a continuous-delivery style of releases: any time that the
behavior of the plugin changes, a new release is cut.

### Making a pull request

All pull requests should be made from a feature branch. Never push
directly to `master`.

After you have made the changes you wish to merge into `master`,
you should:
1. If your pull request will change any behavior
of the plugin (including bug fixes and new features), you should choose
a new version string. Please try to respect
[semantic versioning](https://semver.org/). Do not augment the major
version without explicit approval from all the maintainers.
2. If you changed the version string, search for the current plugin version
in the directory containing this file, and replace all instances of it
with the new version string. Commit the result.
3. Push to your feature branch.
4. Make a pull request against the `master` branch.

### Merging a pull request

#### Prequisites

* Copy the publishing credentials to your `~/.gradle/gradle.properties` file.
The credentials are stored in the same place as the credentials used to make
a Checker Framework release, in a `gradle.properties` file.
To gain access to the credentials, contact one of the maintainers privately.

#### Release process

1. Review the pull request. If you approve it, proceed.
2. Ensure that the
[Travis build](https://travis-ci.com/kelloggm/checkerframework-gradle-plugin/branches)
passes in the pull request.
3. Ensure that the new version string in the pull request is not already in use. Check 
[the plugin's portal page](https://plugins.gradle.org/plugin/org.checkerframework)
to see if this version has already been released.
4. If the version string is already in use, either change it yourself and push to the feature
branch or ask the author of the pull request to change the version string.
5. Squash and merge the pull request.
6. On your local machine, check out the `master` branch and run `git pull origin master`.
7. Run `./gradlew publishPlugins` from the top-level project directory
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
