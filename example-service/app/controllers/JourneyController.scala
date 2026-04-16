package controllers

import javax.inject._
import play.api.mvc._
import play.api.{Configuration, Environment, Logging}
import journeyvalidation._
import scala.io.Source
import scala.util.Using

@Singleton
class JourneyController @Inject()(
    cc:            ControllerComponents,
    configuration: Configuration,
    environment:   Environment
) extends AbstractController(cc) with Logging {

  private val journey: Journey = {
    val storageUrl  = configuration.get[String]("journey.storage.url")
    val serviceName = configuration.get[String]("journey.serviceName")

    val json = JourneyStorageClient.fetch(storageUrl, serviceName) match {
      case Right(j) =>
        logger.info(s"Loaded journey for '$serviceName' from storage API at $storageUrl")
        j
      case Left(err) =>
        logger.warn(s"Could not fetch from storage API ($err). Falling back to classpath journey.json")
        val stream = environment.resourceAsStream("journey.json")
          .getOrElse(throw new RuntimeException("journey.json not found on classpath"))
        Using(Source.fromInputStream(stream))(_.mkString).get
    }

    JourneyParser.parse(json) match {
      case Right(j)  => j
      case Left(err) => throw new RuntimeException(s"Failed to parse journey JSON: $err")
    }
  }

  def start: Action[AnyContent] = Action {
    Redirect(routes.JourneyController.showPage(0))
  }

  def showPage(index: Int): Action[AnyContent] = Action { implicit request =>
    if (index < 0 || index >= journey.pageCount)
      Redirect(routes.JourneyController.complete)
    else
      Ok(views.html.page(journey.pages(index), index))
  }

  def submitPage(index: Int): Action[AnyContent] = Action { implicit request =>
    if (index < 0 || index >= journey.pageCount) {
      Redirect(routes.JourneyController.complete)
    } else {
      val page     = journey.pages(index)
      val formData = request.body.asFormUrlEncoded.getOrElse(Map.empty)

      val nextIndex = page.index match {
        case LinearIndex(next) => next
        case BranchingIndex(branchRoutes) =>
          val answer = formData.get("value").flatMap(_.headOption).getOrElse("")
          branchRoutes.getOrElse(answer, index + 1)
      }

      Redirect(routes.JourneyController.showPage(nextIndex))
    }
  }

  def complete: Action[AnyContent] = Action {
    Ok(views.html.complete())
  }
}
