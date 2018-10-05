package com.kobr4.tradebot

import com.typesafe.config.{Config, ConfigFactory}

case class ScheduledTask(name: String, classToCall: String, cronExpression: String, enabled: Boolean)

object ScheduledTask {
  def fromConfig(config: Config) = ScheduledTask(
    config.getString("name"),
    config.getString("class"),
    config.getString("cron-expression"),
    config.getBoolean("enabled")
  )
}

class Configuration(config: Config) {

  object PoloApi {
    val Key = config.getString("polo.api.key")

    val Secret = config.getString("polo.api.secret")
  }

  object KrakenApi {
    val Key = config.getString("kraken.api.key")

    val Secret = config.getString("kraken.api.secret")
  }

  object Scheduled {
    import scala.collection.JavaConverters._
    val tasks: List[ScheduledTask] = config.getConfigList("scheduled-tasks").asScala.toList.map(ScheduledTask.fromConfig)
  }
}

object DefaultConfiguration extends Configuration(ConfigFactory.load())