name := """lucene-morphologicalanalyzer"""

organization := "fi.seco"

version := "1.0.0"

scalaVersion := "2.12.1"

crossScalaVersions := Seq("2.10.6", "2.11.8", "2.12.1")

resolvers ++= Seq(
    Resolver.mavenLocal
)

libraryDependencies ++= Seq(
  "org.apache.lucene" % "lucene-core" % "6.3.0",
  "fi.seco" % "lexicalanalysis" % "1.5.6"
)

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}
