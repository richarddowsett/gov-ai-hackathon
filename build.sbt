lazy val storageService = (project in file("journey-storage"))
  .enablePlugins(PlayScala)
  .settings(
    name         := "journey-storage",
    version      := "0.1.0",
    scalaVersion := "2.13.16",
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
  .settings(
    name         := "journey-validator",
    version      := "0.1.0",
    scalaVersion := "2.13.16",
    libraryDependencies ++= Seq(
      "org.playframework" %% "play-json"   % "3.0.4",
      "org.jsoup"          % "jsoup"        % "1.18.1",
      "org.scalatest"     %% "scalatest"    % "3.2.19"  % Test,
      "org.slf4j"          % "slf4j-simple" % "2.0.16"  % Test
    ),
    Test / fork := true,
    Test / javaOptions += "-Duser.dir=" + baseDirectory.value.getAbsolutePath,
    Test / javaOptions ++= {
      val props = Seq("journey.json", "prototype.dir", "service.json")
      props.flatMap(key => sys.props.get(key).map(v => s"-D$key=$v"))
    }
  )
