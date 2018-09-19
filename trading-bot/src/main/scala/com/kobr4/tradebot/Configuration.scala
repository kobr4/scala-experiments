package com.kobr4.tradebot

import com.typesafe.config.{ Config, ConfigFactory }

class Configuration(config: Config) {

  object PoloApi {
    val Key = config.getString("polo.api.key")

    val Secret = config.getString("polo.api.secret")
  }

}

object DefaultConfiguration extends Configuration(ConfigFactory.load())