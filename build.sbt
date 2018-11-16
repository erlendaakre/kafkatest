lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "kafkatest",
      scalaVersion := "2.12.7",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "kafkatest",
    Compile/mainClass := Some("kafkatest.Main"),
    libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.3.0-M26",
    libraryDependencies += "org.scalaz" %% "scalaz-zio" % "0.3.2",
    libraryDependencies += "org.apache.kafka" % "kafka-clients" % "2.0.1"
  )
