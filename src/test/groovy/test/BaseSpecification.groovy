package test

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class BaseSpecification extends Specification {
  @Rule TemporaryFolder testProjectDir = new TemporaryFolder()
  def buildFile

  def "setup"() {
    buildFile = testProjectDir.newFile("build.gradle")
  }
}
