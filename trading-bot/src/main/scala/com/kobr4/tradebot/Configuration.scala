package com.kobr4.tradebot

import com.typesafe.config.{ Config, ConfigFactory }

case class ScheduledTaskConfiguration(name: String, classToCall: String, cronExpression: String, enabled: Boolean)

object ScheduledTaskConfiguration {
  def fromConfig(config: Config) = ScheduledTaskConfiguration(
    config.getString("name"),
    config.getString("class"),
    config.getString("cron-expression"),
    config.getBoolean("enabled"))
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
    val tasks: List[ScheduledTaskConfiguration] = config.getConfigList("scheduled-tasks").asScala.toList.map(ScheduledTaskConfiguration.fromConfig)
  }

  object Jwt {
    val Secret = config.getString("jwt.secret")

    val Expiry = config.getDuration("jwt.expiry").toMillis / 1000
  }

  object Mail {
    val Admin = config.getString("mail.admin")

    val Sender = config.getString("mail.sender")

    val Host = config.getString("mail.host")
  }

  object Service {
    val Name = config.getString("service.name")

    val Url = config.getString("service.url")
  }

  object Influx {
    val Host = config.getString("influx.host")

    val Port = config.getInt("influx.port")

    val DB = config.getString("influx.db")
  }
}

object DefaultConfiguration extends Configuration(ConfigFactory.load())