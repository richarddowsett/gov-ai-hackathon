package contract

/** Central configuration for the contract validator test suites.
  *
  * All paths are configurable via system properties so you can point the
  * validator at any journey / prototype / service without changing code:
  *
  *   sbt \
  *     -Djourney.json=path/to/journey.json \
  *     -Dprototype.dir=path/to/prototype \
  *     -Dservice.json=path/to/routes.json \
  *     test
  *
  * Defaults point to the example journey shipped with this repo.
  */
object ValidatorConfig {
  val journeyPath:   String = sys.props.getOrElse("journey.json",   "example/journey.json")
  val prototypeDir:  String = sys.props.getOrElse("prototype.dir",  "prototype")
  val servicePath:   String = sys.props.getOrElse("service.json",   "service/routes.json")
}
