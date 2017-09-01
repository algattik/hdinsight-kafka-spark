import com.databricks.spark.avro.SchemaConverters
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import org.apache.avro.generic.GenericRecord
import org.apache.commons.configuration.Configuration
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrameWriter, Row, SaveMode, SparkSession}
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka010.ConsumerStrategies._
import org.apache.spark.streaming.kafka010.LocationStrategies._
import org.apache.spark.streaming.kafka010._

class consume {

  def run(conf: Configuration) = {

    val spark = SparkSession.builder()
      .appName("kafka.avro.consumer")
      .getOrCreate()

    val schemaRegistry = new CachedSchemaRegistryClient(conf.getString("schemaRegistry.url"), 1000)
    val m = schemaRegistry.getLatestSchemaMetadata(conf.getString("schemaRegistry.subject"))
    val schemaId = m.getId
    val schema = schemaRegistry.getById(schemaId)

    // Kafka configuration
    // The Kafka topic(s) to read from
    val topics = Array(conf.getString("kafka.topics"))
    // Batching interval when reading
    val batchInterval = 2

    // A function that creates a streaming context
    def createStreamingContext(): StreamingContext = {

      // Create a new StreamingContext from the default context.
      val ssc = new StreamingContext(spark.sparkContext, Seconds(batchInterval))

      // Kafka parameters when reading
      // auto.offset.reset = 'earliest' reads from the beginning of the queue
      //     Set to 'latest' to only receive new messages as they are added to the queue.
      val kafkaParams = Map[String, Object](
        "bootstrap.servers" -> conf.getString("kafka.brokers"),
        "key.deserializer" -> classOf[KafkaAvroDeserializer],
        "value.deserializer" -> classOf[KafkaAvroDeserializer],
        "group.id" -> "test1",
        "auto.offset.reset" -> "earliest",
        "enable.auto.commit" -> (false: java.lang.Boolean),
        "schema.registry.url" -> conf.getString("schemaRegistry.url")
      )

      // Create the stream from Kafka
      val messageStream = KafkaUtils.createDirectStream(
        ssc,
        PreferConsistent,
        Subscribe[String, GenericRecord](topics, kafkaParams)
      )

      // Get only the tweets (in JSON format)
      val tweetsJSON = messageStream.map(record => record.value)

      // Convert the records to JSON dataframes, so we can select interesting values
      tweetsJSON.foreachRDD {
        rdd =>
          // because sometimes there's not really an RDD there
          if (rdd.count() >= 1) {
            // Parse the JSON and infer a schema
            val tweetObj = rdd.map(
              v => {
                Row.fromSeq(List[Any](
                  v.get("id"),
                  v.get("createdAt"),
                  v.get("lang").toString, // Type is org.apache.avro.util.Utf8
                  v.get("retweetCount"),
                  v.get("text").toString,
                  v.get("location").toString
                ))
              })
            val schemaStructType = SchemaConverters.toSqlType(schema).dataType.asInstanceOf[StructType]
            val tweetRaw = spark.createDataFrame(tweetObj, schemaStructType)
            val tweetInfo = tweetRaw
              .withColumn("createdAt", from_unixtime(col("createdAt").divide(1000)))
              .withColumn("year", year(col("createdAt")))
              .withColumn("month", month(col("createdAt")))
              .withColumn("day", dayofmonth(col("createdAt")))

            // Show 5 in the console
            tweetInfo.show(5)
            // Append to Parquet
            tweetInfo
              .write
              .partitionBy("year", "month", "day")
              .mode(SaveMode.Append)
              .save(conf.getString("spark.output"))
          }
      }


      // Tell the stream to keep the data around for a minute, so it's there when we query later
      ssc.remember(Minutes(1))
      // Checkpoint for fault-tolerance
      // ssc.checkpoint("/tweetcheckpoint")
      // Return the StreamingContext
      ssc
    }

    // Stop any existing StreamingContext
    val stopActiveContext = true
    if (stopActiveContext) {
      StreamingContext.getActive.foreach {
        _.stop(stopSparkContext = false)
      }
    }

    // Get or create a StreamingContext
    val ssc = StreamingContext.getActiveOrCreate(createStreamingContext)

    // This starts the StreamingContext in the background.
    ssc.start()

    // Set the stream to run with a timeout of batchInterval * 60 * 1000 seconds
    ssc.awaitTerminationOrTimeout(batchInterval * 60 * 700 /* * 1000 */)
  }


}
