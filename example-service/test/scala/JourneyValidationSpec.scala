import journeyvalidation._
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import scala.io.Source
import scala.util.Using

class JourneyValidationSpec extends JourneySpec with GuiceOneAppPerSuite {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder().build()

  lazy val journeyJson: String = {
    val stream = getClass.getClassLoader.getResourceAsStream("journey.json")
    Using(Source.fromInputStream(stream))(_.mkString).get
  }

  "Journey structure" should {

    "load and contain at least one page" in {
      journey.pageCount must be > 0
    }

    "have non-empty titles for all pages" in {
      journey.pages.foreach { page =>
        page.title must not be empty
      }
    }

    "have recognised types for all pages" in {
      val knownTypes = Set(
        "contentPage", "string", "datePage", "boolean",
        "radioButton", "checkbox", "multipleQuestionsPage"
      )
      journey.pages.foreach { page =>
        knownTypes must contain(page.pageType)
      }
    }

    "enumerate at least one path" in {
      JourneyGraph.enumerateAllPaths(journey) must not be empty
    }
  }

  "Page validation" should {
    validatePages()
  }

  "Journey path validation" should {
    validatePaths()
  }
}
