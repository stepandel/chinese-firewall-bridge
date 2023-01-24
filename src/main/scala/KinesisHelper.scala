import java.lang

import com.amazonaws.services.kinesis.AmazonKinesisClient
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder
import com.amazonaws.services.kinesis.model.PutRecordRequest
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials, AWSCredentials}
import java.lang.System.currentTimeMillis
import java.nio.ByteBuffer

class KinesisHelper(
    stream: String,
    region: String,
    awsAccessKey: String,
    awsSecretKey: String) extends Serializable {

    @transient lazy val kinesisClient = AmazonKinesisClientBuilder.standard()
            .withRegion(region)
            .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(awsAccessKey, awsSecretKey)))
            .build()

    def processRequest(byteArray: Array[Byte]) {
        val time = System.currentTimeMillis()
        val request = new PutRecordRequest()
            .withStreamName(stream)
            .withPartitionKey(s"${time}")
            .withData(ByteBuffer.wrap(byteArray))
        val result = kinesisClient.putRecord(request)
    }

}

object KinesisHelper {
    def apply(stream: String, region: String, awsAccessKey: String, awsSecretKey: String): KinesisHelper = new KinesisHelper(stream, region, awsAccessKey, awsSecretKey)
}