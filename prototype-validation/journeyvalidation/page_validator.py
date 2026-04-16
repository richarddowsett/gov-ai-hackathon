from bs4 import BeautifulSoup

from .model import Page, Pass, Fail


def validate_html(html: str, page: Page, label: str) -> list:
    soup = BeautifulSoup(html, "html.parser")
    results = [_validate_title(soup, page, label)]
    results.extend(_validate_elements(soup, page, label))
    return results


def _validate_title(soup, page: Page, label: str):
    h1 = soup.find("h1")
    if not h1:
        return Fail(f"{label} — title", page.title, "<no h1 found>")
    actual = h1.get_text(strip=True)
    if actual == page.title:
        return Pass(f"{label} — title matches")
    return Fail(f"{label} — title", f"'{page.title}'", f"'{actual}'")


def _validate_elements(soup, page: Page, label: str) -> list:
    validators = {
        "contentPage": _validate_content,
        "string": _validate_string,
        "datePage": _validate_date,
        "boolean": _validate_boolean,
        "radioButton": _validate_radio,
        "checkbox": _validate_checkbox,
        "multipleQuestionsPage": _validate_multiple_questions,
    }
    validator = validators.get(page.page_type)
    if validator is None:
        return [Fail(f"{label} — page type", "known page type", f"'{page.page_type}'")]
    return validator(soup, page, label)


def _validate_content(_soup, _page, label):
    return [Pass(f"{label} — content page (no form required)")]


def _validate_string(soup, _page, label):
    found = len(soup.find_all("input", {"type": "text"}))
    if found >= 1:
        return [Pass(f"{label} — has text input")]
    return [Fail(f"{label} — text input", "at least 1 <input type=text>", f"found {found}")]


def _validate_date(soup, _page, label):
    by_name = soup.find_all("input", attrs={"name": lambda n: n and any(
        k in n for k in ("day", "month", "year")
    )})
    by_type = soup.find_all("input", {"type": "date"})
    by_class = soup.find_all(class_="govuk-date-input")
    found = len(by_name) + len(by_type) + len(by_class)
    if found >= 1:
        return [Pass(f"{label} — has date fields")]
    return [Fail(f"{label} — date fields", "date input elements", f"found {found}")]


def _validate_boolean(soup, _page, label):
    found = len(soup.find_all("input", {"type": "radio"}))
    if found >= 2:
        return [Pass(f"{label} — has yes/no radios ({found} found)")]
    return [Fail(f"{label} — boolean radios", "at least 2 radio buttons", f"found {found}")]


def _validate_radio(soup, page: Page, label):
    radios = len(soup.find_all("input", {"type": "radio"}))
    label_texts = {lbl.get_text(strip=True) for lbl in soup.find_all("label")}

    results = []
    if radios >= len(page.options):
        results.append(Pass(f"{label} — radio count ({radios} found)"))
    else:
        results.append(
            Fail(f"{label} — radio count", f"{len(page.options)} radio buttons", f"found {radios}")
        )

    for opt in page.options:
        if any(opt in lt for lt in label_texts):
            results.append(Pass(f"{label} — option '{opt}' present"))
        else:
            results.append(Fail(f"{label} — option '{opt}'", f"label containing '{opt}'", "not found"))

    return results


def _validate_checkbox(soup, page: Page, label):
    found = len(soup.find_all("input", {"type": "checkbox"}))
    label_texts = {lbl.get_text(strip=True) for lbl in soup.find_all("label")}

    results = []
    if found >= len(page.options):
        results.append(Pass(f"{label} — checkbox count ({found} found)"))
    else:
        results.append(
            Fail(f"{label} — checkbox count", f"{len(page.options)} checkboxes", f"found {found}")
        )

    for opt in page.options:
        if any(opt in lt for lt in label_texts):
            results.append(Pass(f"{label} — option '{opt}' present"))
        else:
            results.append(Fail(f"{label} — option '{opt}'", f"label containing '{opt}'", "not found"))

    return results


def _validate_multiple_questions(soup, page: Page, label):
    all_text_elements = soup.find_all(["label", "legend"])
    all_text = {el.get_text(strip=True) for el in all_text_elements}

    results = []
    for q in page.questions:
        if any(q.question_title in t for t in all_text):
            results.append(Pass(f"{label} — question '{q.question_title}' present"))
        else:
            results.append(
                Fail(f"{label} — question", q.question_title, "not found on page")
            )
    return results
