package contract

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, OptionValues}
import org.jsoup.Jsoup
import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}

/** Validates the prototype HTML pages against the journey JSON contract.
  *
  * This spec starts an embedded HTTP server that serves the prototype pages,
  * then walks every journey path and validates that each page's HTML conforms
  * to the contract: correct title, correct form elements, correct page type.
  *
  * This is the "Option A" browser-style validation — using HTTP + HTML parsing
  * to simulate what a browser-based test would check, but without requiring
  * an actual browser runtime, making it fast and CI-friendly.
  */
class PrototypeContractSpec extends AnyFreeSpec with Matchers with BeforeAndAfterAll with OptionValues {

  private val journey  = JourneyParser.parseFile("example/journey.json").toOption.get
  private val server   = new EmbeddedServer("prototype")
  private val client   = HttpClient.newHttpClient()
  private var baseUrl  = ""

  override def beforeAll(): Unit = {
    server.start()
    baseUrl = server.baseUrl
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    server.stop()
    super.afterAll()
  }

  private def fetchPage(pageIndex: Int): String = {
    val request  = HttpRequest.newBuilder(URI.create(s"$baseUrl/page/$pageIndex")).build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    response.statusCode() shouldBe 200
    response.body()
  }

  // Validate every page individually
  "Prototype page-level contract validation" - {
    journey.pages.toList.zipWithIndex.foreach { case (page, idx) =>
      s"page[$idx] '${page.title}' (${page.pageType})" - {

        "should have the correct title in the <h1> element" in {
          val html   = fetchPage(idx)
          val doc    = Jsoup.parse(html)
          val h1     = doc.select("h1")
          h1 should not be empty
          h1.first().text().trim shouldBe page.title
        }

        "should contain the correct form elements for its page type" in {
          val html    = fetchPage(idx)
          val results = PrototypeValidator.validateHtml(html, page, s"page[$idx]")
          val failures = results.filterNot(_.isPass)
          withClue(failures.mkString("\n")) {
            failures shouldBe empty
          }
        }
      }
    }
  }

  // Validate complete journey paths end to end
  "Prototype journey path validation" - {
    val uniquePaths = JourneyGraph.uniquePaths(journey)

    uniquePaths.zipWithIndex.foreach { case (path, pathIdx) =>
      s"Journey path ${pathIdx + 1}: ${path.description}" - {

        "should be able to visit every page in sequence" in {
          path.steps.foreach { step =>
            val html = fetchPage(step.arrayIndex)
            val doc  = Jsoup.parse(html)
            doc.select("h1").first().text().trim shouldBe step.page.title
          }
        }

        "should pass full contract validation for every page" in {
          val report = validatePath(path)
          withClue(report.summary) {
            report.allPassed shouldBe true
          }
        }
      }
    }
  }

  private def validatePath(path: JourneyPath): ValidationReport = {
    val results = path.steps.flatMap { step =>
      val html = fetchPage(step.arrayIndex)
      PrototypeValidator.validateHtml(html, step.page, s"page[${step.arrayIndex}]")
    }
    ValidationReport(path.description, results)
  }
}
