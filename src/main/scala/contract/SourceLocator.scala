package contract

import java.io.File
import scala.io.Source
import scala.util.Using

/** Finds line numbers in source files for pinpointing validation failures. */
object SourceLocator {

  private def readLines(file: String): Option[IndexedSeq[String]] =
    Using(Source.fromFile(file))(_.getLines().toIndexedSeq).toOption

  private def findLine(lines: IndexedSeq[String], needle: String): Option[Int] =
    lines.indexWhere(_.contains(needle)) match {
      case -1  => None
      case idx => Some(idx + 1)
    }

  /** Find the line number of a page's "title" field in the journey JSON. */
  def journeyPageTitle(journeyFile: String, title: String): Option[SourceRef] =
    readLines(journeyFile).flatMap { lines =>
      findLine(lines, s""""title": "$title"""").orElse(findLine(lines, title))
    }.map(SourceRef(journeyFile, _))

  /** Find the line number of a page's "type" field in the journey JSON. */
  def journeyPageType(journeyFile: String, pageType: String, title: String): Option[SourceRef] =
    readLines(journeyFile).flatMap { lines =>
      val titleIdx = lines.indexWhere(_.contains(s""""title": "$title""""))
      if (titleIdx < 0) None
      else {
        val searchStart = Math.max(0, titleIdx - 5)
        val window = lines.slice(searchStart, titleIdx + 1)
        window.lastIndexWhere(_.contains(s""""type":""")) match {
          case -1  => Some(titleIdx + 1)
          case rel => Some(searchStart + rel + 1)
        }
      }
    }.map(SourceRef(journeyFile, _))

  /** Resolve the actual HTML filename for a page index in the prototype dir. */
  def prototypeFile(protoDir: String, pageIndex: Int): Option[String] = {
    val dir = new File(protoDir)
    if (!dir.isDirectory) None
    else {
      val candidates = dir.listFiles()
        .filter(f => f.getName.startsWith(s"page-$pageIndex-") && f.getName.endsWith(".html"))
      candidates.headOption.map(_.getPath)
    }
  }

  /** Find the <h1> line in an HTML file. */
  def prototypeTitle(htmlFile: String): Option[SourceRef] =
    readLines(htmlFile).flatMap(findLine(_, "<h1")).map(SourceRef(htmlFile, _))

  /** Find a specific element line in an HTML file by searching for a text needle. */
  def prototypeElement(htmlFile: String, needle: String): Option[SourceRef] =
    readLines(htmlFile).flatMap(findLine(_, needle)).map(SourceRef(htmlFile, _))

  /** Find where a radio/checkbox option label appears in an HTML file. */
  def prototypeOption(htmlFile: String, optionLabel: String): Option[SourceRef] =
    readLines(htmlFile).flatMap(findLine(_, optionLabel)).map(SourceRef(htmlFile, _))

  /** Find a route's "title" line in the service JSON. */
  def serviceRouteTitle(serviceFile: String, title: String): Option[SourceRef] =
    readLines(serviceFile).flatMap { lines =>
      findLine(lines, s""""title": "$title"""").orElse(findLine(lines, title))
    }.map(SourceRef(serviceFile, _))

  /** Find a route's "pageType" line in the service JSON (near a given title). */
  def serviceRouteType(serviceFile: String, title: String): Option[SourceRef] =
    readLines(serviceFile).flatMap { lines =>
      val titleIdx = lines.indexWhere(_.contains(s""""title": "$title""""))
      if (titleIdx < 0) None
      else {
        val searchStart = Math.max(0, titleIdx - 5)
        val window = lines.slice(searchStart, titleIdx + 1)
        window.lastIndexWhere(_.contains(s""""pageType":""")) match {
          case -1  => Some(titleIdx + 1)
          case rel => Some(searchStart + rel + 1)
        }
      }
    }.map(SourceRef(serviceFile, _))

  /** Find a "nextRoutes" block near a route title in the service JSON. */
  def serviceNextRoutes(serviceFile: String, title: String): Option[SourceRef] =
    readLines(serviceFile).flatMap { lines =>
      val titleIdx = lines.indexWhere(_.contains(s""""title": "$title""""))
      if (titleIdx < 0) None
      else {
        val searchStart = titleIdx
        val searchEnd = Math.min(lines.size, titleIdx + 10)
        val window = lines.slice(searchStart, searchEnd)
        window.indexWhere(_.contains("\"nextRoutes\"")) match {
          case -1  => Some(titleIdx + 1)
          case rel => Some(searchStart + rel + 1)
        }
      }
    }.map(SourceRef(serviceFile, _))
}
