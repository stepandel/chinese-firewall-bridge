This is Spark data streaming application that serves as a bridge over Chinese Great Firewall

## Architecture

    Lambda(China) -> Kinesis(China) -> Spark Cluster(US) -> Kinesis(US) -> Firehose(US) -> S3(US)
        |                                     |
       \/                                    \/
    Firehose(China)                  **Spark Application**
        |                                     |
        \/                                    \/
    S3 Backup(China)                       Spark UI



    Lambda(China) - kk-analytics-firehose-prod-webhook
    Kinesis(China) - prod-kinesis-data-streaming
    Firehose(China) - prod-analytics-backup-firehose
    S3 Backup(China) - prodanalytics-firehose-bucket/backup
    Kinesis(US) - prod-china-kinesis-data-streaming
    Firehose(US) - prod-china-analytics-firehose-parquet
    S3(US) - prodanalytics-firehose-bucket/parquet

## Set up

_Local Machine (MAC)_

- Install Java: `brew cask install java`
- Install Scala: `brew install scala`
  https://www.scala-lang.org/
- Install SBT: `brew install sbt`
  https://www.scala-sbt.org/
- Install Spark: `brew install apache-spark`
  https://spark.apache.org/
- Install Flintrock: `brew install flintrock`
  https://github.com/nchammas/flintrock

_Remote Machine (Linux)_

- Install Java: `sudo yum install java-1.8.0-openjdk-devel`
- Follow [this guide](https://linuxconfig.org/how-to-install-spark-on-redhat-8) to install spark

## First Time Deployment

1. Build fat jar: `sbt assembly`
2. Configure cluster: `flintrock configure`
3. Launch cluster: `flintrock launch prod-spark-cluster`
4. (If not enabled) Enable inbound port 7077, 8080 & 22 for cluster securty gorup in your [AWS](https://console.aws.amazon.com/ec2/home?region=us-east-1#SecurityGroups:sort=groupId)
5. Check master node public DNS by running `flintrock describe`
6. Check if cluster is running by going to http://master_public_dns:8080
7. Copy application jar to the cluster nodes (update local file path):
   `flintrock copy-file prod-spark-cluster \
/Users/stepanarsentjev/Development/chinaDataStreaming/target/scala-2.11/china-data-streaming.jar \
/home/ec2-user/
`
8. ssh to master and all worker instances and set default aws profile with Chinese credentials (can be found in application.conf):
   `aws configure` (set region to cn-northwest-1)
9. Copy jar file to `/home/ec2-user/` of a separate instance (ec2-54-196-74-76.compute-1.amazonaws.com) or if doesn't exist create a new one and install all spark dependencies:
   `scp -i "pem-file.pem" /file/path ec2-user@machine-dns:/remote/path/to/file`
10. ssh to the above instance
11. Deploy Spark app by running the following from ~ : (replace master url with the one found in the web ui)
    `/opt/spark/bin/spark-submit  --deploy-mode cluster --master spark://ec2-3-91-11-48.compute-1.amazonaws.com:7077 --driver-memory 10g /home/ec2-user/china-data-stream.jar`

## Maintanace

##### To make changes to the application running in production:

- Restart cluster (all logs will be removed - so save what you need before restarting)
  `flintrock stop prod-spark-cluster` and `flintrock start prod-spark-cluster`
- Upload updated jar file (china-data-stream.jar)
  ```flintrock copy-file prod-spark-cluster \
  /Users/stepanarsentjev/Development/chinaDataStreaming/target/scala-2.11/china-data-streaming.jar /home/ec2-user/
  ```
- Submit spark job to cluster from external instance (master url will be different!)
  `/opt/spark/bin/spark-submit  --deploy-mode cluster --master spark://ec2-54-234-92-94.compute-1.amazonaws.com:7077 --driver-memory 10g --executor-memory 10g /home/ec2-user/china-data-streaming.jar`

INFO: Records on Kinesis Data Stream are stored for 24 hour (or until read)
To avoid data leaks, redeploy spark cluster within 24 hours

## Debuging

- To increase spark deploy response timeOut; set spark.rpc.askTimeout & spark.network.timeput to 800 in `/opt/spark-2.4.3-bin-hadoop2.6/conf/spark-defaults.conf`
