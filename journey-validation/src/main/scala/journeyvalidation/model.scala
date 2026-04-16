package journeyvalidation

import play.api.libs.json._

sealed trait PageIndex
case class LinearIndex(next: Int)                   extends PageIndex
case class BranchingIndex(routes: Map[String, Int]) extends PageIndex

case class Question(questionTitle: String)

object Question {
  implicit val reads: Reads[Question] = Json.reads[Question]
}

case class Page(
    pageType:   String,
    title:      String,
    index:      PageIndex,
    options:    List[String]         = Nil,
    questions:  List[Question]       = Nil,
    validation: Option[List[String]] = None
)

case class Journey(pages: Vector[Page]) {
  def pageCount: Int = pages.size
}

sealed trait ValidationResult {
  def check: String
  def isPass: Boolean
}

case class Pass(check: String) extends ValidationResult {
  val isPass = true
}

case class Fail(
    check:    String,
    expected: String,
    actual:   String
) extends ValidationResult {
  val isPass = false
}

case class ValidationReport(
    title:   String,
    results: List[ValidationResult]
) {
  def allPassed: Boolean = results.forall(_.isPass)
  def passes:  List[Pass] = results.collect { case p: Pass => p }
  def failures: List[Fail] = results.collect { case f: Fail => f }

  def render: String = {
    val width   = 72
    val bar     = "=" * width
    val thinBar = "-" * width

    val header =
      s"""|+$bar+
          ||  $title${" " * Math.max(0, width - title.length - 2)}|
          |+$bar+""".stripMargin

    val lines = results.map {
      case Pass(chk) =>
        s"  [PASS]  $chk"
      case Fail(chk, exp, act) =>
        s"""|  [FAIL]  $chk
            |        expected : $exp
            |        actual   : $act""".stripMargin
    }

    val passCount     = passes.size
    val failCount     = failures.size
    val total         = results.size
    val summaryStatus = if (allPassed) "ALL CHECKS PASSED" else s"$failCount of $total FAILED"

    val footer =
      s"""|  $thinBar
          |  $summaryStatus  ($passCount passed, $failCount failed, $total total)
          |""".stripMargin

    (header :: lines ::: List(footer)).mkString("\n")
  }
}

object ValidationReport {
  def combine(title: String, reports: List[ValidationReport]): ValidationReport =
    ValidationReport(title, reports.flatMap(_.results))
}
