package com.dns

import com.dns.entity.DnsEntity
import com.dns.utils.{DateUtils, ESClient, HBaseUtil, RedisUtils}
import kafka.serializer.StringDecoder
import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client.Put
import org.apache.hadoop.hbase.util.Bytes
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}
import org.json.JSONObject
import org.elasticsearch.common.xcontent.XContentFactory
import org.slf4j.LoggerFactory

import scala.util.Try


/**
  * filebeat清洗数据
  * 没完成，后期解决
  *
  */
object Test {

  def main(args: Array[String]): Unit = {

    // 创建Sparkconf
    val sparkConf = new SparkConf()
      .setAppName("Test")
      .setMaster("local[20]");

    // 创建Sparkcontext
    val sc = new SparkContext(sparkConf);
    sc.setLogLevel("WARN");

    // 创建StreamingContext
    val streamContext = new StreamingContext(sc, Seconds(1));
    streamContext.checkpoint("hdfs://localhost:9000/spark/checkpoint/log_dns");

    // 配置kafka参数
    val kafkaParams = Map(
      "metadata.broker.list" -> "localhost:9092",
      "group.id" -> "dns"
      //      "auto.offset.reset" -> "latest"
    );
    // 设定topic
    val topic = Set("log_dns");


    // 通过kafka低级api接收数据
    val dstream: InputDStream[(String, String)] =
      KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](streamContext, kafkaParams, topic)

    // 获取kafka数据
    dstream.foreachRDD(rdd => {
      val messageInfo = rdd.map(_._2).foreach(
        line => {
          println(line)
          val obj = new JSONObject(line)
          val time = obj.get("@timestamp")
//          println(time)
          val msgType = obj.get("msgType")
          val sources =  obj.get("syslog_hostname")
          val message = obj.get("message").toString
          val hbaseConnection = HBaseUtil.getHBaseConn("localhost", "2181");
          val infos:Array[String] = message.split(" ")
          // println(message)

          try{
            val clientIp =  infos(infos.length-4)
            val requestAddress = infos(infos.length-3)
            println(clientIp)
            println(requestAddress)

            val ipVersion = {
              if (infos(infos.length-2) == "A"){
                "IPV4"
              }else if (infos(infos.length-2) == "AAAA"){
                "IPV6"
              }else{
                infos(infos.length-2)
              }
            }
            println(ipVersion)
            val rowKey = time + "_" + clientIp + "_" + requestAddress + "_" + ipVersion
            val tableName = TableName.valueOf("LOG_DNS");
            val table = hbaseConnection.getTable(tableName);
            val put = new Put(Bytes.toBytes(rowKey))
            put.addColumn(Bytes.toBytes("fileInfo"), Bytes.toBytes("message"), Bytes.toBytes(message));
            //              table.put(put)
            Try(table.put(put)).getOrElse(table.close()) //将数据写入HBase，若出错关闭table
            table.close() //分区数据写入HBase后关闭连接

          }
          catch {
            case e: Exception => "出错了"
          }

          hbaseConnection.close()
        })
    })
//    dstream.foreachRDD(rdd => {
//      val messageInfo = rdd.map(_._2).foreach(
//        line => {
//          //          打印获取到的数据
//          //          println(line)
//          var count = 0
//          // 解析json字符串
//          val obj = new JSONObject(line)
//          //通过key获取value
//          val messages = obj.getString("message")
//
//          val hbaseConnection = HBaseUtil.getHBaseConn("localhost", "2181");
//          val infos = messages.split(" ")
//          try {
//            if (infos(2).substring(0, infos(2).length - 1) == "info") {
//              val time = DateUtils.parse2Time(infos(0).substring(1, infos(0).length - 1))
//              val date = time.split(" ")(0)
//              val hour = time.split(" ")(1).split(":")(0) + "_" + time.split(" ")(1).split(":")(1)
//              val millisecond = infos(0).substring(1, infos(0).length - 1).toLong
//              val msgLeve = infos(2).substring(0, infos(2).length - 1)
//              val clientIp = infos(3)
//              val requestAddress = infos(4).substring(0, infos(4).length - 1)
//              val ipVersion = {
//                if (infos(5) == "A") {
//                  "ipv4"
//                }
//
//                else if ((infos(5) == "AAAA")) {
//                  "ipv6"
//                }
//                else {
//                  infos(5)
//                }
//              }
//              val message = messages
//              count += 1
//
//              // 获取HBase链接
//              val rowKey = infos(0).substring(1, infos(0).length - 1) + "_" + clientIp + "_" + requestAddress + "_" + date + "_" + hour;
//
//              val tableName = TableName.valueOf("LOG_DNS");
//              val table = hbaseConnection.getTable(tableName);
//              val put = new Put(Bytes.toBytes(rowKey))
//
//              put.addColumn(Bytes.toBytes("fileInfo"), Bytes.toBytes("date"), Bytes.toBytes(date));
//              put.addColumn(Bytes.toBytes("fileInfo"), Bytes.toBytes("hour"), Bytes.toBytes(hour));
//              put.addColumn(Bytes.toBytes("fileInfo"), Bytes.toBytes("timestamp"), Bytes.toBytes(millisecond));
//              put.addColumn(Bytes.toBytes("fileInfo"), Bytes.toBytes("clientIp"), Bytes.toBytes(clientIp));
//              put.addColumn(Bytes.toBytes("fileInfo"), Bytes.toBytes("requestAddress"), Bytes.toBytes(requestAddress));
//              put.addColumn(Bytes.toBytes("fileInfo"), Bytes.toBytes("ipVersion"), Bytes.toBytes(ipVersion));
//              put.addColumn(Bytes.toBytes("fileInfo"), Bytes.toBytes("message"), Bytes.toBytes(message));
//
//              //              table.put(put)
//              Try(table.put(put)).getOrElse(table.close()) //将数据写入HBase，若出错关闭table
//              table.close() //分区数据写入HBase后关闭连接
//
//               index2ES(rowKey, date,hour, clientIp, requestAddress , millisecond)
//
//
//              DnsEntity(time, date, hour,millisecond, msgLeve, clientIp, requestAddress, ipVersion, message)
//              var jedis = RedisUtils.getJedis()
//              jedis.select(0)
//              jedis.incrBy("dns:"+ date,count)
//              jedis.incrBy("dns:total",count)
//              jedis.close()
//              //          }
//            }
//          }
//          catch {
//            case e: Exception => "出错了"
//          }
//          hbaseConnection.close()
//        })
//
//    })

//    dstream.print()
    streamContext.start();
    streamContext.awaitTermination();

  }

//   添加数据到es
    def index2ES(rokey: String, date: String,hour : String , clientIp: String, requestAddress: String , millisecond : Long): Unit = {
      val sourceBuilder = XContentFactory.jsonBuilder()
        .startObject()
        .field("rokey", rokey)
        .field("date", date)
        .field("hour",hour)
        .field("timestamp",millisecond)
        .field("clientIp", clientIp)
        .field("requestAddress", requestAddress)
        .endObject()

      ESClient.esClient().prepareIndex("log_dns", "dns", rokey).setSource(sourceBuilder).get();

      ESClient.esClient().close();

    }
}
