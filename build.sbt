name := "china-data-streaming"

version := "1.3.2"

logLevel := Level.Debug

val sparkVersion = "2.4.4"
val hadoopVersion = "2.7.3"
val awsSdkVersion = "1.11.646"

scalaVersion := "2.11.12"

// lazy val root = (project in file("."))
//     .settings(
//         name := "Kinesis Data Transfromation",
//         scalaVersion := "2.11.8"
//     )

assemblyJarName in assembly := "china-data-streaming.jar"

assemblyMergeStrategy in assembly := {
    case PathList("META-INF", xs @ _*) => MergeStrategy.discard
    case x => MergeStrategy.last
}

libraryDependencies ++= Seq(
    "org.apache.spark" %% "spark-core" % sparkVersion,
    "org.apache.spark" %% "spark-sql" % sparkVersion,
    "org.apache.spark" %% "spark-hive" % sparkVersion,
    "org.apache.spark" %% "spark-mllib" % sparkVersion,
    "org.apache.spark" %% "spark-streaming" % sparkVersion,
    "org.apache.spark" %% "spark-streaming-kinesis-asl" % sparkVersion,
    "org.apache.hadoop" % "hadoop-common" % hadoopVersion,
    "org.apache.hadoop" % "hadoop-aws" % hadoopVersion,
    "org.apache.httpcomponents" % "httpclient" % "4.5.10",
    "com.amazonaws" % "aws-java-sdk" % awsSdkVersion,
    "com.amazonaws" % "aws-java-sdk-kms" % awsSdkVersion,
    "com.amazonaws" % "aws-java-sdk-core" % awsSdkVersion,
    "com.amazonaws" % "aws-java-sdk-bundle" % awsSdkVersion,
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.2.3",
    "com.fasterxml.jackson.core" % "jackson-annotations" % "2.2.3",
    "com.typesafe" % "config" % "1.3.1"
)