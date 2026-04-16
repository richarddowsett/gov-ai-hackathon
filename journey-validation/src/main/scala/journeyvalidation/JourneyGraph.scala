package journeyvalidation

case class PathStep(arrayIndex: Int, page: Page, selectedAnswer: Option[String])

case class JourneyPath(steps: List[PathStep]) {
  def description: String = {
    val choices     = steps.flatMap(_.selectedAnswer).mkString(" -> ")
    val firstTitle  = steps.headOption.map(_.page.title).getOrElse("(empty)")
    val lastTitle   = steps.lastOption.map(_.page.title).getOrElse("(empty)")
    val choiceSuffix = if (choices.nonEmpty) s" [choices: $choices]" else ""
    s"$firstTitle ... $lastTitle$choiceSuffix"
  }
}

object JourneyGraph {

  def enumerateAllPaths(journey: Journey): List[JourneyPath] = {
    if (journey.pages.isEmpty) return Nil

    def walk(currentIndex: Int, visited: Set[Int], acc: List[PathStep]): List[JourneyPath] = {
      if (currentIndex < 0 || currentIndex >= journey.pageCount) {
        List(JourneyPath(acc.reverse))
      } else if (visited.contains(currentIndex)) {
        List(JourneyPath(acc.reverse))
      } else {
        val page       = journey.pages(currentIndex)
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

  def uniquePaths(journey: Journey): List[JourneyPath] =
    enumerateAllPaths(journey)
      .groupBy(_.steps.map(_.arrayIndex))
      .values
      .map(_.head)
      .toList
      .sortBy(_.steps.map(_.arrayIndex).mkString(","))
}
