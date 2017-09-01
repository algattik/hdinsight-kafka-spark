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
      Console.err.println("Missing ../application.properties.")
      System.exit(1)
    }

    val conf = new PropertiesConfiguration(configFile.getPath)
    new consume().run(conf)
  }
}
