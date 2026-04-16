package services

import org.jsoup.Jsoup

import java.nio.file.{Files, Path, Paths}
import javax.inject.Singleton
import scala.jdk.CollectionConverters._

@Singleton
class BrowserViewService {
  private val prototypeDir = Paths.get("prototype").toAbsolutePath.normalize()
  private val servicePath = Paths.get("service/routes.json").toAbsolutePath.normalize()

  def loadPrototypePage(page: String): Option[PrototypePageView] = {
    val safeName = if (page.endsWith(".html")) page else s"$page.html"
    val candidate = prototypeDir.resolve(safeName).normalize()
    if (!candidate.startsWith(prototypeDir) || !Files.exists(candidate) || Files.isDirectory(candidate)) {
      return None
    }

    val doc = Jsoup.parse(Files.readString(candidate))
    val title = Option(doc.selectFirst("h1")).map(_.text().trim).getOrElse(safeName)

    val fields = doc.select("input[name], textarea[name], select[name]").asScala.toSeq.map { el =>
      val fieldName = Option(el.attr("name")).filter(_.nonEmpty).getOrElse("field")
      val fieldType = Option(el.attr("type")).filter(_.nonEmpty).getOrElse(el.tagName())
      fieldName -> fieldType
    }

    val transitions = Option(doc.selectFirst("main")).toSeq.flatMap { m =>
      m.attributes().asList().asScala.toSeq
        .filter(a => a.getKey.startsWith("data-next") && a.hasDeclaredValue)
        .map { a =>
          val key = if (a.getKey == "data-next") "Continue" else a.getKey.stripPrefix("data-next-").replace('-', ' ')
          key -> a.getValue
        }
    }

    Some(PrototypePageView(title, fields, transitions))
  }

  def loadServicePages(): Seq[ServicePageView] = {
    if (!Files.exists(servicePath)) return Seq.empty

    val root = ujson.read(Files.readString(servicePath))
    root("pages").arr.toSeq.map { page =>
      val transitions = page("next").obj.toSeq.sortBy(_._1).map { case (k, v) => k -> v.str }
      ServicePageView(
        id = page("id").str,
        title = page("title").str,
        transitions = transitions
      )
    }
  }
}
