package com.dns.utils

import redis.clients.jedis.{Jedis, JedisPool}

object RedisUtils {

  //在这里搞一个redis的连接池，可以是懒加载的，不用就不加载，可以是私有的，通过方法来得到我的连接对象
  private lazy val Jpool = new JedisPool("127.0.0.1",6379)

  def getJedis():Jedis ={Jpool.getResource}

}
