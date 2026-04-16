package contract

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll
import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}

/** JSON-driven contract validation spec.
  *
  * Point it at any journey JSON + prototype directory + service descriptor
  * and it validates everything automatically. Nothing is hardcoded.
  *
  * Configure via system properties:
  *   -Djourney.json=example/journey.json
  *   -Dprototype.dir=prototype
  *   -Dservice.json=service/routes.json
  */
class JourneyContractSpec extends AnyFreeSpec with Matchers with BeforeAndAfterAll {

  private val journeyPath = ValidatorConfig.journeyPath
  private val protoDir    = ValidatorConfig.prototypeDir
  private val servicePath = ValidatorConfig.servicePath

  private val journey = JourneyParser.parseFile(journeyPath) match {
    case Right(j) => j
    case Left(err) => fail(s"Could not load journey JSON ($journeyPath): $err")
  }

  private val server  = new EmbeddedServer(protoDir)
  private val client  = HttpClient.newHttpClient()
  private var baseUrl = ""

  override def beforeAll(): Unit = {
    server.start()
    baseUrl = server.baseUrl
    info(s"╔══════════════════════════════════════════════════════╗")
    info(s"║  Journey Contract Validator                         ║")
    info(s"║  journey   : $journeyPath")
    info(s"║  prototype : $protoDir")
    info(s"║  service   : $servicePath")
    info(s"║  pages     : ${journey.pageCount}")
    info(s"║  paths     : ${JourneyGraph.uniquePaths(journey).size} unique, ${JourneyGraph.enumerateAllPaths(journey).size} total")
    info(s"╚══════════════════════════════════════════════════════╝")
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    server.stop()
    super.afterAll()
  }

  private def fetchPage(pageIndex: Int): String = {
    val request  = HttpRequest.newBuilder(URI.create(s"$baseUrl/page/$pageIndex")).build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    withClue(s"HTTP GET /page/$pageIndex returned ${response.statusCode()}") {
      response.statusCode() shouldBe 200
    }
    response.body()
  }

  private def logResults(results: List[ValidationResult]): Unit =
    results.foreach {
      case Pass(check) => info(s"  ✅  $check")
      case f: Fail =>
        val base = s"  ❌  ${f.check}\n        expected : ${f.expected}\n        actual   : ${f.actual}"
        val contract = f.contractRef.map(r => s"\n        contract : $r").getOrElse("")
        val fix      = f.sourceRef.map(r =>   s"\n        fix at   : $r").getOrElse("")
        info(base + contract + fix)
    }

  // ─── 1. Schema structure ─────────────────────────────────────────────

  "Journey schema" - {

    "should load and contain at least one page" in {
      journey.pageCount should be > 0
      info(s"  Loaded ${journey.pageCount} pages")
    }

    "every page should have a non-empty title" in {
      journey.pages.zipWithIndex.foreach { case (page, idx) =>
        withClue(s"page[$idx] has empty title: ") {
          page.title should not be empty
        }
      }
    }

    "every page should have a recognised type" in {
      val knownTypes = Set("contentPage", "string", "datePage", "boolean",
                           "radioButton", "checkbox", "multipleQuestionsPage")
      journey.pages.zipWithIndex.foreach { case (page, idx) =>
        withClue(s"page[$idx] '${page.title}' has unknown type '${page.pageType}': ") {
          knownTypes should contain(page.pageType)
        }
      }
    }

    "every branching page should define routes" in {
      journey.pages.zipWithIndex.foreach { case (page, idx) =>
        page.index match {
          case BranchingIndex(routes) =>
            withClue(s"page[$idx] '${page.title}' is branching but has no routes: ") {
              routes should not be empty
            }
          case _ =>
        }
      }
    }

    "all indices should be non-negative" in {
      journey.pages.zipWithIndex.foreach { case (page, idx) =>
        page.index match {
          case LinearIndex(next) =>
            withClue(s"page[$idx] points to $next: ") { next should be >= 0 }
          case BranchingIndex(routes) =>
            routes.foreach { case (answer, target) =>
              withClue(s"page[$idx] answer '$answer' points to $target: ") { target should be >= 0 }
            }
        }
      }
    }
  }

  // ─── 2. Journey graph ────────────────────────────────────────────────

  "Journey graph" - {

    "should have at least one traversable path" in {
      val paths = JourneyGraph.enumerateAllPaths(journey)
      paths should not be empty
    }

    "every path should start at page[0] and not loop" in {
      JourneyGraph.enumerateAllPaths(journey).foreach { path =>
        path.steps.head.arrayIndex shouldBe 0
        val indices = path.steps.map(_.arrayIndex)
        withClue(s"Cycle detected in path: $indices") {
          indices.size shouldBe indices.distinct.size
        }
      }
    }

    JourneyGraph.uniquePaths(journey).zipWithIndex.foreach { case (path, i) =>
      s"path ${i + 1}: ${path.description}" in {
        info(s"  ${path.steps.map(s => s"[${s.arrayIndex}] ${s.page.title}").mkString(" → ")}")
        path.steps should not be empty
      }
    }
  }

  // ─── 3. Prototype validation ─────────────────────────────────────────

  "Prototype validation" - {

    journey.pages.toList.zipWithIndex.foreach { case (page, idx) =>

      s"page[$idx] (${page.pageType}) '${page.title}'" in {
        val html      = fetchPage(idx)
        val protoFile = SourceLocator.prototypeFile(protoDir, idx)
        val ctx       = Some(PrototypeValidator.FileContext(journeyPath, protoFile))
        val results   = PrototypeValidator.validateHtml(html, page, s"page[$idx]", ctx)

        logResults(results)

        val report = ValidationReport(s"page[$idx]", results)
        withClue(s"\n${report.render}") {
          report.allPassed shouldBe true
        }
      }
    }

    JourneyGraph.uniquePaths(journey).zipWithIndex.foreach { case (path, i) =>

      s"full path ${i + 1}: ${path.description}" in {
        val allResults = path.steps.flatMap { step =>
          val html      = fetchPage(step.arrayIndex)
          val protoFile = SourceLocator.prototypeFile(protoDir, step.arrayIndex)
          val ctx       = Some(PrototypeValidator.FileContext(journeyPath, protoFile))
          PrototypeValidator.validateHtml(html, step.page, s"page[${step.arrayIndex}]", ctx)
        }

        info(s"  Traversed ${path.steps.size} pages")
        logResults(allResults)

        val report = ValidationReport(s"Path ${i + 1}", allResults)
        withClue(s"\n${report.render}") {
          report.allPassed shouldBe true
        }
      }
    }
  }

  // ─── 4. Service validation ───────────────────────────────────────────

  "Service validation" - {

    s"all pages should have matching routes in $servicePath" in {
      val sd = ServiceDescriptor.parseFile(servicePath) match {
        case Right(s)  => s
        case Left(err) => fail(s"Could not load service descriptor ($servicePath): $err")
      }

      val ctx    = Some(ServiceValidator.FileContext(journeyPath, servicePath))
      val report = ServiceValidator.validate(journey, sd, ctx)

      logResults(report.results)
      info(s"\n  ${report.passes.size} passed, ${report.failures.size} failed out of ${report.results.size} checks")

      withClue(s"\n${report.render}") {
        report.allPassed shouldBe true
      }
    }
  }
}
