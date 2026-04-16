package contract

import play.api.libs.json._
import scala.io.Source
import scala.util.Using

/** Represents a single route in a service's routing table. */
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

/** Represents a service's self-describing routing metadata.
  *
  * In a real Play Framework service, this would be extracted from conf/routes
  * and controller annotations. For the hackathon, services expose a routes.json
  * that describes their implemented pages.
  */
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
  *
  * Checks that:
  *   - every page in the journey has a corresponding route in the service
  *   - page titles match between the contract and the service
  *   - page types are consistent
  *   - branching routes exist for all options defined in the contract
  */
object ServiceValidator {

  def validate(journey: Journey, service: ServiceDescriptor): ValidationReport = {
    val routesByTitle = service.routes.groupBy(_.title)

    val results: List[ValidationResult] = journey.pages.toList.zipWithIndex.flatMap {
      case (page, idx) =>
        val slug = s"page[$idx]"
        routesByTitle.get(page.title) match {
          case None =>
            List(Fail(slug, s"No service route found for page '${page.title}'"))

          case Some(matchingRoutes) =>
            val route = matchingRoutes.head
            val typeCheck =
              if (route.pageType == page.pageType) Pass(s"$slug type")
              else Fail(s"$slug type", s"Expected type '${page.pageType}', service has '${route.pageType}'")

            val titleCheck = Pass(s"$slug title")

            val branchingChecks: List[ValidationResult] = page.index match {
              case BranchingIndex(routes) =>
                val missingRoutes = routes.keys.toList.filterNot(route.nextRoutes.contains)
                if (missingRoutes.isEmpty) List(Pass(s"$slug branching"))
                else List(Fail(s"$slug branching", s"Service missing routes for answers: ${missingRoutes.mkString(", ")}"))
              case _ => Nil
            }

            typeCheck :: titleCheck :: branchingChecks
        }
    }

    ValidationReport(pathDescription = "Service contract validation", results = results)
  }
}
