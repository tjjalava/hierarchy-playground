import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "hierarchy-playground"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "com.typesafe.slick" %% "slick" % "1.0.1",
    "org.postgresql" % "postgresql" % "9.2-1003-jdbc4",
    jdbc,
    anorm
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
  )

}
