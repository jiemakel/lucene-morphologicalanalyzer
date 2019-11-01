name := """lucene-morphologicalanalyzer"""

organization := "fi.seco"

version := "1.2.1"

scalaVersion := "2.13.1"

crossScalaVersions := Seq("2.11.12","2.12.10")

resolvers ++= Seq(
    Resolver.mavenLocal
)

libraryDependencies ++= Seq(
  "org.apache.lucene" % "lucene-core" % "8.0.0",
  "org.apache.lucene" % "lucene-queryparser" % "8.0.0",
  "fi.seco" % "lexicalanalysis" % "1.5.16",
  "junit" % "junit" % "4.12" % "test",
  "fi.seco" % "lexicalanalysis-resources-fi-core" % "1.5.16" % "test"
)

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}
