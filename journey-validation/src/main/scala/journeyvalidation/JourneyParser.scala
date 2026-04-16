package journeyvalidation

import play.api.libs.json._
import scala.io.Source
import scala.util.{Try, Using}

object JourneyParser {

  private implicit val pageIndexReads: Reads[PageIndex] = Reads { json =>
    json.validate[Int].map(LinearIndex.apply) orElse
      json.validate[Map[String, Int]].map(BranchingIndex.apply)
  }

  private implicit val pageReads: Reads[Page] = Reads { json =>
    for {
      pageType   <- (json \ "type").validate[String]
      title      <- (json \ "title").validate[String]
      index      <- (json \ "index").validate[PageIndex]
      options    <- (json \ "options").validateOpt[List[String]]
      questions  <- (json \ "questions").validateOpt[List[Question]]
      validation <- (json \ "validation").validateOpt[List[String]]
                      .orElse((json \ "validation").validateOpt[String].map(_.map(List(_))))
    } yield Page(
      pageType   = pageType,
      title      = title,
      index      = index,
      options    = options.getOrElse(Nil),
      questions  = questions.getOrElse(Nil),
      validation = validation
    )
  }

  private implicit val journeyReads: Reads[Journey] = Reads { json =>
    (json \ "pages").validate[Vector[Page]].map(Journey.apply)
  }

  def parse(jsonString: String): Either[String, Journey] =
    Try(Json.parse(jsonString)).toEither
      .left.map(e => s"Invalid JSON: ${e.getMessage}")
      .flatMap { jsValue =>
        jsValue.validate[Journey] match {
          case JsSuccess(journey, _) => Right(journey)
          case JsError(errors) =>
            Left(errors.flatMap { case (path, validationErrors) =>
              validationErrors.map(e => s"$path: ${e.message}")
            }.mkString("; "))
        }
      }

  def parseFile(path: String): Either[String, Journey] =
    Using(Source.fromFile(path))(_.mkString)
      .toEither
      .left.map(e => s"Could not read file $path: ${e.getMessage}")
      .flatMap(parse)

  def parseResource(resourcePath: String): Either[String, Journey] = {
    val stream = Option(getClass.getResourceAsStream(resourcePath))
      .orElse(Option(getClass.getClassLoader.getResourceAsStream(resourcePath)))
      .toRight(s"Resource not found: $resourcePath")
    stream
      .map(is => Using(Source.fromInputStream(is))(_.mkString).get)
      .flatMap(parse)
  }
}
