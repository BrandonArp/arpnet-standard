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

  val s = Defaults.defaultSettings ++ play.Project.playJavaSettings

  val main = play.Project(appName, appVersion, appDependencies, settings = s).settings(
    javacOptions ++= Seq("-Xlint:deprecation", "-Xlint:unchecked"),

    name := "arpnet-standard",
    description := "Play module to setup a base set of standard monitoring and performance logging",
    organization := "com.arpnetworking",
    resolvers += "Local Maven" at Path.userHome.asFile.toURI.toURL + ".m2/repository",

    pomIncludeRepository := { _ => false },
    publishMavenStyle := true,
    publishTo <<= version {
      (v: String) =>
        val nexus = "https://oss.sonatype.org/"
        if (v.trim.endsWith("SNAPSHOT"))
          Some("snapshots" at nexus + "content/repositories/snapshots")
        else
          Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },

    licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
    homepage := Some (url("https://github.com/BrandonArp/arpnet-standard")),
    pomExtra := (
        <scm>
          <url>git@github.com:BrandonArp/arpnet-standard.git</url>
          <connection>scm:git:git@github.com:BrandonArp/arpnet-standard.git</connection>
        </scm>
        <developers>
          <developer>
            <id>barp</id>
            <name>Brandon Arp</name>
            <email>brandonarp@gmail.com</email>
          </developer>
        </developers>)
  )
}
