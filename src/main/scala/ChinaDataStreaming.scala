import java.lang

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.internal.Logging
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials, AWSCredentials}
import com.amazonaws.services.kinesis.AmazonKinesisClient
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream
import com.typesafe.config.ConfigFactory
import org.apache.spark.SparkConf
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kinesis.KinesisUtils
import org.apache.spark.streaming.{Milliseconds, Minutes, Seconds, StreamingContext}
import org.apache.spark.streaming.kinesis.DefaultCredentials
import org.apache.log4j.{Level, LogManager, PropertyConfigurator}
import com.amazonaws.services.s3._
import org.apache.hadoop.fs._
import org.apache.spark.rdd.RDD
import org.apache.spark.sql._
import org.apache.spark.sql.types._
import org.apache.spark.sql.functions._
import java.lang.System.currentTimeMillis
import java.nio.ByteBuffer

// JSON Parsing
import scala.util.parsing.json.JSON


object ChinaDataStreaming {

    def main(args: Array[String]) {

        val conf = new SparkConf().setAppName("prod-kinesis-data-streaming")
        // conf.setIfMissing("spark.master", "local[*]") // Remove for cluster mode deploy

        // Set log
        val log = LogManager.getRootLogger
        log.setLevel(Level.WARN)

        // Load kinesis config
        val kinesisConf = ConfigFactory.load.getConfig("kinesis")

        val appName = kinesisConf.getString("appName")
        val streamName = kinesisConf.getString("streamName")
        val endpointUrl = kinesisConf.getString("endpointUrl")
        val regionName = kinesisConf.getString("regionName")

        // Load Chinese kinesis config
        val kinesisConfChina = ConfigFactory.load.getConfig("kinesisChina")

        val appNameChina = kinesisConfChina.getString("appName")
        val streamNameChina = kinesisConfChina.getString("streamName")
        val endpointUrlChina = kinesisConfChina.getString("endpointUrl")
        val regionNameChina = kinesisConfChina.getString("regionName")

        // Load credentials
        val awsCredentials = ConfigFactory.load.getConfig("credentials")

        val accessKey = awsCredentials.getString("aws_access_key_id")
        val secretAccessKey = awsCredentials.getString("aws_secret_access_key")

        // Load Chinese credentials
        val awsCredentialsChina = ConfigFactory.load.getConfig("credentialsChina")

        val accessKeyChina = awsCredentialsChina.getString("aws_access_key_id")
        val secretAccessKeyChina = awsCredentialsChina.getString("aws_secret_access_key")
        
        // Create Kinesis Chinese Client
        val credentialsChina = new BasicAWSCredentials(accessKeyChina, secretAccessKeyChina)
        val kinesisClientChina = new AmazonKinesisClient(credentialsChina)
        kinesisClientChina.setEndpoint(endpointUrlChina)

        // Define stream defaults (shards, batchInterval)
        val numShards = kinesisClientChina.describeStream(streamNameChina).getStreamDescription().getShards().size
        val numStreams = numShards
        val batchInterval = Minutes(1)
        val kinesisCheckpointInterval = batchInterval

        val ssc = new StreamingContext(conf, batchInterval)

        // Create Kinesis producer streams
        val kinesisStreamsChina = (0 until numStreams).map { i => 
            KinesisUtils.createStream(
                ssc, 
                appNameChina, 
                streamNameChina, 
                endpointUrlChina, 
                regionNameChina, 
                InitialPositionInStream.LATEST,
                kinesisCheckpointInterval, 
                StorageLevel.MEMORY_AND_DISK_2
                )
        }

        // Union all producer streams
        val unionChinaStreams = ssc.union(kinesisStreamsChina)

        unionChinaStreams.print()


        // Set up kineisHelper for US Kinesis
        val kinesisHelper = KinesisHelper(streamName, regionName, accessKey, secretAccessKey)

        // Process streaming data
        val chineseData = unionChinaStreams
            .map { byteArray => 
                
                val result = kinesisHelper.processRequest(byteArray)

                new String(byteArray)
            }                         
            .map { jsonStr => 
                val json = JSON.parseFull(jsonStr).get.asInstanceOf[Map[String, Any]]
                val (key, values) = json.toList.sortBy(_._1).unzip
                values
            }
            .map { line => Row.fromSeq(line) }
            .foreachRDD { rdd => rdd.collect().foreach(print)}

        
        ssc.remember(Minutes(5))

        ssc.start()
        ssc.awaitTermination()
    }
}