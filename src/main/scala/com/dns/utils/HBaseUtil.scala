package com.dns.utils

import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.client.{Connection, ConnectionFactory}

object HBaseUtil extends Serializable {
  /**
    * @param zkList Zookeeper列表已逗号隔开
    * @param port ZK端口号
    * @return
    */
  def getHBaseConn(zkList: String, port: String): Connection = {
    val conf = HBaseConfiguration.create()
    conf.set("hbase.zookeeper.quorum", zkList)
    conf.set("hbase.zookeeper.property.clientPort", port)
    val connection = ConnectionFactory.createConnection(conf)
    connection
  }
}