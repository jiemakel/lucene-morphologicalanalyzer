name := """lucene-morphologicalanalyzer"""

organization := "fi.seco"

version := "1.1.3"

scalaVersion := "2.12.4"

crossScalaVersions := Seq("2.10.6", "2.11.11")

resolvers ++= Seq(
    Resolver.mavenLocal
)

libraryDependencies ++= Seq(
  "org.apache.lucene" % "lucene-core" % "7.1.0",
  "fi.seco" % "lexicalanalysis" % "1.5.13",
  "junit" % "junit" % "4.12" % "test",
  "fi.seco" % "lexicalanalysis-resources-fi" % "1.5.13" % "test"
)

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}
