
name := "spark-twitter-produce"

version := "1.0"

scalaVersion := "2.11.11"

resolvers += "Confluent" at "http://packages.confluent.io/maven"

val sparkVersion = "2.0.2"

lazy val sparkDependencies = Seq(
"org.apache.spark" %% "spark-streaming-kafka-0-10" % sparkVersion,
"org.apache.spark" %% "spark-sql" % sparkVersion,
"org.apache.spark" %% "spark-core" % sparkVersion,
"org.apache.spark" %% "spark-streaming" % sparkVersion,
"org.apache.spark" %% "spark-hive" % sparkVersion
)

libraryDependencies ++= sparkDependencies.map(_ % "compile")
libraryDependencies += "commons-configuration" % "commons-configuration" % "1.6"
libraryDependencies += "com.databricks" %% "spark-avro" % "3.2.0"
libraryDependencies += "io.confluent" % "kafka-avro-serializer" % "3.3.0" exclude("com.fasterxml.jackson.core", "jackson-databind")
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.6.5"

// Producer only
libraryDependencies += "org.twitter4j" % "twitter4j-stream" % "4.0.4"
