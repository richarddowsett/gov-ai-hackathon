lazy val commonSettings = Seq(
  scalaVersion := "2.13.16",
  scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")
)

lazy val journeyValidation = (project in file("journey-validation"))
  .settings(commonSettings)
  .settings(
    name    := "journey-validation",
    version := "0.1.0",
    libraryDependencies ++= Seq(
      "org.playframework"      %% "play-json"         % "3.0.4",
      "org.jsoup"               % "jsoup"             % "1.18.1",
      "org.scalatest"          %% "scalatest"          % "3.2.19",
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1"
    )
  )

lazy val exampleService = (project in file("example-service"))
  .enablePlugins(PlayScala)
  .dependsOn(journeyValidation)
  .settings(commonSettings)
  .settings(
    name := "example-service",
    libraryDependencies ++= Seq(
      guice,
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test
    )
  )

lazy val storageService = (project in file("journey-storage"))
  .enablePlugins(PlayScala)
  .settings(commonSettings)
  .settings(
    name := "journey-storage",
    libraryDependencies ++= Seq(
      guice,
      jdbc,
      filters,
      evolutions,
      "org.postgresql" % "postgresql" % "42.7.3"
    )
  )

addCommandAlias("journeyStorage", "storageService/run")

lazy val root = (project in file("."))
  .aggregate(journeyValidation, exampleService, storageService)
  .settings(
    name           := "journey-validator",
    publish / skip := true,
    Compile / unmanagedSourceDirectories := Seq.empty,
    Test / unmanagedSourceDirectories    := Seq.empty
  )
