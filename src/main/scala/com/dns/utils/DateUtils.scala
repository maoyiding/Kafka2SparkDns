package com.dns.utils

import java.util.Date

import org.apache.commons.lang3.time.FastDateFormat

object DateUtils {


  val toTime = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss")


  def getTime(time: String) = {
    (time+"000").toLong
  }

  def parse2Time(time : String) = {
    toTime.format(new Date(getTime(time)))
  }

  def main(args: Array[String]): Unit = {
    println(parse2Time("1541570446"))
    val hi =  "(null,[1541570446]".substring(7,17)+"000"
    println(hi)

  }

}
