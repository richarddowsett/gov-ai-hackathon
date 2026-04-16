package controllers

import models.Journey
import play.api.libs.json._
import play.api.mvc._
import repositories.JourneyRepository

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JourneyController @Inject()(cc: ControllerComponents, repo: JourneyRepository)(implicit ec: ExecutionContext)
    extends AbstractController(cc) {

  def list = Action.async {
    repo.list().map(journeys => Ok(Json.toJson(journeys)))
  }

  def get(serviceName: String) = Action.async {
    repo.get(serviceName).map {
      case Some(journey) => Ok(Json.toJson(journey))
      case None          => NotFound(Json.obj("error" -> s"Journey for '$serviceName' not found"))
    }
  }

  def create = Action.async(parse.json) { request =>
    request.body.validate[Journey] match {
      case JsSuccess(journey, _) =>
        repo.create(journey).map(j => Created(Json.toJson(j)))
      case JsError(errors) =>
        Future.successful(BadRequest(Json.obj("error" -> JsError.toJson(errors))))
    }
  }

  def update(serviceName: String) = Action.async(parse.json) { request =>
    request.body.validate[Journey] match {
      case JsSuccess(journey, _) =>
        repo.update(serviceName, journey).map {
          case Some(updated) => Ok(Json.toJson(updated))
          case None          => NotFound(Json.obj("error" -> s"Journey for '$serviceName' not found"))
        }
      case JsError(errors) =>
        Future.successful(BadRequest(Json.obj("error" -> JsError.toJson(errors))))
    }
  }

  def delete(serviceName: String) = Action.async {
    repo.delete(serviceName).map {
      case true  => NoContent
      case false => NotFound(Json.obj("error" -> s"Journey for '$serviceName' not found"))
    }
  }
}
