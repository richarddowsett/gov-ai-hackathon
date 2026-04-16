# --- !Ups

CREATE TABLE journeys (
  service_name VARCHAR(255) PRIMARY KEY,
  json         TEXT NOT NULL
);

# --- !Downs

DROP TABLE journeys;
