import java.io.{BufferedWriter, File, FileWriter, PrintWriter}
import java.nio.file.{Files, Path, Paths}

import com.databricks.spark.avro.SchemaConverters
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import org.apache.avro.generic.GenericRecord
import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.sql.functions.from_unixtime
import org.apache.spark.sql.types.StructType
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka010.ConsumerStrategies._
import org.apache.spark.streaming.kafka010.LocationStrategies._
import org.apache.spark.streaming.kafka010._
import org.apache.commons.configuration.{ConfigurationFactory, PropertiesConfiguration}




object main {

  val configFile = new File("../application.properties")
  def main(args: Array[String]): Unit = {
    if (!Files.exists(configFile.toPath)) {
      val bw = new PrintWriter(new BufferedWriter(new FileWriter(configFile)))
      bw.println("kafka.brokers = localhost:9092")
      bw.println("schemaRegistry.url = http://localhost:8081")
      bw.println("schemaRegistry.subject = example.avro.tweet")
      bw.println("kafka.topics = tweets")
      bw.println("twitter.searchTerms = #gameofthrones")
      bw.println("twitter.oauth.consumerKey = YOUR_TWITTER_CONSUMER_KEY")
      bw.println("twitter.oauth.consumerSecret = YOUR_TWITTER_CONSUMER_KEY")
      bw.println("twitter.oauth.accessToken = YOUR_TWITTER_ACCESS_TOKEN")
      bw.println("twitter.oauth.accessTokenSecret = YOUR_TWITTER_ACCESS_TOKEN_SECRET")
      bw.println("spark.output = /twitter/tweets.parquet")
      bw.close()
      Console.err.println("Missing ../application.properties. A default ../application.properties file was written.")
      System.exit(1)
    }

    val conf = new PropertiesConfiguration(configFile.getPath)

    new produce().run(conf)
  }
}
