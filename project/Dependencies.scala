import Versions._
import sbt._

object Dependencies {

  lazy val all: Seq[ModuleID] = Seq(
    "org.typelevel" %% "cats-effect" % catsEffectVersion,
    "org.http4s" %% "http4s-dsl" % http4sVersion,
    "org.http4s" %% "http4s-ember-server" % http4sVersion,
    "org.http4s" %% "http4s-circe" % http4sVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-fs2" % circeVersion,
    "io.github.iltotore" %% "iron" % ironVersion,
    "org.tpolecat" %% "doobie-core" % doobieVersion,
    "org.tpolecat" %% "doobie-hikari" % doobieVersion,
    "org.tpolecat" %% "doobie-postgres" % doobieVersion,
    "com.github.pureconfig" %% "pureconfig-core" % pureConfigVersion,
    "org.typelevel" %% "log4cats-slf4j" % log4catsVersion,
    "org.slf4j" % "slf4j-simple" % slf4jVersion,
    "io.github.jmcardon" %% "tsec-http4s" % tsecVersion,
    "com.sun.mail" % "javax.mail" % javaMailVersion
  ) ++ testDependencies

  lazy val testDependencies: Seq[ModuleID] = Seq(
    "org.tpolecat" %% "doobie-scalatest" % doobieVersion % "it,test",
    "org.typelevel" %% "log4cats-noop" % log4catsVersion % "it,test",
    "org.scalatest" %% "scalatest" % scalaTestVersion % "it,test",
    "org.scalactic" %% "scalactic" % scalaTestVersion % "it,test",
    "org.typelevel" %% "cats-effect-testing-scalatest" % scalaTestCatsEffectVersion % "it,test",
    "org.testcontainers" % "testcontainers" % testContainerVersion % "it,test",
    "org.testcontainers" % "postgresql" % testContainerVersion % "it,test",
    "ch.qos.logback" % "logback-classic" % logbackVersion % "it,test",
    "org.scalacheck" %% "scalacheck" % scalacheckVersion % "it,test",
    "org.scalatestplus" %% "scalacheck-1-17" % scalaTestPlusVersion % "it,test"
  )

}
