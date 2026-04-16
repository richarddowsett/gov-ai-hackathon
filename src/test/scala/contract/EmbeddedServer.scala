package contract

import com.sun.net.httpserver.{HttpExchange, HttpServer}
import java.io.File
import java.net.InetSocketAddress
import scala.io.Source
import scala.util.Using

/** A lightweight HTTP server that serves prototype HTML pages from a directory.
  *
  * Used during tests so that the prototype validator can fetch pages over HTTP
  * just like it would against a real running prototype.
  *
  * Pages are mapped by array index:
  *   GET /page/0 → page-0-welcome.html
  *   GET /page/3 → page-3-license.html
  *   etc.
  */
class EmbeddedServer(prototypeDir: String, port: Int = 0) {
  private var server: HttpServer = _
  private var assignedPort: Int  = 0

  def start(): Int = {
    server = HttpServer.create(new InetSocketAddress("localhost", port), 0)

    server.createContext("/page/", (exchange: HttpExchange) => {
      val path  = exchange.getRequestURI.getPath
      val index = path.stripPrefix("/page/").takeWhile(_.isDigit)

      val dir   = new File(prototypeDir)
      val files = dir.listFiles().filter(f => f.getName.startsWith(s"page-$index-") && f.getName.endsWith(".html"))

      val (code, body) = files.headOption match {
        case Some(file) =>
          Using(Source.fromFile(file))(_.mkString) match {
            case scala.util.Success(content) => (200, content)
            case scala.util.Failure(_)       => (500, "Error reading file")
          }
        case None =>
          (404, s"No prototype page found for index $index")
      }

      val bytes = body.getBytes("UTF-8")
      exchange.getResponseHeaders.set("Content-Type", "text/html; charset=UTF-8")
      exchange.sendResponseHeaders(code, bytes.length)
      exchange.getResponseBody.write(bytes)
      exchange.getResponseBody.close()
    })

    server.createContext("/health", (exchange: HttpExchange) => {
      val body  = "OK"
      val bytes = body.getBytes("UTF-8")
      exchange.sendResponseHeaders(200, bytes.length)
      exchange.getResponseBody.write(bytes)
      exchange.getResponseBody.close()
    })

    server.setExecutor(null)
    server.start()
    assignedPort = server.getAddress.getPort
    assignedPort
  }

  def baseUrl: String = s"http://localhost:$assignedPort"

  def stop(): Unit =
    if (server != null) server.stop(0)
}
