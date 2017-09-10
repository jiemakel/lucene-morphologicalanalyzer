name := """lucene-morphologicalanalyzer"""

organization := "fi.seco"

version := "1.1.2"

scalaVersion := "2.12.3"

crossScalaVersions := Seq("2.10.6", "2.11.8")

resolvers ++= Seq(
    Resolver.mavenLocal
)

libraryDependencies ++= Seq(
  "org.apache.lucene" % "lucene-core" % "6.5.1",
  "fi.seco" % "lexicalanalysis" % "1.5.11"
)

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}
