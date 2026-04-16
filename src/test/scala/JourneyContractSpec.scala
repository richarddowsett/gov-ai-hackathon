import contract.{ContractLoader, DriftIssue, PrototypeValidator, ServiceValidator}
import org.scalatest.funsuite.AnyFunSuite

class JourneyContractSpec extends AnyFunSuite {

  private val contract = ContractLoader.loadJourneyContract()

  private def formatIssues(issues: Seq[DriftIssue]): String =
    issues
      .map(i => s"page=${i.pageId} kind=${i.kind} expected='${i.expected}' actual='${i.actual}'")
      .mkString("\n")

  test("prototype matches journey contract") {
    val result = PrototypeValidator.validate(contract)
    assert(result.ok, s"Prototype drift detected:\n${formatIssues(result.issues)}")
  }

  test("service implementation matches journey contract") {
    val result = ServiceValidator.validate(contract)
    assert(result.ok, s"Service drift detected:\n${formatIssues(result.issues)}")
  }

  test("all journey transitions point to known pages or END") {
    val ids = contract.pages.map(_.id).toSet

    val invalidTransitions = contract.pages.flatMap { page =>
      page.transitions.values
        .filterNot(next => next == "END" || ids.contains(next))
        .map(next => s"${page.id} -> $next")
    }

    assert(
      invalidTransitions.isEmpty,
      s"Invalid transitions found:\n${invalidTransitions.mkString("\n")}"
    )
  }
}
