package contract

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

/** Validates a service's route descriptor against the journey JSON contract.
  *
  * In a real scenario, a Play Framework service would expose metadata about
  * its routes (or these would be extracted from conf/routes at build time).
  * This spec checks that the service descriptor covers every page in the
  * journey contract with the correct page type, title, and branching routes.
  */
class ServiceContractSpec extends AnyFreeSpec with Matchers {

  private val journey = JourneyParser.parseFile("example/journey.json").toOption.get
  private val service = ServiceDescriptor.parseFile("service/routes.json")

  "ServiceContractSpec" - {
    "should be able to parse the service descriptor" in {
      service shouldBe a[Right[_, _]]
    }

    "should have a route for every page in the journey" in {
      val sd     = service.toOption.get
      val report = ServiceValidator.validate(journey, sd)

      withClue(report.summary) {
        report.allPassed shouldBe true
      }
    }

    journey.pages.toList.zipWithIndex.foreach { case (page, idx) =>
      s"page[$idx] '${page.title}'" - {

        "should have a matching service route with the correct page type" in {
          val sd    = service.toOption.get
          val route = sd.routes.find(_.title == page.title)

          route shouldBe defined
          route.get.pageType shouldBe page.pageType
        }

        if (page.index.isInstanceOf[BranchingIndex]) {
          "should have all branching routes defined in the service" in {
            val sd        = service.toOption.get
            val route     = sd.routes.find(_.title == page.title).get
            val branching = page.index.asInstanceOf[BranchingIndex]

            branching.routes.keys.foreach { answer =>
              withClue(s"Missing route for answer '$answer': ") {
                route.nextRoutes should contain key answer
              }
            }
          }
        }
      }
    }
  }
}
