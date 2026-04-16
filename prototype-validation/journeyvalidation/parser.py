import json
from .model import Journey, Page, Question


def parse(json_string: str) -> Journey:
    data = json.loads(json_string)
    pages = []
    for p in data["pages"]:
        questions = [Question(q["questionTitle"]) for q in p.get("questions", [])]
        validation = p.get("validation")
        if isinstance(validation, str):
            validation = [validation]
        pages.append(
            Page(
                page_type=p["type"],
                title=p["title"],
                index=p["index"],
                options=p.get("options", []),
                questions=questions,
                validation=validation,
            )
        )
    return Journey(pages=pages)


def parse_file(path: str) -> Journey:
    with open(path, encoding="utf-8") as f:
        return parse(f.read())
