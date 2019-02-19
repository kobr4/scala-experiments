package com.nicolasmy

import com.typesafe.config.{Config, ConfigFactory}

class Configuration(config: Config) {

  object Influx {
    val Host = config.getString("influx.host")

    val Port = config.getInt("influx.port")

    val DB = config.getString("influx.db")
  }

}

object DefaultConfiguration extends Configuration(ConfigFactory.load())