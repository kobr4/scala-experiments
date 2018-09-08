package com.nicolasmy

import com.typesafe.config.{Config, ConfigFactory}

class Configuration(config: Config) {

  val RpcUrl = config.getString("rpc.url")
  val RpcUser = config.getString("rpc.user")
  val RpcPassword = config.getString("rpc.password")
}


object DefaultConfiguration extends Configuration(ConfigFactory.load())