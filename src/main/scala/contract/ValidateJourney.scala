package contract

object ValidateJourney {

  private def printResult(result: ValidationResult): Unit = {
    if (result.ok) {
      println(s"✅ ${result.target} validation passed")
    } else {
      println(s"❌ ${result.target} drift detected")
      result.issues.foreach { issue =>
        println(
          s"- page=${issue.pageId} kind=${issue.kind} expected='${issue.expected}' actual='${issue.actual}'"
        )
      }
    }
  }

  def main(args: Array[String]): Unit = {
    val mode = args.headOption.getOrElse("all")
    val contract =
      try {
        ContractLoader.loadJourneyContract()
      } catch {
        case ex: Exception =>
          System.err.println(s"Contract loading failed: ${ex.getMessage}")
          System.exit(1)
          return
      }

    val results = mode match {
      case "prototype" => Seq(PrototypeValidator.validate(contract))
      case "service"   => Seq(ServiceValidator.validate(contract))
      case "all"       => Seq(PrototypeValidator.validate(contract), ServiceValidator.validate(contract))
      case _ =>
        System.err.println("Usage: sbt \"runMain contract.ValidateJourney [prototype|service|all]\"")
        System.exit(1)
        Seq.empty
    }

    results.foreach(printResult)

    if (!results.forall(_.ok)) {
      System.exit(1)
    }
  }
}
