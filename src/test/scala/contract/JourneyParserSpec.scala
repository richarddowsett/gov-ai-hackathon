package contract

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class JourneyParserSpec extends AnyFreeSpec with Matchers {

  private val exampleJsonPath = "example/journey.json"

  "JourneyParser" - {
    "should parse the example journey JSON from file" in {
      val result = JourneyParser.parseFile(exampleJsonPath)
      result shouldBe a[Right[_, _]]

      val journey = result.toOption.get
      journey.pageCount shouldBe 9
    }

    "should parse all 7 page types correctly" in {
      val journey = JourneyParser.parseFile(exampleJsonPath).toOption.get
      val types   = journey.pages.map(_.pageType).toSet

      types should contain("contentPage")
      types should contain("string")
      types should contain("datePage")
      types should contain("boolean")
      types should contain("radioButton")
      types should contain("checkbox")
      types should contain("multipleQuestionsPage")
    }

    "should parse linear indices as LinearIndex" in {
      val journey = JourneyParser.parseFile(exampleJsonPath).toOption.get
      val welcome = journey.pages(0)

      welcome.pageType shouldBe "contentPage"
      welcome.title    shouldBe "Welcome to our survey"
      welcome.index    shouldBe LinearIndex(1)
    }

    "should parse boolean branching indices as BranchingIndex" in {
      val journey = JourneyParser.parseFile(exampleJsonPath).toOption.get
      val license = journey.pages(3)

      license.pageType shouldBe "boolean"
      license.index    shouldBe a[BranchingIndex]

      val branching = license.index.asInstanceOf[BranchingIndex]
      branching.routes("true")  shouldBe 4
      branching.routes("false") shouldBe 6
    }

    "should parse radio button options and branching index" in {
      val journey = JourneyParser.parseFile(exampleJsonPath).toOption.get
      val vehicle = journey.pages(4)

      vehicle.pageType shouldBe "radioButton"
      vehicle.options  should contain allOf("Car", "Motorcycle", "Truck", "Other")

      val branching = vehicle.index.asInstanceOf[BranchingIndex]
      branching.routes("Car")   shouldBe 5
      branching.routes("Other") shouldBe 6
    }

    "should parse checkbox options" in {
      val journey  = JourneyParser.parseFile(exampleJsonPath).toOption.get
      val features = journey.pages(5)

      features.pageType shouldBe "checkbox"
      features.options should have size 5
      features.options should contain("GPS Navigation")
    }

    "should parse multipleQuestionsPage with questions and validation" in {
      val journey = JourneyParser.parseFile(exampleJsonPath).toOption.get
      val rate    = journey.pages(7)

      rate.pageType shouldBe "multipleQuestionsPage"
      rate.questions should have size 2
      rate.questions.head.questionTitle shouldBe "How satisfied are you with our service?"
      rate.validation shouldBe Some(List("^[1-5]$", "^[1-5]$"))
    }

    "should return Left for invalid JSON" in {
      val result = JourneyParser.parse("{invalid json}")
      result shouldBe a[Left[_, _]]
    }

    "should return Left for missing required fields" in {
      val result = JourneyParser.parse("""{"pages": [{"type": "contentPage"}]}""")
      result shouldBe a[Left[_, _]]
    }
  }
}
