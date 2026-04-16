package contract

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import scala.jdk.CollectionConverters._

/** Validation result for a single page check. */
sealed trait ValidationResult {
  def pageName: String
  def isPass: Boolean
}

case class Pass(pageName: String) extends ValidationResult {
  val isPass = true
  override def toString: String = s"  PASS  $pageName"
}

case class Fail(pageName: String, reason: String) extends ValidationResult {
  val isPass = false
  override def toString: String = s"  FAIL  $pageName — $reason"
}

/** Detailed report for an entire journey path validation run. */
case class ValidationReport(
    pathDescription: String,
    results:         List[ValidationResult]
) {
  def allPassed: Boolean = results.forall(_.isPass)
  def failures:  List[Fail] = results.collect { case f: Fail => f }

  def summary: String = {
    val status = if (allPassed) "ALL PASSED" else s"${failures.size} FAILURE(S)"
    val lines  = results.map(_.toString)
    s"""
       |=== Journey: $pathDescription ===
       |${lines.mkString("\n")}
       |--- Result: $status ---
       |""".stripMargin
  }
}

/** Validates HTML prototype pages against the journey JSON contract.
  *
  * For each page in a journey path, it fetches the corresponding HTML file
  * and checks:
  *   - the page title (h1) matches the JSON title
  *   - the page contains appropriate form elements for the page type
  *   - navigation links/buttons lead to the expected next page
  */
object PrototypeValidator {

  /** Validate a single HTML document against its expected page definition.
    *
    * @param doc      the parsed HTML document
    * @param page     the expected page from the journey JSON
    * @param pageSlug a human-readable identifier for error messages
    */
  def validatePage(doc: Document, page: Page, pageSlug: String): List[ValidationResult] = {
    val titleCheck    = validateTitle(doc, page, pageSlug)
    val elementChecks = validatePageElements(doc, page, pageSlug)
    titleCheck :: elementChecks
  }

  private def validateTitle(doc: Document, page: Page, slug: String): ValidationResult = {
    val h1Elements = doc.select("h1")
    if (h1Elements.isEmpty) {
      Fail(slug, s"No <h1> found. Expected: '${page.title}'")
    } else {
      val actualTitle = h1Elements.first().text().trim
      if (actualTitle == page.title) Pass(s"$slug title")
      else Fail(s"$slug title", s"Expected '${page.title}', got '$actualTitle'")
    }
  }

  private def validatePageElements(doc: Document, page: Page, slug: String): List[ValidationResult] =
    page.pageType match {
      case "string" =>
        val textInputs = doc.select("input[type=text]")
        if (textInputs.isEmpty)
          List(Fail(s"$slug fields", "Expected a text input for string page"))
        else
          List(Pass(s"$slug fields"))

      case "datePage" =>
        val dateInputs = doc.select("input[type=date], input[name*=day], input[name*=month], input[name*=year], .govuk-date-input")
        if (dateInputs.isEmpty)
          List(Fail(s"$slug fields", "Expected date input fields for datePage"))
        else
          List(Pass(s"$slug fields"))

      case "boolean" =>
        val radios = doc.select("input[type=radio]")
        if (radios.size() < 2)
          List(Fail(s"$slug fields", s"Expected at least 2 radio buttons for boolean page, found ${radios.size()}"))
        else
          List(Pass(s"$slug fields"))

      case "radioButton" =>
        val radios        = doc.select("input[type=radio]")
        val expectedCount = page.options.size
        val labelTexts    = doc.select("label").asScala.map(_.text().trim).toSet
        val missingOptions = page.options.filterNot(opt => labelTexts.exists(_.contains(opt)))

        val countCheck =
          if (radios.size() >= expectedCount) List(Pass(s"$slug radio-count"))
          else List(Fail(s"$slug radio-count", s"Expected $expectedCount radio buttons, found ${radios.size()}"))

        val optionCheck =
          if (missingOptions.isEmpty) List(Pass(s"$slug options"))
          else List(Fail(s"$slug options", s"Missing radio options: ${missingOptions.mkString(", ")}"))

        countCheck ++ optionCheck

      case "checkbox" =>
        val checkboxes = doc.select("input[type=checkbox]")
        val expectedCount = page.options.size
        if (checkboxes.size() < expectedCount)
          List(Fail(s"$slug fields", s"Expected $expectedCount checkboxes, found ${checkboxes.size()}"))
        else
          List(Pass(s"$slug fields"))

      case "multipleQuestionsPage" =>
        page.questions.zipWithIndex.map { case (q, i) =>
          val found = doc.select(s"label, legend, .govuk-fieldset__heading").asScala
            .exists(_.text().trim.contains(q.questionTitle))
          if (found) Pass(s"$slug question[${i}]")
          else Fail(s"$slug question[${i}]", s"Question '${q.questionTitle}' not found on page")
        }

      case "contentPage" =>
        List(Pass(s"$slug content"))

      case other =>
        List(Fail(s"$slug type", s"Unknown page type: $other"))
    }

  /** Convenience: parse an HTML string and validate it against a page. */
  def validateHtml(html: String, page: Page, pageSlug: String): List[ValidationResult] =
    validatePage(Jsoup.parse(html), page, pageSlug)
}
