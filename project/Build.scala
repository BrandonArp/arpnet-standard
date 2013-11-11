import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "arpnet-standard"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    javaCore,
    javaJdbc,
    javaEbean
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    javacOptions ++= Seq("-Xlint:deprecation", "-Xlint:unchecked"),

    organization := "com.arpnetworking"
  )
}
