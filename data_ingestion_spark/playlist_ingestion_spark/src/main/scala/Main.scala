import org.apache.spark.sql.{Encoders, SparkSession}
import org.apache.spark.sql.functions._
import Utilities.flattenStructSchema



object Main {
  def main(args: Array[String]): Unit = {

    val date = java.time.LocalDate.now

    val spark = SparkSession
      .builder()
      .appName("spotify_playlists_ingestion")
      .config("hive.metastore.uris", "thrift://d271ee89-3c06-4d40-b9d6-d3c1d65feb57.priv.instances.scw.cloud:9083")
      .config("spark.sql.warehouse.dir", "/user/hive/warehouse")
      .enableHiveSupport()
      .getOrCreate()

    spark.conf.set("spark.sql.sources.partitionOverwriteMode","dynamic")

    val encoderSchema = Encoders.product[JsonObject].schema
    val df_to_parquet = spark.read.schema(encoderSchema).json("hdfs:///user/groupe5/raw_data/playlists/" + date + ".txt")

    df_to_parquet.select(flattenStructSchema(df_to_parquet.schema):_*)
      .withColumn("partition_date",lit(current_date())).
      write.mode("overwrite").parquet("hdfs:///user/groupe5/parquets_data/playlists/" + date)

    val df_to_hive = spark.read.parquet("hdfs:///user/groupe5/parquets_data/playlists/" + date)

    df_to_hive.write.mode("overwrite")
      .insertInto("iabd1_groupe5.spotify_playlists")
  }
}
