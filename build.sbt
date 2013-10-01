import play.Project._

name := "hierarchy-playground"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "1.0.1",
  "org.postgresql" % "postgresql" % "9.2-1003-jdbc4",
  "org.neo4j" % "neo4j" % "2.0.0-M05",
  jdbc,
  anorm
)

play.Project.playScalaSettings

scalacOptions ++= Seq("-feature")
