package contract

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.{JsonSchemaFactory, SpecVersion}

import java.nio.file.{Files, Path, Paths}
import scala.jdk.CollectionConverters._

object SchemaValidator {
  private val mapper = new ObjectMapper()
  private val defaultSchemaPath = Paths.get("schema/journey.schema.json")

  def validateJourneyFile(journeyPath: Path, schemaPath: Path = defaultSchemaPath): Unit = {
    val schemaStream = Files.newInputStream(schemaPath)
    val journeyStream = Files.newInputStream(journeyPath)
    try {
      val factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
      val schema = factory.getSchema(schemaStream)
      val documentNode = mapper.readTree(journeyStream)
      val errors = schema.validate(documentNode).asScala.toSeq

      if (errors.nonEmpty) {
        val details = errors
          .map(err => s"${err.getInstanceLocation}: ${err.getMessage}")
          .sorted
          .mkString("; ")
        throw new IllegalArgumentException(s"Journey JSON failed schema validation: $details")
      }
    } finally {
      journeyStream.close()
      schemaStream.close()
    }
  }
}
