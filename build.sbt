name := "krs"

scalaVersion := "2.11.8"

com.twitter.scrooge.ScroogeSBT.newSettings

scalariformSettings

val finchVersion = "0.11.0-M4"
val circeVersion = "0.5.3"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.0",
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "com.github.finagle" %% "finch-core" % finchVersion,
  "com.github.finagle" %% "finch-circe" % finchVersion,
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "com.twitter" %% "twitter-server" % "1.25.0",
  "com.twitter" %% "finagle-stats" % "6.40.0",
  "com.twitter.common" % "metrics" % "0.0.38",
  "com.twitter" %% "scrooge-core" % "4.12.0",
  "org.apache.thrift" % "libthrift" % "0.9.3",
  "com.typesafe" % "config" % "1.3.1"
)

addCommandAlias("full", ";clean ;compile ;test ;scalastyle")