name := "macwire-test"

organization := "com.github.dnvriend"

version := "1.0.0"

scalaVersion := "2.11.8"

// functional and typelevel programming
// https://github.com/scalaz/scalaz
libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.2.8"
// https://github.com/mpilquist/simulacrum
libraryDependencies += "com.github.mpilquist" %% "simulacrum" % "0.10.0"
// https://github.com/milessabin/shapeless
//libraryDependencies += "com.chuusai" %% "shapeless" % "2.3.2"

// compile-time DI (only used at compile-time so in "provided" scope)
libraryDependencies += "com.softwaremill.macwire" %% "macros" % "2.2.5" % "provided"

// testing
// https://github.com/typelevel/scalaz-scalatest
libraryDependencies += "org.typelevel" %% "scalaz-scalatest" % "1.1.1" % Test
// https://www.playframework.com/documentation/2.5.x/ScalaTestingWithScalaTest#Mockito  
//libraryDependencies += "org.mockito" % "mockito-core" % "2.2.21" % Test
// https://github.com/scalatest/scalatest
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1"

// testing configuration
fork in Test := true

parallelExecution := false

licenses +=("Apache-2.0", url("http://opensource.org/licenses/apache2.0.php"))

// enable scala code formatting //
import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform

// Scalariform settings
SbtScalariform.autoImport.scalariformPreferences := SbtScalariform.autoImport.scalariformPreferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
  .setPreference(DoubleIndentClassDeclaration, true)

// enable updating file headers //
import de.heikoseeberger.sbtheader.license.Apache2_0

headers := Map(
  "scala" -> Apache2_0("2016", "Dennis Vriend"),
  "conf" -> Apache2_0("2016", "Dennis Vriend", "#")
)

enablePlugins(AutomateHeaderPlugin, SbtScalariform)