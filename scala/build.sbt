resolvers in ThisBuild ++= Seq(
  "Apache Development Snapshot Repository" at "https://repository.apache.org/content/repositories/snapshots/",
  Resolver.mavenLocal
)

name := "chgeuername"

version := "0.1-SNAPSHOT"

organization := "com.microsoft.chgeuer"

scalaVersion in ThisBuild := "2.11.8"

val flinkVersion = "1.2.0"

val flinkDependencies = Seq(
  "org.apache.flink" %% "flink-scala" % flinkVersion % "provided",
  "org.apache.flink" %% "flink-streaming-scala" % flinkVersion % "provided",
  "org.apache.flink" %% "flink-connector-kafka-0.10" % flinkVersion
)

lazy val root = (project in file(".")).
  settings(
    libraryDependencies ++= flinkDependencies
  )

mainClass in assembly := Some("com.microsoft.chgeuer.ScalaJob")

// make run command include the provided dependencies
run in Compile := Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run))

// exclude Scala library from assembly
assemblyOption in assembly := (assemblyOption in assembly).value.copy(
  includeScala = false,
  includeDependency = true
)

// PB.pythonExe := "\"C:\\Program Files\\Python36\\Python.exe\""
PB.pythonExe := "\"C:\\Python27\\Python.exe\""
PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)
// If you need scalapb/scalapb.proto or anything from google/protobuf/*.proto
libraryDependencies += "com.trueaccord.scalapb" %% "scalapb-runtime" % com.trueaccord.scalapb.compiler.Version.scalapbVersion % "protobuf"
