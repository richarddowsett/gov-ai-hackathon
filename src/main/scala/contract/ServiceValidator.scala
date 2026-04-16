package contract

import java.nio.file.{Files, Path, Paths}

object ServiceValidator {
  private val ServicePath = Paths.get("service/routes.json")

  private final case class ServicePage(id: String, title: String, transitions: Map[String, String])

  private def loadServicePages(path: Path = ServicePath): Map[String, ServicePage] = {
    val root = ujson.read(Files.readString(path))
    root("pages").arr.toSeq
      .map { page =>
        val transitions = page.obj
          .get("next")
          .map(_.obj.toSeq.map { case (k, v) => k -> v.str }.toMap)
          .getOrElse(Map.empty[String, String])

        val parsed = ServicePage(
          id = page("id").str,
          title = page("title").str,
          transitions = transitions
        )
        parsed.id -> parsed
      }
      .toMap
  }

  def validate(contract: JourneyContract): ValidationResult = {
    JourneyRunner.run(contract)
    val serviceById = loadServicePages()

    val contractIssues = contract.pages.flatMap { page =>
      serviceById.get(page.id) match {
        case None => Seq(DriftIssue(page.id, "missing", page.id, "(missing service page)"))
        case Some(actual) =>
          val titleIssue =
            if (actual.title == page.title) Seq.empty
            else Seq(DriftIssue(page.id, "mismatch", page.title, actual.title))

          val transitionIssues = page.transitions.toSeq.flatMap { case (key, expectedTarget) =>
            actual.transitions.get(key) match {
              case None => Seq(DriftIssue(page.id, "missing", s"transition:$key->$expectedTarget", "(missing service transition)"))
              case Some(actualTarget) if actualTarget != expectedTarget =>
                Seq(DriftIssue(page.id, "transition", s"$key->$expectedTarget", s"$key->$actualTarget"))
              case _ => Seq.empty
            }
          }

          titleIssue ++ transitionIssues
      }
    }

    val expectedIds = contract.pages.map(_.id).toSet
    val extraIssues = serviceById.keys.filterNot(expectedIds.contains).toSeq.sorted.map { id =>
      DriftIssue(id, "extra", "(none)", id)
    }

    ValidationResult(target = "service", issues = contractIssues ++ extraIssues)
  }
}
