ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(Defaults.itSettings)
  .settings(
    organization := Versions.organization,
    scalaVersion := Versions.scala3Version,
    name := "typelevel-jobsboard",
    idePackagePrefix.withRank(KeyRanks.Invisible) := Some("com.github.dpratt747"),
    libraryDependencies := Dependencies.all,
    scalafmtOnCompile := true,
    IntegrationTest / testForkedParallel := true,
    IntegrationTest / dependencyClasspath := (IntegrationTest / dependencyClasspath).value
      ++ (Test / exportedProducts).value
  )
