package contract

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import org.jsoup.Jsoup

import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.util.concurrent.CountDownLatch

object BrowserServer {
  private val Host = "127.0.0.1"
  private val Port = 8080
  private val PrototypeDir = Paths.get("prototype").toAbsolutePath.normalize()
  private val ServicePath = Paths.get("service/routes.json").toAbsolutePath.normalize()

  private val GovUkCss = "https://cdn.jsdelivr.net/npm/govuk-frontend@5.7.1/dist/govuk/govuk-frontend.min.css"
  private val GovUkJs = "https://cdn.jsdelivr.net/npm/govuk-frontend@5.7.1/dist/govuk/govuk-frontend.min.js"

  private final case class ServicePage(id: String, title: String, next: Map[String, String])

  def main(args: Array[String]): Unit = {
    val server = HttpServer.create(new InetSocketAddress(Host, Port), 0)
    server.createContext("/", new RootHandler)
    server.createContext("/prototype", new PrototypeHandler)
    server.createContext("/service", new ServiceHandler)
    server.setExecutor(null)
    server.start()
    Runtime.getRuntime.addShutdownHook(new Thread(() => server.stop(0)))
    println(s"Browser demo running at http://$Host:$Port")
    println(s"- Prototype: http://$Host:$Port/prototype/page-1.html")
    println(s"- Service view: http://$Host:$Port/service")
    println("Press Ctrl+C to stop.")
    new CountDownLatch(1).await()
  }

  private def send(exchange: HttpExchange, status: Int, body: String, contentType: String): Unit = {
    val bytes = body.getBytes(StandardCharsets.UTF_8)
    exchange.getResponseHeaders.add("Content-Type", contentType)
    exchange.sendResponseHeaders(status, bytes.length)
    val os = exchange.getResponseBody
    try os.write(bytes)
    finally os.close()
  }

  private def renderLayout(title: String, content: String): String = {
    s"""<!doctype html>
       |<html lang="en" class="govuk-template">
       |  <head>
       |    <meta charset="UTF-8" />
       |    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
       |    <title>${escape(title)} - GOV.UK</title>
       |    <link rel="stylesheet" href="$GovUkCss" />
       |    <style>
       |      .hmrc-header-tag { font-size: 16px; margin-left: 8px; color: #dbe8f1; }
       |      .journey-links a { margin-right: 12px; }
       |      .transition-list { margin-top: 20px; }
       |      .transition-list li { margin-bottom: 10px; }
       |      .service-table code { white-space: nowrap; }
       |    </style>
       |  </head>
       |  <body class="govuk-template__body">
       |    <header class="govuk-header" role="banner" data-module="govuk-header">
       |      <div class="govuk-header__container govuk-width-container">
       |        <div class="govuk-header__content">
       |          <a href="/" class="govuk-header__link govuk-header__link--service-name">
       |            AI Journey Contract Validator
       |          </a>
       |          <span class="hmrc-header-tag">HMRC-style prototype shell</span>
       |        </div>
       |      </div>
       |    </header>
       |    <div class="govuk-width-container">
       |      <main class="govuk-main-wrapper" id="main-content" role="main">
       |        $content
       |      </main>
       |    </div>
       |    <script src="$GovUkJs"></script>
       |    <script>window.GOVUKFrontend && window.GOVUKFrontend.initAll && window.GOVUKFrontend.initAll();</script>
       |  </body>
       |</html>
       |""".stripMargin
  }

  private final class RootHandler extends HttpHandler {
    override def handle(exchange: HttpExchange): Unit = {
      val content =
        """<h1 class="govuk-heading-xl">Journey Browser Demo</h1>
          |<p class="govuk-body">View both layers in a GOV.UK-style interface.</p>
          |<div class="journey-links">
          |  <a class="govuk-link" href="/prototype/page-1.html">Open prototype journey</a>
          |  <a class="govuk-link" href="/service">Open service flow map</a>
          |</div>
          |<div class="govuk-inset-text" style="margin-top: 24px;">
          |  This is a lightweight HMRC/GOV.UK-style frontend shell over your existing prototype and service contract data.
          |</div>
          |""".stripMargin
      send(exchange, 200, renderLayout("Journey Browser Demo", content), "text/html; charset=utf-8")
    }
  }

  private final class PrototypeHandler extends HttpHandler {
    override def handle(exchange: HttpExchange): Unit = {
      val requestPath = Option(exchange.getRequestURI.getPath).getOrElse("/prototype")
      val suffix = requestPath.stripPrefix("/prototype").stripPrefix("/")
      val fileName = if (suffix.isBlank) "page-1.html" else suffix
      val candidate = PrototypeDir.resolve(fileName).normalize()

      if (!candidate.startsWith(PrototypeDir) || !Files.exists(candidate) || Files.isDirectory(candidate)) {
        send(exchange, 404, renderLayout("Not Found", s"<h1 class=\"govuk-heading-l\">Prototype file not found: ${escape(fileName)}</h1>"), "text/html; charset=utf-8")
        return
      }

      if (!fileName.endsWith(".html")) {
        send(exchange, 200, Files.readString(candidate), "text/plain; charset=utf-8")
        return
      }

      val doc = Jsoup.parse(Files.readString(candidate))
      val pageTitle = Option(doc.selectFirst("h1")).map(_.text().trim).getOrElse(fileName)
      val main = Option(doc.selectFirst("main"))

      val fieldsHtml = doc.select("input[name], textarea[name], select[name]").toArray.toSeq
        .map(_.asInstanceOf[org.jsoup.nodes.Element])
        .map { el =>
          val fieldName = Option(el.attr("name")).filter(_.nonEmpty).getOrElse("field")
          val fieldType = Option(el.attr("type")).filter(_.nonEmpty).getOrElse(el.tagName())
          s"""<div class="govuk-form-group">
             |  <label class="govuk-label" for="$fieldName">${escape(fieldName)}</label>
             |  <input class="govuk-input" id="$fieldName" name="$fieldName" type="${escape(fieldType)}" />
             |</div>
             |""".stripMargin
        }
        .mkString("\n")

      val transitions = main.map { m =>
        m.attributes().asList().toArray.toSeq
          .map(_.asInstanceOf[org.jsoup.nodes.Attribute])
          .filter(a => a.getKey.startsWith("data-next") && a.hasDeclaredValue)
          .map { a =>
            val key = if (a.getKey == "data-next") "Continue" else a.getKey.stripPrefix("data-next-").replace('-', ' ')
            key -> a.getValue
          }
      }.getOrElse(Seq.empty)

      val transitionHtml =
        if (transitions.isEmpty) {
          "<p class=\"govuk-body\">No transitions defined on this page.</p>"
        } else {
          transitions.map { case (key, target) =>
            val href = if (target == "END") "/service" else s"/prototype/$target.html"
            s"<li><a class=\"govuk-button govuk-button--secondary\" href=\"$href\">${escape(key)} → ${escape(target)}</a></li>"
          }.mkString("<ul class=\"transition-list govuk-list\">", "", "</ul>")
        }

      val content =
        s"""<a href="/" class="govuk-back-link">Back</a>
           |<h1 class="govuk-heading-l">${escape(pageTitle)}</h1>
           |<form>
           |  $fieldsHtml
           |</form>
           |$transitionHtml
           |""".stripMargin

      send(exchange, 200, renderLayout(pageTitle, content), "text/html; charset=utf-8")
    }
  }

  private final class ServiceHandler extends HttpHandler {
    override def handle(exchange: HttpExchange): Unit = {
      val pages = loadServicePages()
      val rows = pages
        .map { p =>
          val next = p.next.toSeq.sortBy(_._1).map { case (k, v) => s"<code>${escape(k)}</code> → <code>${escape(v)}</code>" }.mkString("<br/>")
          s"<tr><td>${escape(p.id)}</td><td>${escape(p.title)}</td><td>$next</td></tr>"
        }
        .mkString("\n")

      val content =
        s"""<a href="/" class="govuk-back-link">Back</a>
           |<h1 class="govuk-heading-l">Service Flow View</h1>
           |<p class="govuk-body">Source: <code>service/routes.json</code></p>
           |<table class="govuk-table service-table">
           |  <thead class="govuk-table__head">
           |    <tr class="govuk-table__row">
           |      <th class="govuk-table__header">Page Id</th>
           |      <th class="govuk-table__header">Title</th>
           |      <th class="govuk-table__header">Transitions</th>
           |    </tr>
           |  </thead>
           |  <tbody class="govuk-table__body">
           |    $rows
           |  </tbody>
           |</table>
           |""".stripMargin

      send(exchange, 200, renderLayout("Service Flow View", content), "text/html; charset=utf-8")
    }

    private def loadServicePages(): Seq[ServicePage] = {
      val root = ujson.read(Files.readString(ServicePath))
      root("pages").arr.toSeq.map { page =>
        val next = page("next").obj.toSeq.map { case (k, v) => k -> v.str }.toMap
        ServicePage(
          id = page("id").str,
          title = page("title").str,
          next = next
        )
      }
    }
  }

  private def escape(value: String): String = {
    value
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
  }
}
