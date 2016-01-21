name := "script_graphics"

version := "0.0.1"

scalaVersion := "2.11.7"

libraryDependencies := Seq(
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test")

lazy val myProject = Project("scala_parser", file("."))
  .dependsOn(soundPlayerProject)

lazy val soundPlayerProject =
  ProjectRef(uri("https://github.com/dpblh/scala-parser.git#master"), "scala-parser")