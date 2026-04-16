ThisBuild / scalaVersion := "2.13.14"
ThisBuild / version      := "0.1.0"

lazy val root = (project in file("."))
  .settings(
    name := "gov-ai-hackathon",
    libraryDependencies ++= Seq(
      "org.seleniumhq.selenium" % "selenium-java"    % "4.21.0",
      "io.github.bonigarcia"    % "webdrivermanager" % "5.9.1"
    )
  )
