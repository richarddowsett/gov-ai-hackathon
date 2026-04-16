package contract

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

/** Demonstrates drift detection: when the prototype or service diverges
  * from the journey contract, the validator pinpoints exactly what changed.
  *
  * These tests intentionally introduce mismatches to verify that the
  * validator catches them with clear, actionable error messages.
  */
class DriftDetectionSpec extends AnyFreeSpec with Matchers {

  private val journey = JourneyParser.parseFile("example/journey.json").toOption.get

  "Drift detection" - {

    "should catch a title mismatch" in {
      val driftedHtml =
        """<html><body>
          |  <h1>What's your name?</h1>
          |  <input type="text" name="fullName">
          |</body></html>""".stripMargin

      val namePage = journey.pages(1)
      namePage.title shouldBe "What is your name?"

      val results  = PrototypeValidator.validateHtml(driftedHtml, namePage, "page[1]")
      val failures = results.filterNot(_.isPass)

      failures should not be empty
      failures.head.toString should include("Expected 'What is your name?'")
      failures.head.toString should include("What's your name?")
    }

    "should catch a missing form field" in {
      val missingFieldHtml =
        """<html><body>
          |  <h1>What is your name?</h1>
          |</body></html>""".stripMargin

      val namePage = journey.pages(1)
      val results  = PrototypeValidator.validateHtml(missingFieldHtml, namePage, "page[1]")
      val failures = results.filterNot(_.isPass)

      failures should not be empty
      failures.exists(_.toString.contains("text input")) shouldBe true
    }

    "should catch a missing radio option" in {
      val missingOptionHtml =
        """<html><body>
          |  <h1>What type of vehicle do you own?</h1>
          |  <input type="radio" name="vehicle" value="Car"><label>Car</label>
          |  <input type="radio" name="vehicle" value="Motorcycle"><label>Motorcycle</label>
          |  <input type="radio" name="vehicle" value="Truck"><label>Truck</label>
          |</body></html>""".stripMargin

      val vehiclePage = journey.pages(4)
      val results     = PrototypeValidator.validateHtml(missingOptionHtml, vehiclePage, "page[4]")
      val failures    = results.filterNot(_.isPass)

      failures should not be empty
      failures.exists(_.toString.contains("Other")) shouldBe true
    }

    "should catch a missing service route" in {
      val incompleteService = ServiceDescriptor(routes = List(
        ServiceRoute("GET", "/welcome", "contentPage", "Welcome to our survey")
      ))

      val report = ServiceValidator.validate(journey, incompleteService)
      report.allPassed shouldBe false
      report.failures.size should be >= 1
      report.failures.exists(_.reason.contains("No service route found")) shouldBe true
    }

    "should catch a page type mismatch in service" in {
      val wrongTypeService = ServiceDescriptor(routes = journey.pages.toList.map { page =>
        val wrongType = if (page.pageType == "string") "contentPage" else page.pageType
        ServiceRoute("GET", s"/${page.title.take(10)}", wrongType, page.title,
          page.index match {
            case BranchingIndex(routes) => routes.map { case (k, v) => k -> s"/page-$v" }
            case _ => Map.empty
          })
      })

      val report = ServiceValidator.validate(journey, wrongTypeService)
      report.allPassed shouldBe false
      report.failures.exists(_.reason.contains("Expected type 'string'")) shouldBe true
    }

    "should pass when everything matches" in {
      val goodHtml =
        """<html><body>
          |  <h1>Welcome to our survey</h1>
          |</body></html>""".stripMargin

      val results = PrototypeValidator.validateHtml(goodHtml, journey.pages(0), "page[0]")
      results.forall(_.isPass) shouldBe true
    }
  }
}
