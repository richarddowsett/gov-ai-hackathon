package journeyvalidation

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import play.api.libs.json._
import scala.util.Try

object JourneyStorageClient {

  private val client = HttpClient.newHttpClient()

  /**
   * Fetches journey JSON from the journey-storage API.
   *
   * @param storageUrl  base URL of the storage API, e.g. "http://localhost:9000"
   * @param serviceName the service name key stored in the database
   * @return Right(journeyJsonString) on success, Left(errorMessage) on failure
   */
  def fetch(storageUrl: String, serviceName: String): Either[String, String] =
    Try {
      val request = HttpRequest.newBuilder()
        .uri(URI.create(s"${storageUrl.stripSuffix("/")}/journeys/$serviceName"))
        .GET()
        .build()

      val response = client.send(request, HttpResponse.BodyHandlers.ofString())

      if (response.statusCode() == 200) {
        val body = Json.parse(response.body())
        (body \ "json").asOpt[String] match {
          case Some(json) => Right(json)
          case None       => Left(s"Response missing 'json' field: ${response.body().take(200)}")
        }
      } else {
        Left(s"Storage API returned ${response.statusCode()}: ${response.body().take(200)}")
      }
    }.recover {
      case e: java.net.ConnectException =>
        Left(s"Cannot connect to storage API at $storageUrl: ${e.getMessage}")
      case e: Exception =>
        Left(s"Error fetching from storage API: ${e.getMessage}")
    }.get
}
