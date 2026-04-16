package contract

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import scala.jdk.CollectionConverters._

sealed trait ValidationResult {
  def check: String
  def isPass: Boolean
}

case class Pass(check: String) extends ValidationResult {
  val isPass = true
}

case class Fail(check: String, expected: String, actual: String) extends ValidationResult {
  val isPass = false
}

/** Detailed report for a validation run, with formatted human-readable output. */
case class ValidationReport(
    title:   String,
    results: List[ValidationResult]
) {
  def allPassed: Boolean = results.forall(_.isPass)
  def passes:    List[Pass] = results.collect { case p: Pass => p }
  def failures:  List[Fail] = results.collect { case f: Fail => f }

  def render: String = {
    val width  = 72
    val bar    = "═" * width
    val thinBar = "─" * width

    val header =
      s"""
         |╔$bar╗
         |║  $title${" " * (width - title.length - 2)}║
         |╚$bar╝""".stripMargin

    val lines = results.map {
      case Pass(check) =>
        s"  ✅  $check"
      case Fail(check, expected, actual) =>
        s"""  ❌  $check
           |        expected : $expected
           |        actual   : $actual""".stripMargin
    }

    val passCount = passes.size
    val failCount = failures.size
    val total     = results.size

    val summaryStatus = if (allPassed) "ALL CHECKS PASSED" else s"$failCount of $total FAILED"
    val summaryIcon   = if (allPassed) "✅" else "❌"

    val footer =
      s"""  $thinBar
         |  $summaryIcon  $summaryStatus  ($passCount passed, $failCount failed, $total total)
         |""".stripMargin

    (header :: lines ::: List(footer)).mkString("\n")
  }
}

object ValidationReport {
  /** Merge multiple reports into a single combined report. */
  def combine(title: String, reports: List[ValidationReport]): ValidationReport =
    ValidationReport(title, reports.flatMap(_.results))
}

/** Validates HTML prototype pages against the journey JSON contract.
  *
  * Every check is derived from the JSON — no hardcoded expectations.
  */
object PrototypeValidator {

  def validatePage(doc: Document, page: Page, pageLabel: String): List[ValidationResult] = {
    val titleCheck    = validateTitle(doc, page, pageLabel)
    val elementChecks = validatePageElements(doc, page, pageLabel)
    titleCheck :: elementChecks
  }

  private def validateTitle(doc: Document, page: Page, label: String): ValidationResult = {
    val h1Elements = doc.select("h1")
    if (h1Elements.isEmpty)
      Fail(s"$label — title", page.title, "<no h1 found>")
    else {
      val actualTitle = h1Elements.first().text().trim
      if (actualTitle == page.title) Pass(s"$label — title matches")
      else Fail(s"$label — title", page.title, actualTitle)
    }
  }

  private def validatePageElements(doc: Document, page: Page, label: String): List[ValidationResult] =
    page.pageType match {
      case "string" =>
        val found = doc.select("input[type=text]").size()
        if (found >= 1) List(Pass(s"$label — has text input"))
        else List(Fail(s"$label — text input", "at least 1 <input type=text>", s"found $found"))

      case "datePage" =>
        val found = doc.select("input[type=date], input[name*=day], input[name*=month], input[name*=year], .govuk-date-input").size()
        if (found >= 1) List(Pass(s"$label — has date fields"))
        else List(Fail(s"$label — date fields", "date input elements", s"found $found"))

      case "boolean" =>
        val found = doc.select("input[type=radio]").size()
        if (found >= 2) List(Pass(s"$label — has yes/no radios ($found found)"))
        else List(Fail(s"$label — boolean radios", "at least 2 radio buttons", s"found $found"))

      case "radioButton" =>
        val radios     = doc.select("input[type=radio]").size()
        val labelTexts = doc.select("label").asScala.map(_.text().trim).toSet
        val missing    = page.options.filterNot(opt => labelTexts.exists(_.contains(opt)))

        val countCheck =
          if (radios >= page.options.size) List(Pass(s"$label — radio count ($radios found)"))
          else List(Fail(s"$label — radio count", s"${page.options.size} radio buttons", s"found $radios"))

        val optionChecks = page.options.map { opt =>
          if (labelTexts.exists(_.contains(opt))) Pass(s"$label — option '$opt' present")
          else Fail(s"$label — option '$opt'", s"label containing '$opt'", "not found")
        }

        countCheck ++ optionChecks

      case "checkbox" =>
        val found = doc.select("input[type=checkbox]").size()
        val countCheck =
          if (found >= page.options.size) List(Pass(s"$label — checkbox count ($found found)"))
          else List(Fail(s"$label — checkbox count", s"${page.options.size} checkboxes", s"found $found"))

        val labelTexts = doc.select("label").asScala.map(_.text().trim).toSet
        val optionChecks = page.options.map { opt =>
          if (labelTexts.exists(_.contains(opt))) Pass(s"$label — option '$opt' present")
          else Fail(s"$label — option '$opt'", s"label containing '$opt'", "not found")
        }

        countCheck ++ optionChecks

      case "multipleQuestionsPage" =>
        page.questions.map { q =>
          val found = doc.select("label, legend, .govuk-fieldset__heading").asScala
            .exists(_.text().trim.contains(q.questionTitle))
          if (found) Pass(s"$label — question '${q.questionTitle}' present")
          else Fail(s"$label — question", q.questionTitle, "not found on page")
        }

      case "contentPage" =>
        List(Pass(s"$label — content page (no form required)"))

      case other =>
        List(Fail(s"$label — page type", "known page type", s"'$other'"))
    }

  def validateHtml(html: String, page: Page, pageLabel: String): List[ValidationResult] =
    validatePage(Jsoup.parse(html), page, pageLabel)
}
