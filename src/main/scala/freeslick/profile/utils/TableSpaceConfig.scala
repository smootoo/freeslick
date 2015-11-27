package freeslick.profile.utils

import com.typesafe.config.{ ConfigException, Config }

trait TableSpaceConfig {
  def connectionConfig: Option[Config] = None
  protected lazy val tableTableSpace = try {
    connectionConfig.map(_.getString("tableTableSpace"))
  } catch {
    case _: ConfigException.Missing => None
  }

  protected lazy val indexTableSpace = try {
    connectionConfig.map(_.getString("indexTableSpace"))
  } catch {
    case _: ConfigException.Missing => None
  }

}
