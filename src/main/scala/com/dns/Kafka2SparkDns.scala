package com.tj.dns

import com.dns.utils.HBaseUtil
import kafka.serializer.StringDecoder
import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client.Put
import org.apache.hadoop.hbase.util.Bytes

import scala.util.parsing.json.JSON
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.streaming.{Seconds, StreamingContext}

import scala.util.Try

object Kafka2SparkDns {

  def main(args: Array[String]): Unit = {

    // 创建Sparkconf
    val sparkConf = new SparkConf()
      .setAppName("Kafka2SparkDns")
      .setMaster("local[4]")

    // 创建Sparkcontext
    val sc = new SparkContext(sparkConf);
    sc.setLogLevel("WARN");

    // 创建StreamingContext
    val streamContext = new StreamingContext(sc, Seconds(1));
    //    streamContext.checkpoint("hdfs://cdh1:8020/spark/checkpoint/log_dns");
    streamContext.checkpoint("hdfs://localhost:9000/spark/checkpoint/log_dns");


    // 配置kafka参数
    val kafkaParams = Map(
      //      "metadata.broker.list" -> "cdh2:9092",
      "metadata.broker.list" -> "localhost:9092",
      "group.id" -> "dns"
    );
    // 设定topic
    //    val topic = Set(args(0));

    val topic = Set("LOG_DNS");

    // 通过kafka低级api接收数据
    val dstream: InputDStream[(String, String)] =
      KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](streamContext, kafkaParams, topic)

    dstream.foreachRDD(rdd =>{
      rdd.foreachPartition(partitionRecords => {
        val hbaseConnection = HBaseUtil.getHBaseConn("localhost", "2181");
        partitionRecords.foreach(line => {
          val jsonObj =  JSON.parseFull(line._2)
          println(jsonObj)
          val map:Map[String,Any] = jsonObj.get.asInstanceOf[Map[String, Any]]
          val rowkey = map.get("rowKey").get.asInstanceOf[String]
          val message = map.get("message").get.asInstanceOf[String]

          val tableName = TableName.valueOf("LOG_DNS")
          val table = hbaseConnection.getTable(tableName)
          val put = new Put(Bytes.toBytes(rowkey))
          put.addColumn(Bytes.toBytes("fileInfo"), Bytes.toBytes("message"), Bytes.toBytes(message))
          Try(table.put(put)).getOrElse(table.close())//将数据写入HBase，若出错关闭table
          table.close()//分区数据写入HBase后关闭连接
        })
        hbaseConnection.close()

      })
    })

    //    dstream.print()
    streamContext.start();
    streamContext.awaitTermination();

  }

}
