package models

import play.api.libs.json._

case class Journey(serviceName: String, json: String)

object Journey {
  implicit val format: Format[Journey] = Json.format[Journey]
}
