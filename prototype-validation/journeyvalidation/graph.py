from dataclasses import dataclass, field
from typing import List, Optional

from .model import Journey, Page


@dataclass
class PathStep:
    array_index: int
    page: Page
    selected_answer: Optional[str] = None


@dataclass
class JourneyPath:
    steps: List[PathStep] = field(default_factory=list)

    @property
    def description(self) -> str:
        choices = " -> ".join(
            s.selected_answer for s in self.steps if s.selected_answer
        )
        first = self.steps[0].page.title if self.steps else "(empty)"
        last = self.steps[-1].page.title if self.steps else "(empty)"
        suffix = f" [choices: {choices}]" if choices else ""
        return f"{first} ... {last}{suffix}"


def enumerate_all_paths(journey: Journey) -> List[JourneyPath]:
    if not journey.pages:
        return []

    def walk(current_index: int, visited: frozenset, acc: list):
        if current_index < 0 or current_index >= journey.page_count:
            return [JourneyPath(steps=list(reversed(acc)))]
        if current_index in visited:
            return [JourneyPath(steps=list(reversed(acc)))]

        page = journey.pages[current_index]
        new_visited = visited | {current_index}

        if isinstance(page.index, dict):
            paths = []
            for answer, next_index in page.index.items():
                step = PathStep(current_index, page, selected_answer=answer)
                paths.extend(walk(next_index, new_visited, [step] + acc))
            return paths
        else:
            step = PathStep(current_index, page)
            return walk(page.index, new_visited, [step] + acc)

    return walk(0, frozenset(), [])


def unique_paths(journey: Journey) -> List[JourneyPath]:
    all_paths = enumerate_all_paths(journey)
    seen = {}
    for path in all_paths:
        key = tuple(s.array_index for s in path.steps)
        if key not in seen:
            seen[key] = path
    return sorted(seen.values(), key=lambda p: [s.array_index for s in p.steps])
