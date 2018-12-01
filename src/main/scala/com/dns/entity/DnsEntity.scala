package com.dns.entity

/**
  * DNS实体类
  * @param time
  * @param day
  * @param msgLeve
  * @param clientIp
  * @param requestAddress
  * @param ipVersion
  * @param message
  */
case class DnsEntity(time: String, day: String, hour :String ,msgLeve: String, clientIp: String, requestAddress: String, ipVersion: String, message: String)
