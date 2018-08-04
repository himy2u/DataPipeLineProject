import java.io.FileNotFoundException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.amazon.exceptions.jdbc4.JDBC4ExceptionConverter
import com.typesafe.config.ConfigFactory
import org.apache.spark.sql.{SaveMode, SparkSession}
import org.apache.spark.sql.functions._

import scala.util.control.Exception

object SourcesDataLoading {
  def main(args: Array[String]): Unit = {

    //SparkSession creation

    val sparksession = SparkSession.builder().master("local[*]").appName("DataPipelineDemo").getOrCreate()
    sparksession.sparkContext.hadoopConfiguration.set("fs.s3n.awsAccessKeyId", "AKIAJMKPMQABLQT34EXA")
    sparksession.sparkContext.hadoopConfiguration.set("fs.s3n.awsSecretAccessKey", "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")


    //Source Data Reading
    try {

      //getting all configuration list
      val config = ConfigFactory.load("application.conf").getConfig("conf")

      //getting source list
      val srcList = config.getStringList("source_list").toArray()

      //iterating through list

      for (src <- srcList) {
        src match {
          case "OL" =>
            //getting only SFTP configuration
            val olSftpConf = config.getConfig("sftp_ol")
            val redshiftConf = config.getConfig("redshift_dm")
            val jdbcHostname = redshiftConf.getString("hostname")
            val jdbcPort = redshiftConf.getString("port")
            val jdbcDatabase = redshiftConf.getString("database")
            val jdbcUsername = redshiftConf.getString("username")
            val jdbcPassword = redshiftConf.getString("password")

            //forming Redshift url
            var redshiftJdbcUrl = s"jdbc:redshift://${jdbcHostname}:${jdbcPort}/${jdbcDatabase}?user=${jdbcUsername}&password=${jdbcPassword}"

            //reading file path from sftp
            var filePath = olSftpConf.getString("filepath")

            //
            var currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy"))
            currentDate = "14_10_2017"
            filePath = filePath.replace("DATE", currentDate)

            val olDataFrameSFTP = sparksession.read.
              format("com.springml.spark.sftp").
              option(Constants.host, olSftpConf.getString(Constants.host)).
              option(Constants.port, olSftpConf.getString(Constants.port)).
              option(Constants.username, olSftpConf.getString(Constants.username)).
              option(Constants.password, olSftpConf.getString(Constants.password)).
              option(Constants.fileType, olSftpConf.getString(Constants.fileType)).
              option(Constants.delimiter, olSftpConf.getString(Constants.delimiter)).
              option("inferSchema", true).
              load(filePath)

            //converting NULL to @NULL@ in order to strore null value inside Redshift

            val formattedOlTxnDf = olDataFrameSFTP.select(
              olDataFrameSFTP.columns.map(c => (when(col(c) === "NULL", "@NULL@")
                .otherwise(col(c))).alias(c)): _*)

            //Adding extra colmun to dataframe
            val dfSFTP = formattedOlTxnDf.withColumn("ins_ts", current_timestamp())

            dfSFTP.write
              .format("com.databricks.spark.redshift")
              .option(Constants.redShiftUrl, redshiftJdbcUrl)
              .option(Constants.tempdir, redshiftConf.getString(Constants.tempdir) + redshiftConf.getString(Constants.redShiftDbTable))
              .option("forward_spark_s3_credentials", "true")
              .option("extracopyoptions", "EMPTYASNULL")
              .option("tempformat", "CSV")
              .option("csvnullstring", "@NULL@")
              .option(Constants.redShiftDbTable, redshiftConf.getString(Constants.redShiftDbTable))
              .mode(SaveMode.Overwrite)
              .save()
          case "ORA" =>

            val olOracleConf = config.getConfig("oracle_ol")
            val redshiftConf = config.getConfig("redshift_dm")
            val jdbcHostname = redshiftConf.getString("hostname")
            val jdbcPort = redshiftConf.getString("port")
            val jdbcDatabase = redshiftConf.getString("database")
            val jdbcUsername = redshiftConf.getString("username")
            val jdbcPassword = redshiftConf.getString("password")

            //forming Redshift url
            var redshiftJdbcUrl = s"jdbc:redshift://${jdbcHostname}:${jdbcPort}/${jdbcDatabase}?user=${jdbcUsername}&password=${jdbcPassword}"

            val olDataFrameOracle = sparksession.read.
              format("jdbc").
              option(Constants.oracledriver, olOracleConf.getString(Constants.oracledriver)).
              option(Constants.oracleurl, olOracleConf.getString(Constants.oracleurl)).
              option("user", olOracleConf.getString("user")).
              option("password", olOracleConf.getString("password")).
              option("db", olOracleConf.getString("db")).
              option("dbtable", olOracleConf.getString("dbtable")).
              option("inferSchema", true).
              load()


            olDataFrameOracle.write
              .format("com.databricks.spark.redshift")
              .option(Constants.redShiftUrl, redshiftJdbcUrl)
              .option(Constants.tempdir, redshiftConf.getString(Constants.tempdir) + redshiftConf.getString(Constants.redShiftDbTable))
              .option("forward_spark_s3_credentials", "true")
              .option("extracopyoptions", "EMPTYASNULL")
              .option("tempformat", "CSV")
              .option("csvnullstring", "@NULL@")
              .option(Constants.redShiftDbTable, redshiftConf.getString(Constants.redShiftDbTable))
              .mode(SaveMode.Overwrite)
              .save()

          case "1CP" => //one customer portal UK market

            val cpConfig = config.getConfig("1cp")
            val redshiftConf = config.getConfig("redshift_conf")

            val jdbcHostname = redshiftConf.getString("host")
            val jdbcPort = redshiftConf.getString("jdbcport")
            val jdbcDatabase = redshiftConf.getString("jdbcdatabase")
            val jdbcUsername = redshiftConf.getString("jdbcUsername")
            val jdbcPassword = redshiftConf.getString("jdbcpassword")
            //jdbc URL 'S' string formatter
            var redshiftJdbcUrl = s"jdbc:redshift://${jdbcHostname}:${jdbcPort}" +
              s"/${jdbcDatabase}?user=${jdbcUsername}&password=${jdbcPassword}"


            var filePath = cpConfig.getString(Constants.filepath)
            // Date
            var currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            currentDate = "20171009"
            // adding date to the file
            filePath = filePath.replace("DATE", currentDate)

            val customerData = sparkSession.read //.csv("s3n://vinay-buck/1CP_UK/KC_Extract_1_20171009.csv")
              .format("csv")
              .option("header", "true")
              .option("delimiter", "|")
              .option(Constants.filepath, cpConfig.getString(Constants.filepath)).load(filePath)
            // customerData.show(10)
            //.csv(cpConfig.Constants.filepath)

            val newcustomerData = customerData.withColumn("INS_TS", current_timestamp())
            //write into REDSHIFT
            newcustomerData.write
              .format(redshiftConf.getString(Constants.format))
              .option(Constants.url, redshiftJdbcUrl)
              .option("tempdir", redshiftConf.getString(Constants.tempdir) + redshiftConf.getString(Constants.cptable))
              .option("forward_spark_s3_credentials", "true")
              .option("extracopyoptions", "EMPTYASNULL")
              .option("tempformat", "CSV")
              .option("csvnullstring", "@NULL@")
              .option("dbtable", redshiftConf.getString(Constants.cptable))
              .mode(SaveMode.Overwrite)
              .save()
        }

      }
    }

    //Start RedShift Reading

    //      val olRedshiftConf = config.getConfig("redshift_ol")

    //      val olDataFrameRedShift = sparksession.read.
    //        format("com.databricks.spark.redshift").
    //        option(Constants.redShiftUrl, olRedshiftConf.getString(Constants.redShiftUrl)).
    //        option(Constants.redShiftDbTable, olRedshiftConf.getString(Constants.redShiftDbTable)).
    //        option("tempdir", "s3n://pallab-s3/TempSftp").
    //        option("forward_spark_s3_credentials", "true").
    //        option("inferSchema", true).
    //        load().show(10)

    // End Redshift
    catch {
      case ex1: FileNotFoundException => println("File not found/Invalid path")
      case ex2: JDBC4ExceptionConverter => println("Cannot connect JDBC connection")
      case ex: Exception => println("Exception :- " + ex)
    }
    finally {
      sparksession.close()
    }
  }

}
