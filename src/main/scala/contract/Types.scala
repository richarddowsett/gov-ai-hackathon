package contract

final case class JourneyPage(
    id: String,
    title: String,
    pageType: String,
    options: Seq[String],
    questions: Seq[String],
    validation: Seq[String],
    transitions: Map[String, String]
)

final case class JourneyContract(name: String, pages: Seq[JourneyPage])

final case class DriftIssue(pageId: String, kind: String, expected: String, actual: String)

final case class ValidationResult(target: String, issues: Seq[DriftIssue]) {
  def ok: Boolean = issues.isEmpty
}

final case class JourneyTransition(from: String, by: String, to: String)

final case class JourneyRunState(
    journey: String,
    visited: Seq[String],
    transitions: Seq[JourneyTransition]
)
