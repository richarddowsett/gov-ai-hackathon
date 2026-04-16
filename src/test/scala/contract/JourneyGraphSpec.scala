package contract

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class JourneyGraphSpec extends AnyFreeSpec with Matchers {

  private val journey = JourneyParser.parseFile("example/journey.json").toOption.get

  "JourneyGraph" - {
    "should enumerate all paths through the example journey" in {
      val allPaths = JourneyGraph.enumerateAllPaths(journey)

      allPaths should not be empty
      allPaths.foreach { path =>
        path.steps should not be empty
        path.steps.head.arrayIndex shouldBe 0
        info(s"Path: ${path.description}")
      }
    }

    "should find paths that end at different terminal pages" in {
      val allPaths   = JourneyGraph.enumerateAllPaths(journey)
      val lastPages  = allPaths.map(_.steps.last.page.title).toSet

      lastPages should contain("Rate your experience")
      lastPages should contain("Thank you for completing the survey!")
    }

    "should find the license=true path going through vehicle type and features" in {
      val allPaths = JourneyGraph.enumerateAllPaths(journey)
      val licenseTrueCarPath = allPaths.find { path =>
        path.steps.exists(s => s.page.title.contains("license") && s.selectedAnswer.contains("true")) &&
        path.steps.exists(s => s.page.title.contains("vehicle") && s.selectedAnswer.contains("Car"))
      }

      licenseTrueCarPath shouldBe defined
      val path = licenseTrueCarPath.get
      val titles = path.steps.map(_.page.title)
      titles should contain("Select all vehicle features you have:")
      titles should contain("Rate your experience")
    }

    "should find the license=false path skipping vehicle type" in {
      val allPaths = JourneyGraph.enumerateAllPaths(journey)
      val licenseFalsePath = allPaths.find { path =>
        path.steps.exists(s => s.page.title.contains("license") && s.selectedAnswer.contains("false"))
      }

      licenseFalsePath shouldBe defined
      val titles = licenseFalsePath.get.steps.map(_.page.title)
      titles should not contain "What type of vehicle do you own?"
      titles should contain("What is your preferred transportation method?")
      titles should contain("Thank you for completing the survey!")
    }

    "should produce unique paths collapsing identical-route options" in {
      val uniquePaths = JourneyGraph.uniquePaths(journey)

      uniquePaths.size should be < JourneyGraph.enumerateAllPaths(journey).size
      info(s"All paths: ${JourneyGraph.enumerateAllPaths(journey).size}, Unique: ${uniquePaths.size}")
    }

    "should detect cycles and terminate gracefully" in {
      val cyclicJourney = Journey(Vector(
        Page("contentPage", "A", BranchingIndex(Map("loop" -> 0)))
      ))
      val paths = JourneyGraph.enumerateAllPaths(cyclicJourney)
      paths should have size 1
      paths.head.steps should have size 1
    }

    "should handle empty journey" in {
      JourneyGraph.enumerateAllPaths(Journey(Vector.empty)) shouldBe Nil
    }

    "every path should start at page 0 (the entry point)" in {
      val allPaths = JourneyGraph.enumerateAllPaths(journey)
      allPaths.foreach { path =>
        path.steps.head.page.title shouldBe "Welcome to our survey"
      }
    }
  }
}
