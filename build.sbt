name := "datapipeline_demo"

version := "0.1"

scalaVersion := "2.11.8"

libraryDependencies += "org.apache.spark" % "spark-core_2.11" % "2.1.1"
libraryDependencies += "org.apache.spark" % "spark-sql_2.11" % "2.1.1"
libraryDependencies += "org.apache.hadoop" % "hadoop-aws" % "2.7.4"
libraryDependencies += "com.oracle" % "ojdbc14" % "10.2.0.4.0"
libraryDependencies += "com.databricks" % "spark-redshift_2.11" % "3.0.0-preview1"
libraryDependencies += "com.amazon.redshift" % "redshift-jdbc42" % "1.2.1.1001"
libraryDependencies += "com.springml" % "spark-sftp_2.11" % "1.1.1"
dependencyOverrides += "com.databricks" % "spark-avro_2.11" % "3.2.0"
libraryDependencies += "com.typesafe" % "config" % "1.2.1"
