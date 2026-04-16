package contract

import java.nio.file.{Files, Path, Paths}

object ContractLoader {
  private val JourneyPath = Paths.get("example/journey.json")

  private def pageId(position: Int): String = s"page-$position"

  private def pageIdFromValue(value: Int, total: Int): String = {
    val nextPosition = value + 1
    if (nextPosition > total) "END" else pageId(nextPosition)
  }

  def loadJourneyContract(path: Path = JourneyPath): JourneyContract = {
    SchemaValidator.validateJourneyFile(path)
    val root = ujson.read(Files.readString(path))
    val pageValues = root("pages").arr.toSeq
    val totalPages = pageValues.size

    val pages = pageValues.zipWithIndex.map { case (page, idx) =>
      val position = idx + 1

      val options = page.obj.get("options").map(_.arr.toSeq.map(_.str)).getOrElse(Seq.empty)
      val questions = page.obj
        .get("questions")
        .map(_.arr.toSeq.map(q => q("questionTitle").str))
        .getOrElse(Seq.empty)

      val validation = page.obj.get("validation") match {
        case Some(v) if v.isInstanceOf[ujson.Str] => Seq(v.str)
        case Some(v) if v.isInstanceOf[ujson.Arr] => v.arr.toSeq.map(_.str)
        case _                                     => Seq.empty
      }

      val transitions = page("index") match {
        case value: ujson.Num =>
          Map("_default" -> pageIdFromValue(value.value.toInt, totalPages))

        case value: ujson.Obj =>
          value.value.toSeq.map { case (answerKey, destination) =>
            answerKey -> pageIdFromValue(destination.num.toInt, totalPages)
          }.toMap

        case other =>
          throw new IllegalArgumentException(s"Unsupported index shape on ${pageId(position)}: $other")
      }

      JourneyPage(
        id = pageId(position),
        title = page("title").str,
        pageType = page("type").str,
        options = options,
        questions = questions,
        validation = validation,
        transitions = transitions
      )
    }

    JourneyContract(name = "journey", pages = pages)
  }
}
