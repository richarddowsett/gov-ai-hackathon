package contract

import org.jsoup.Jsoup

import java.nio.file.{Files, Paths}

object PrototypeValidator {
  private val PrototypeDir = Paths.get("prototype")

  private final case class PrototypePage(title: String, transitions: Map[String, String])

  private def keyToAttr(key: String): String =
    key.toLowerCase.replaceAll("[^a-z0-9]+", "-").stripPrefix("-").stripSuffix("-")

  private def loadPrototypePage(pageId: String): PrototypePage = {
    val filePath = PrototypeDir.resolve(s"$pageId.html")
    require(Files.exists(filePath), s"Missing prototype file: $filePath")

    val html = Files.readString(filePath)
    val doc = Jsoup.parse(html)
    val main = Option(doc.selectFirst("main")).getOrElse(throw new IllegalArgumentException(s"Missing <main> in $filePath"))

    val title = Option(doc.selectFirst("h1")).map(_.text().trim).getOrElse("")

    val default = Option(main.attr("data-next")).map(_.trim).getOrElse("")
    val keyed = main.attributes().asList().toArray.toSeq
      .map(_.asInstanceOf[org.jsoup.nodes.Attribute])
      .filter(a => a.getKey.startsWith("data-next-") && a.hasDeclaredValue)
      .map { a =>
        val raw = a.getKey.stripPrefix("data-next-")
        raw -> a.getValue.trim
      }
      .toMap

    val transitions = if (default.nonEmpty) keyed + ("_default" -> default) else keyed
    PrototypePage(title = title, transitions = transitions)
  }

  def validate(contract: JourneyContract): ValidationResult = {
    JourneyRunner.run(contract)

    val issues = contract.pages.flatMap { page =>
      try {
        val actual = loadPrototypePage(page.id)

        val titleIssue =
          if (actual.title == page.title) Seq.empty
          else Seq(DriftIssue(page.id, "mismatch", page.title, if (actual.title.isEmpty) "(missing title)" else actual.title))

        val transitionIssues = page.transitions.toSeq.flatMap { case (expectedKey, expectedTarget) =>
          val lookupKey = if (expectedKey == "_default") "_default" else keyToAttr(expectedKey)
          actual.transitions.get(lookupKey) match {
            case None => Seq(DriftIssue(page.id, "missing", s"transition:$expectedKey->$expectedTarget", "(missing in prototype)"))
            case Some(actualTarget) if actualTarget != expectedTarget =>
              Seq(DriftIssue(page.id, "transition", s"$expectedKey->$expectedTarget", s"$expectedKey->$actualTarget"))
            case _ => Seq.empty
          }
        }

        titleIssue ++ transitionIssues
      } catch {
        case ex: Exception =>
          Seq(DriftIssue(page.id, "missing", s"${page.id}.html", ex.getMessage))
      }
    }

    ValidationResult(target = "prototype", issues = issues)
  }
}
