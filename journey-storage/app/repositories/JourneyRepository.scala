package repositories

import models.Journey
import play.api.db.Database

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JourneyRepository @Inject()(db: Database)(implicit ec: ExecutionContext) {

  def list(): Future[Seq[Journey]] = Future {
    db.withConnection { conn =>
      val stmt = conn.prepareStatement("SELECT service_name, json FROM journeys ORDER BY service_name")
      val rs   = stmt.executeQuery()
      val buf  = scala.collection.mutable.ListBuffer.empty[Journey]
      while (rs.next()) buf += Journey(rs.getString("service_name"), rs.getString("json"))
      rs.close(); stmt.close()
      buf.toSeq
    }
  }

  def get(serviceName: String): Future[Option[Journey]] = Future {
    db.withConnection { conn =>
      val stmt = conn.prepareStatement("SELECT service_name, json FROM journeys WHERE service_name = ?")
      stmt.setString(1, serviceName)
      val rs     = stmt.executeQuery()
      val result = if (rs.next()) Some(Journey(rs.getString("service_name"), rs.getString("json"))) else None
      rs.close(); stmt.close()
      result
    }
  }

  def create(journey: Journey): Future[Journey] = Future {
    db.withConnection { conn =>
      val stmt = conn.prepareStatement("INSERT INTO journeys (service_name, json) VALUES (?, ?)")
      stmt.setString(1, journey.serviceName)
      stmt.setString(2, journey.json)
      stmt.executeUpdate()
      stmt.close()
      journey
    }
  }

  def update(serviceName: String, journey: Journey): Future[Option[Journey]] = Future {
    db.withConnection { conn =>
      val stmt = conn.prepareStatement("UPDATE journeys SET json = ? WHERE service_name = ?")
      stmt.setString(1, journey.json)
      stmt.setString(2, serviceName)
      val rows = stmt.executeUpdate()
      stmt.close()
      if (rows > 0) Some(journey.copy(serviceName = serviceName)) else None
    }
  }

  def delete(serviceName: String): Future[Boolean] = Future {
    db.withConnection { conn =>
      val stmt = conn.prepareStatement("DELETE FROM journeys WHERE service_name = ?")
      stmt.setString(1, serviceName)
      val rows = stmt.executeUpdate()
      stmt.close()
      rows > 0
    }
  }
}
