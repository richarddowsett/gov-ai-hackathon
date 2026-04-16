from dataclasses import dataclass, field
from typing import Dict, List, Optional, Union


@dataclass
class Question:
    question_title: str


@dataclass
class Page:
    page_type: str
    title: str
    index: Union[int, Dict[str, int]]
    options: List[str] = field(default_factory=list)
    questions: List[Question] = field(default_factory=list)
    validation: Optional[List[str]] = None

    @property
    def is_branching(self) -> bool:
        return isinstance(self.index, dict)


@dataclass
class Journey:
    pages: List[Page]

    @property
    def page_count(self) -> int:
        return len(self.pages)


@dataclass
class Pass:
    check: str
    is_pass: bool = field(default=True, init=False)


@dataclass
class Fail:
    check: str
    expected: str
    actual: str
    is_pass: bool = field(default=False, init=False)


@dataclass
class ValidationReport:
    title: str
    results: list

    @property
    def all_passed(self) -> bool:
        return all(r.is_pass for r in self.results)

    @property
    def passes(self) -> list:
        return [r for r in self.results if r.is_pass]

    @property
    def failures(self) -> list:
        return [r for r in self.results if not r.is_pass]

    def render(self) -> str:
        width = 72
        bar = "=" * width
        thin_bar = "-" * width

        lines = [f"+{bar}+", f"|  {self.title:<{width - 2}}|", f"+{bar}+"]

        for r in self.results:
            if r.is_pass:
                lines.append(f"  [PASS]  {r.check}")
            else:
                lines.append(f"  [FAIL]  {r.check}")
                lines.append(f"        expected : {r.expected}")
                lines.append(f"        actual   : {r.actual}")

        pass_count = len(self.passes)
        fail_count = len(self.failures)
        total = len(self.results)
        status = "ALL CHECKS PASSED" if self.all_passed else f"{fail_count} of {total} FAILED"

        lines.append(f"  {thin_bar}")
        lines.append(f"  {status}  ({pass_count} passed, {fail_count} failed, {total} total)")

        return "\n".join(lines)
