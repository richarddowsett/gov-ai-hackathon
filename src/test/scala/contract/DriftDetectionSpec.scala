package contract

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

/** Demonstrates drift detection using the journey JSON as the source of truth.
  *
  * Finds pages by type from the JSON, then constructs drifted HTML to verify
  * that the validator catches each kind of mismatch. No page indices or titles
  * are hardcoded — everything is discovered from the journey.
  */
class DriftDetectionSpec extends AnyFreeSpec with Matchers {

  private val journey = JourneyParser.parseFile(ValidatorConfig.journeyPath).toOption.get

  private def findPageByType(pageType: String): (Page, Int) = {
    val idx = journey.pages.indexWhere(_.pageType == pageType)
    require(idx >= 0, s"Journey has no page of type '$pageType'")
    (journey.pages(idx), idx)
  }

  private def logResults(results: List[ValidationResult]): Unit =
    results.foreach {
      case Pass(check) => info(s"  ✅  $check")
      case f: Fail =>
        val base = s"  ❌  ${f.check} — expected: '${f.expected}', actual: '${f.actual}'"
        val refs = List(
          f.contractRef.map(r => s"contract: $r"),
          f.sourceRef.map(r => s"fix at: $r")
        ).flatten
        val suffix = if (refs.nonEmpty) s"\n        ${refs.mkString("  |  ")}" else ""
        info(base + suffix)
    }

  "Drift detection" - {

    "title mismatch: changing a page title should be caught" in {
      val (page, idx) = findPageByType("string")
      val driftedTitle = page.title + " (modified)"

      val html = s"""<html><body><h1>$driftedTitle</h1><input type="text"></body></html>"""

      val results  = PrototypeValidator.validateHtml(html, page, s"page[$idx]")
      val failures = results.filterNot(_.isPass)

      info(s"  Original title : '${page.title}'")
      info(s"  Drifted title  : '$driftedTitle'")
      logResults(failures)

      failures should not be empty
    }

    "missing field: removing a required input should be caught" in {
      val (page, idx) = findPageByType("string")

      val html = s"""<html><body><h1>${page.title}</h1></body></html>"""

      val results  = PrototypeValidator.validateHtml(html, page, s"page[$idx]")
      val failures = results.filterNot(_.isPass)

      logResults(failures)

      failures should not be empty
    }

    "missing option: removing a radio option should be caught" in {
      val (page, idx) = findPageByType("radioButton")
      val keptOptions   = page.options.init
      val droppedOption = page.options.last

      val radiosHtml = keptOptions.map { opt =>
        s"""<input type="radio" name="choice" value="$opt"><label>$opt</label>"""
      }.mkString("\n")

      val html = s"""<html><body><h1>${page.title}</h1>$radiosHtml</body></html>"""

      val results  = PrototypeValidator.validateHtml(html, page, s"page[$idx]")
      val failures = results.filterNot(_.isPass)

      info(s"  Kept options    : ${keptOptions.mkString(", ")}")
      info(s"  Dropped option  : $droppedOption")
      logResults(failures)

      failures should not be empty
      failures.exists(_.check.contains(droppedOption)) shouldBe true
    }

    "missing service route: incomplete service descriptor should be caught" in {
      val firstPage = journey.pages.head
      val incompleteService = ServiceDescriptor(routes = List(
        ServiceRoute("GET", "/only-one", firstPage.pageType, firstPage.title)
      ))

      val report = ServiceValidator.validate(journey, incompleteService)

      logResults(report.failures)
      info(s"\n  ${report.failures.size} missing routes detected out of ${journey.pageCount} pages")

      report.allPassed shouldBe false
      report.failures.size should be >= (journey.pageCount - 1)
    }

    "correct page: matching HTML should pass all checks" in {
      val (page, idx) = findPageByType("contentPage")

      val html = s"""<html><body><h1>${page.title}</h1></body></html>"""

      val results = PrototypeValidator.validateHtml(html, page, s"page[$idx]")

      logResults(results)

      results.forall(_.isPass) shouldBe true
    }
  }
}
