ThisBuild / scalaVersion := "3.3.6"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name := "ai-journey-contract-validator",
    libraryDependencies ++= Seq(
      guice,
      "com.lihaoyi" %% "ujson" % "3.3.1",
      "com.networknt" % "json-schema-validator" % "1.5.6",
      "org.jsoup" % "jsoup" % "1.18.1",
      "uk.gov.hmrc" %% "bootstrap-frontend-play-30" % "10.5.0",
      "uk.gov.hmrc" %% "play-frontend-hmrc-play-30" % "13.2.0",
      "org.scalatest" %% "scalatest" % "3.2.19" % Test
    ),
    dependencyOverrides ++= Seq(
      "com.fasterxml.jackson.core" % "jackson-core" % "2.15.4",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.15.4",
      "com.fasterxml.jackson.core" % "jackson-annotations" % "2.15.4",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.15.4"
    ),
    Compile / unmanagedSourceDirectories += baseDirectory.value / "src" / "main" / "scala",
    Test / unmanagedSourceDirectories += baseDirectory.value / "src" / "test" / "scala",
    resolvers += "HMRC Releases" at "https://open.artefacts.tax.service.gov.uk/maven2",
    Test / parallelExecution := false
  )
