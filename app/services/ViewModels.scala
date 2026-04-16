package services

final case class PrototypePageView(
    title: String,
    fields: Seq[(String, String)],
    transitions: Seq[(String, String)]
)

final case class ServicePageView(
    id: String,
    title: String,
    transitions: Seq[(String, String)]
)
