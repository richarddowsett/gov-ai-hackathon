# JSON Creator Only: Run Instructions

This branch (`feature/json-creator-only`) runs a standalone browser app.
It does **not** run with Play Framework and does **not** need `sbt run`.

## Run Everything With Docker Compose

```bash
cd /Users/nived/code/hackathon/gov-ai-hackathon
docker compose up -d
# or:
./scripts/docker-up.sh
```

Open:

- JSON Creator: http://127.0.0.1:8787/
- Prototype app: http://127.0.0.1:4000/

Postgres is included for persistence work:

- Host: `127.0.0.1`
- Port: `5433` (default on host; container is still `5432`)
- Database: `journey`
- User: `journey_user`
- Password: `journey_pass`

Use a different host port if needed:

```bash
POSTGRES_PORT=55432 docker compose up -d
```

Stop everything:

```bash
docker compose down
# or:
./scripts/docker-down.sh
```

## Start

```bash
cd /Users/nived/code/hackathon/gov-ai-hackathon
./scripts/start-json-creator.sh
```

Open:

- http://127.0.0.1:8787/

## Quick Check (optional)

In a second terminal:

```bash
curl -I http://127.0.0.1:8787/
curl -I http://127.0.0.1:8787/app.js
curl -I http://127.0.0.1:8787/journey.schema.json
```

All should return `HTTP/1.0 200 OK`.

## If You See `application.conf resource not found`

You started a Play/SBT command by mistake (for example `sbt run`).
Stop it and use the startup script above.
