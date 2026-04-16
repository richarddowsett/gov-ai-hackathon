package controllers

import models.Journey
import play.api.libs.json._
import play.api.mvc._

import java.util.concurrent.ConcurrentHashMap
import javax.inject._
import scala.jdk.CollectionConverters._

@Singleton
class JourneyController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  private val store = new ConcurrentHashMap[String, Journey]()

  def list = Action {
    Ok(Json.toJson(store.values().asScala.toList))
  }

  def get(serviceName: String) = Action {
    Option(store.get(serviceName)) match {
      case Some(journey) => Ok(Json.toJson(journey))
      case None          => NotFound(Json.obj("error" -> s"Journey for '$serviceName' not found"))
    }
  }

  def create = Action(parse.json) { request =>
    request.body.validate[Journey] match {
      case JsSuccess(journey, _) =>
        store.put(journey.serviceName, journey)
        Created(Json.toJson(journey))
      case JsError(errors) =>
        BadRequest(Json.obj("error" -> JsError.toJson(errors)))
    }
  }

  def update(serviceName: String) = Action(parse.json) { request =>
    request.body.validate[Journey] match {
      case JsSuccess(journey, _) =>
        val existed = store.containsKey(serviceName)
        val updated = journey.copy(serviceName = serviceName)
        store.put(serviceName, updated)
        if (existed) Ok(Json.toJson(updated)) else Created(Json.toJson(updated))
      case JsError(errors) =>
        BadRequest(Json.obj("error" -> JsError.toJson(errors)))
    }
  }

  def delete(serviceName: String) = Action {
    if (store.remove(serviceName) != null) NoContent
    else NotFound(Json.obj("error" -> s"Journey for '$serviceName' not found"))
  }
}
