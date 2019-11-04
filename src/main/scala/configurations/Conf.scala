package configurations

import com.typesafe.config.{Config, ConfigFactory}

object Conf {
  val conf: Config = ConfigFactory.load("ATMKeeperDispenserApp_Configurations")
  val confSecretKey: String = conf.getString("conf.dispenserservice.secretKey")
}
