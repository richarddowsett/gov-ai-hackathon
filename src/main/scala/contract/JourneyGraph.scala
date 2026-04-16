package contract

/** A single step in a journey path, recording which page was visited
  * and what answer was selected (if the page was a branching page).
  */
case class PathStep(arrayIndex: Int, page: Page, selectedAnswer: Option[String])

/** A complete path through the journey from start to a terminal page. */
case class JourneyPath(steps: List[PathStep]) {
  def description: String = {
    val choices = steps.flatMap(_.selectedAnswer).mkString(" -> ")
    val firstTitle = steps.headOption.map(_.page.title).getOrElse("(empty)")
    val lastTitle  = steps.lastOption.map(_.page.title).getOrElse("(empty)")
    val choicesSuffix = if (choices.nonEmpty) s" [choices: $choices]" else ""
    s"$firstTitle ... $lastTitle$choicesSuffix"
  }
}

/** Builds a directed graph from a [[Journey]] and enumerates all valid paths.
  *
  * Each branching page (boolean, radioButton) forks into multiple paths.
  * Linear pages produce a single edge to the next page.
  * A path terminates when the index points beyond the pages array.
  */
object JourneyGraph {

  /** Enumerate every distinct start-to-terminal path through the journey. */
  def enumerateAllPaths(journey: Journey): List[JourneyPath] = {
    if (journey.pages.isEmpty) return Nil

    def walk(currentIndex: Int, visited: Set[Int], acc: List[PathStep]): List[JourneyPath] = {
      if (currentIndex < 0 || currentIndex >= journey.pageCount) {
        List(JourneyPath(acc.reverse))
      } else if (visited.contains(currentIndex)) {
        // cycle detection — treat as terminal to avoid infinite recursion
        List(JourneyPath(acc.reverse))
      } else {
        val page      = journey.pages(currentIndex)
        val newVisited = visited + currentIndex

        page.index match {
          case LinearIndex(next) =>
            val step = PathStep(currentIndex, page, selectedAnswer = None)
            walk(next, newVisited, step :: acc)

          case BranchingIndex(routes) =>
            routes.toList.flatMap { case (answer, nextIndex) =>
              val step = PathStep(currentIndex, page, selectedAnswer = Some(answer))
              walk(nextIndex, newVisited, step :: acc)
            }
        }
      }
    }

    walk(currentIndex = 0, visited = Set.empty, acc = Nil)
  }

  /** Deduplicate paths that visit exactly the same sequence of pages
    * (e.g. multiple radio options all leading to the same next page).
    */
  def uniquePaths(journey: Journey): List[JourneyPath] =
    enumerateAllPaths(journey)
      .groupBy(_.steps.map(_.arrayIndex))
      .values
      .map(_.head)
      .toList
      .sortBy(_.steps.map(_.arrayIndex).mkString(","))
}
