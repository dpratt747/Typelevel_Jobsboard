ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
    organization := Versions.organization,
    scalaVersion := Versions.scala3Version,
    name := "typelevel-jobboard",
    idePackagePrefix.withRank(KeyRanks.Invisible) := Some("com.github.dpratt747"),
    libraryDependencies := Dependencies.all
  )
