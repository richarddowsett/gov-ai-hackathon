# JSON Creator Only: Run Instructions

This branch (`feature/json-creator-only`) runs a standalone browser app.
It does **not** run with Play Framework and does **not** need `sbt run`.

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
