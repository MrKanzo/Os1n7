#!/usr/bin/env python3
"""Offline OS1N7 quiz audit. Standard library only, no Maven dependencies."""
from collections import Counter
from pathlib import Path
import re
import sys
import unicodedata

ROOT = Path(__file__).resolve().parents[1] / "app/src/main/java/com/mrkanzo/os1n7"


def read(name):
    return (ROOT / name).read_text(encoding="utf-8")


def strings(source):
    return re.findall(r'"((?:\\.|[^"\\])*)"', source)


def clean(text):
    text = unicodedata.normalize("NFKC", text).casefold()
    text = re.sub(r"^\s*\d+[.)]\s*", "", text)
    return re.sub(r"\s+", " ", re.sub(r"[^\w\s]", " ", text)).strip()


def require(condition, message):
    if not condition:
        raise AssertionError(message)


def main():
    library = read("LessonLibrary.kt")
    lessons = []
    for line in library.splitlines():
        if line.strip().startswith('Lesson("""'):
            row = re.findall(r'"""(.*?)"""', line)
            require(len(row) == 6, "Invalid lesson row")
            lessons.append(row)
    require(len(lessons) == 18, "Expected 18 lessons")
    require(len({clean(row[0]) for row in lessons}) == 18, "Duplicate lesson titles")
    require(len({clean(row[5]) for row in lessons}) == 18, "Duplicate practice tasks")

    bank = read("LessonQuizBank.kt")
    headings = re.findall(r'^\s*"([^"]+)" to listOf\(', bank, re.M)
    require(len(headings) == 18, "Missing lesson quiz sections")
    require(set(headings) == {row[0] for row in lessons}, "Quiz / lesson names mismatch")

    pattern = (r'Question\(\s*"([^"]+)",\s*listOf\((.*?)\),\s*'
               r'(\d+),\s*"([^"]+)"\s*\)')
    entries = re.findall(pattern, bank, re.S)
    require(len(entries) == 36, f"Expected 36 lesson questions, got {len(entries)}")
    stems = []
    distribution = Counter()
    for question, values, correct, why in entries:
        choices = strings(values)
        require(len(choices) == 4, "Missing options: " + question)
        require(len(set(map(clean, choices))) == 4, "Duplicate options: " + question)
        require(int(correct) in range(4), "Invalid key: " + question)
        require(bool(why.strip()), "No explanation: " + question)
        distribution[int(correct)] += 1
        stems.append(question)
    require(distribution == Counter({0: 9, 1: 9, 2: 9, 3: 9}),
            "Answer positions are not balanced")

    academy = read("AcademyActivity.kt")
    academy_entries = re.findall(
        r'MiniQuiz\(\s*"([^"]+)",\s*listOf\((.*?)\),\s*(\d+),\s*"([^"]+)"\s*\)',
        academy, re.S,
    )
    require(len(academy_entries) == 3, "Academy does not have three distinct quizzes")
    for question, values, correct, why in academy_entries:
        options = strings(values)
        require(len(options) == 3, "Academy wrong number of options")
        require(len(set(map(clean, options))) == 3, "Academy duplicate options")
        require(int(correct) in range(3) and bool(why), "Academy invalid answer")
        stems.append(question)
    require("quiz.checkedRadioButtonId!=200+quizData.correct" in academy,
            "Academy grading must use topic-specific key")

    foundations = read("FoundationsActivity.kt")
    section = re.search(r'private val questions=listOf\((.*?)\n \)', foundations, re.S)
    require(section is not None, "Foundations questions missing")
    foundation_questions = strings(section.group(1))
    require(len(foundation_questions) == 7, "Foundations need 7 stage questions")
    stems.extend(foundation_questions)
    found = re.search(r'correctPositions=listOf\(([^)]+)\)', foundations)
    require(found is not None, "Foundations answer order missing")
    positions = [int(x.strip()) for x in found.group(1).split(",")]
    require(len(positions) == 7 and set(positions) == {0, 1, 2},
            "Foundations answers lack varied positions")
    require("choices.add(correctPosition,answers[i][0])" in foundations,
            "Foundations choice placement changed")
    require("group.checkedRadioButtonId!=100+correctPosition" in foundations,
            "Foundations grading key changed")

    mirror = read("EvidenceMirror.kt")
    checks = re.findall(
        r'Check\(\s*"([^"]+)",\s*listOf\((.*?)\),\s*(\d+),\s*"([^"]+)"\s*\)',
        mirror, re.S,
    )
    require(len(checks) == 4, "Evidence Mirror needs four questions")
    for question, values, correct, why in checks:
        options = strings(values)
        require(len(options) == 2 and len(set(map(clean, options))) == 2,
                "Mirror repeated or missing options")
        require(int(correct) in (0, 1) and bool(why), "Mirror invalid answer")
        stems.append(question)

    normalized = list(map(clean, stems))
    require(len(stems) == 50, f"Expected 50 questions, found {len(stems)}")
    require(len(set(normalized)) == len(normalized), "Duplicate quiz question")

    category = read("CategoryLibraryActivity.kt")
    require("if (completed && !replay)" in category,
            "Completed quiz should not auto-repeat")
    require("if (correct && last && !completed)" in category,
            "Quiz should award progress only after final question")
    require("quiz:" in category, "Existing completion keys missing")
    print("OS1N7 quiz QA PASS: 18 unique lessons / tasks, 50 distinct questions")
    print("Answer positions in 36 lesson questions: 9 / 9 / 9 / 9")
    print("Old completion keys retained; replay opt-in and no repeated XP")


if __name__ == "__main__":
    try:
        main()
    except (AssertionError, OSError, ValueError) as e:
        print("OS1N7 quiz QA FAILED:", e, file=sys.stderr)
        sys.exit(1)
