package contract

import play.api.libs.json._

/** Represents the navigational index for a page in the journey.
  *
  * Linear pages have a single integer pointing to the next page's array position.
  * Branching pages (boolean, radioButton) map each possible answer to a target page index.
  * An index pointing beyond the pages array is treated as a terminal (end of journey).
  */
sealed trait PageIndex
case class LinearIndex(next: Int)                     extends PageIndex
case class BranchingIndex(routes: Map[String, Int])   extends PageIndex

/** A single question within a multipleQuestionsPage. */
case class Question(questionTitle: String)

object Question {
  implicit val reads: Reads[Question] = Json.reads[Question]
}

/** A page in the journey, as defined by the JSON contract.
  *
  * @param pageType    one of: contentPage, string, datePage, boolean, radioButton, checkbox, multipleQuestionsPage
  * @param title       the heading displayed to the user — used as the primary validation anchor
  * @param index       where to navigate next (see [[PageIndex]])
  * @param options     radio/checkbox option labels (empty for non-option pages)
  * @param questions   sub-questions for multipleQuestionsPage (empty otherwise)
  * @param validation  optional regex patterns for input validation
  */
case class Page(
    pageType:   String,
    title:      String,
    index:      PageIndex,
    options:    List[String]          = Nil,
    questions:  List[Question]        = Nil,
    validation: Option[List[String]]  = None
)

/** The top-level journey contract: an ordered list of pages forming a directed graph. */
case class Journey(pages: Vector[Page]) {
  def pageCount: Int = pages.size
}
