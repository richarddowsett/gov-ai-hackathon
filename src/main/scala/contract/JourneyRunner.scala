package contract

import java.nio.file.{Files, Path, Paths}
import scala.collection.mutable.ListBuffer

object JourneyRunner {
  private val OutputPath = Paths.get("journey-state.json")

  def run(contract: JourneyContract, outputPath: Path = OutputPath): JourneyRunState = {
    val pageById = contract.pages.map(page => page.id -> page).toMap

    require(contract.pages.nonEmpty, "Journey must contain at least one page")

    val visited = ListBuffer.empty[String]
    val transitions = ListBuffer.empty[JourneyTransition]
    val seen = scala.collection.mutable.Set.empty[String]

    var current = contract.pages.head.id

    while (current != "END") {
      if (seen.contains(current)) {
        throw new IllegalArgumentException(s"Cycle detected at page '$current'")
      }

      pageById.get(current) match {
        case Some(page) =>
          seen += current
          visited += page.id

          val next = page.transitions.getOrElse("_default", page.transitions.values.headOption.getOrElse("END"))
          val edgeKey = if (page.transitions.contains("_default")) "_default" else page.transitions.keys.head

          transitions += JourneyTransition(page.id, edgeKey, next)
          current = next

        case None =>
          throw new IllegalArgumentException(s"Unknown page reference '$current'")
      }
    }

    val state = JourneyRunState(
      journey = contract.name,
      visited = visited.toSeq,
      transitions = transitions.toSeq
    )

    val json = ujson.Obj(
      "journey" -> state.journey,
      "visited" -> ujson.Arr.from(state.visited),
      "transitions" -> ujson.Arr.from(
        state.transitions.map(t => ujson.Obj("from" -> t.from, "by" -> t.by, "to" -> t.to))
      )
    )

    Files.writeString(outputPath, ujson.write(json, indent = 2))
    state
  }
}
