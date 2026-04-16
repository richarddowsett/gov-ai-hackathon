package controllers

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.BrowserViewService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}

@Singleton
class BrowserPlayController @Inject()(
    override val controllerComponents: MessagesControllerComponents,
    browserViewService: BrowserViewService
) extends FrontendController(controllerComponents) {

  def home: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.home())
  }

  def prototype(page: String): Action[AnyContent] = Action { implicit request =>
    browserViewService.loadPrototypePage(page) match {
      case Some(proto) => Ok(views.html.prototype(page, proto.title, proto.fields, proto.transitions))
      case None        => NotFound(views.html.home(Some(s"Prototype page not found: $page")))
    }
  }

  def service: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.service(browserViewService.loadServicePages()))
  }
}
