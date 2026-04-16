package contract

import play.api.libs.json._
import scala.io.Source
import scala.util.Using

case class ServiceRoute(
    method:     String,
    path:       String,
    pageType:   String,
    title:      String,
    nextRoutes: Map[String, String] = Map.empty
)

object ServiceRoute {
  implicit val reads: Reads[ServiceRoute] = Reads { json =>
    for {
      method     <- (json \ "method").validate[String]
      path       <- (json \ "path").validate[String]
      pageType   <- (json \ "pageType").validate[String]
      title      <- (json \ "title").validate[String]
      nextRoutes <- (json \ "nextRoutes").validateOpt[Map[String, String]]
    } yield ServiceRoute(method, path, pageType, title, nextRoutes.getOrElse(Map.empty))
  }
}

case class ServiceDescriptor(routes: List[ServiceRoute])

object ServiceDescriptor {
  implicit val reads: Reads[ServiceDescriptor] = Json.reads[ServiceDescriptor]

  def parseFile(path: String): Either[String, ServiceDescriptor] =
    Using(Source.fromFile(path))(_.mkString)
      .toEither
      .left.map(_.getMessage)
      .flatMap { content =>
        Json.parse(content).validate[ServiceDescriptor] match {
          case JsSuccess(sd, _) => Right(sd)
          case JsError(errors)  => Left(errors.toString())
        }
      }
}

/** Validates a service descriptor against the journey JSON contract.
  * All checks are derived from the JSON — no hardcoded expectations.
  * When file paths are provided, failures include exact line references.
  */
object ServiceValidator {

  case class FileContext(
      journeyFile: String,
      serviceFile: String
  )

  def validate(journey: Journey, service: ServiceDescriptor, ctx: Option[FileContext] = None): ValidationReport = {
    val routesByTitle = service.routes.groupBy(_.title)

    val results: List[ValidationResult] = journey.pages.toList.zipWithIndex.flatMap {
      case (page, idx) =>
        val label = s"page[$idx] '${page.title}'"
        routesByTitle.get(page.title) match {
          case None =>
            List(Fail(s"$label — route exists", "a matching service route", "no route found",
              contractRef = ctx.flatMap(c => SourceLocator.journeyPageTitle(c.journeyFile, page.title)),
              sourceRef   = ctx.map(c => SourceRef(c.serviceFile, 1))))

          case Some(matchingRoutes) =>
            val route = matchingRoutes.head

            val typeCheck =
              if (route.pageType == page.pageType) Pass(s"$label — type matches")
              else Fail(s"$label — type", page.pageType, route.pageType,
                contractRef = ctx.flatMap(c => SourceLocator.journeyPageType(c.journeyFile, page.pageType, page.title)),
                sourceRef   = ctx.flatMap(c => SourceLocator.serviceRouteType(c.serviceFile, page.title)))

            val titleCheck = Pass(s"$label — title matches")

            val branchingChecks: List[ValidationResult] = page.index match {
              case BranchingIndex(routes) =>
                routes.keys.toList.map { answer =>
                  if (route.nextRoutes.contains(answer))
                    Pass(s"$label — branching route for '$answer' exists")
                  else
                    Fail(s"$label — branching route", s"route for answer '$answer'", "not defined in service",
                      contractRef = ctx.flatMap(c => SourceLocator.journeyPageTitle(c.journeyFile, page.title)),
                      sourceRef   = ctx.flatMap(c => SourceLocator.serviceNextRoutes(c.serviceFile, page.title)))
                }
              case _ => Nil
            }

            typeCheck :: titleCheck :: branchingChecks
        }
    }

    ValidationReport("Service contract validation", results)
  }
}
