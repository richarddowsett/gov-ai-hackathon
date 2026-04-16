package journeyvalidation

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._

/**
 * Mix this trait into a test spec alongside [[GuiceOneAppPerSuite]] to
 * automatically validate a Play service against a journey.json definition.
 *
 * Usage:
 * {{{
 * class MyJourneySpec extends JourneySpec with GuiceOneAppPerSuite {
 *   lazy val journeyJson = Source.fromResource("journey.json").mkString
 *   override def fakeApplication() = new GuiceApplicationBuilder().build()
 *
 *   "Page validation" should {
 *     validatePages()
 *   }
 *   "Journey paths" should {
 *     validatePaths()
 *   }
 * }
 * }}}
 */
trait JourneySpec extends PlaySpec { self: BaseOneAppPerSuite =>

  /** Raw JSON content of the journey definition — must be available at test registration time. */
  def journeyJson: String

  /** URL path pattern for pages. {index} is replaced with the page array index. */
  def pageUrlPattern: String = "/page/{index}"

  protected lazy val journey: Journey = JourneyParser.parse(journeyJson) match {
    case Right(j)  => j
    case Left(err) => fail(s"Failed to parse journey JSON: $err")
  }

  protected def pageUrl(index: Int): String =
    pageUrlPattern.replace("{index}", index.toString)

  /** Registers one test per page that GETs the page and validates its HTML. */
  def validatePages(): Unit = {
    journey.pages.toList.zipWithIndex.foreach { case (page, idx) =>
      s"page[$idx] (${page.pageType}) '${page.title}' should render correctly" in {
        val result = route(app, FakeRequest(GET, pageUrl(idx))).get
        status(result) mustBe OK

        val html    = contentAsString(result)
        val results = PageValidator.validateHtml(html, page, s"page[$idx]")
        val report  = ValidationReport(s"page[$idx]", results)
        withClue(s"\n${report.render}") {
          report.allPassed mustBe true
        }
      }
    }
  }

  /** Registers one test per unique path that walks the journey end-to-end,
   *  validating every page and verifying navigation redirects.
   */
  def validatePaths(): Unit = {
    JourneyGraph.uniquePaths(journey).zipWithIndex.foreach { case (path, pathIdx) =>
      s"path ${pathIdx + 1}: ${path.description}" in {
        path.steps.zipWithIndex.foreach { case (step, stepIdx) =>
          val getResult = route(app, FakeRequest(GET, pageUrl(step.arrayIndex))).get
          status(getResult) mustBe OK

          val html    = contentAsString(getResult)
          val results = PageValidator.validateHtml(html, step.page, s"page[${step.arrayIndex}]")
          withClue(s"Validation failed for page[${step.arrayIndex}] '${step.page.title}':\n") {
            results.forall(_.isPass) mustBe true
          }

          if (stepIdx < path.steps.size - 1) {
            val formData   = buildFormData(step.page, step.selectedAnswer)
            val postResult = route(app, FakeRequest(POST, pageUrl(step.arrayIndex))
              .withFormUrlEncodedBody(formData: _*)).get
            status(postResult) mustBe SEE_OTHER

            val nextStep = path.steps(stepIdx + 1)
            redirectLocation(postResult) mustBe Some(pageUrl(nextStep.arrayIndex))
          }
        }
      }
    }
  }

  /** Builds form data appropriate for the given page type and chosen answer. */
  protected def buildFormData(page: Page, answer: Option[String]): Seq[(String, String)] =
    page.pageType match {
      case "contentPage"          => Seq.empty
      case "string"               => Seq("value" -> "Test Input")
      case "datePage"             => Seq("day" -> "01", "month" -> "01", "year" -> "2000")
      case "boolean"              => Seq("value" -> answer.getOrElse("true"))
      case "radioButton"          => Seq("value" -> answer.getOrElse(page.options.headOption.getOrElse("")))
      case "checkbox"             => page.options.map("value[]" -> _)
      case "multipleQuestionsPage" =>
        page.questions.indices.map(i => s"question_$i" -> "3")
      case _ => Seq.empty
    }
}
