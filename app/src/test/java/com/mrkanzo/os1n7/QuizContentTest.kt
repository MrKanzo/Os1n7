package com.mrkanzo.os1n7

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regression checks for distinct, well-formed offline OSINT quizzes. */
class QuizContentTest {
    @Test fun allLessonsHaveDistinctAndValidQuestions() {
        val errors = LessonQuizBank.validate()
        assertTrue(errors.joinToString("\n"), errors.isEmpty())
        assertEquals(18, LessonLibrary.lessons.size)
        assertEquals(36, LessonLibrary.lessons.sumOf { LessonQuizBank.forLesson(it).size })
    }

    @Test fun answersDoNotAlwaysAppearInTheSamePosition() {
        val all = LessonLibrary.lessons.flatMap { LessonQuizBank.forLesson(it) }
        (0..3).forEach { index ->
            assertEquals("Position $index should be used evenly", 9, all.count { it.correctIndex == index })
        }
    }

    @Test fun lessonPracticesAreNotExactDuplicates() {
        val tasks = LessonLibrary.lessons.map { it.task.trim().lowercase() }
        val titles = LessonLibrary.lessons.map { it.title.trim().lowercase() }
        assertEquals(tasks.size, tasks.distinct().size)
        assertEquals(titles.size, titles.distinct().size)
    }

    @Test fun noOptionSetContainsDuplicateChoices() {
        val questions = LessonLibrary.lessons.flatMap { LessonQuizBank.forLesson(it) }
        questions.forEach { q ->
            assertEquals(q.prompt, 4, q.options.size)
            assertEquals(q.prompt, 4, q.options.map { it.trim().lowercase() }.distinct().size)
            assertTrue(q.prompt, q.explanation.isNotBlank())
        }
    }
}
