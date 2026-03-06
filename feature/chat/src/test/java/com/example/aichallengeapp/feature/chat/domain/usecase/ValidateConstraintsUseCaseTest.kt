package com.example.aichallengeapp.feature.chat.domain.usecase

import com.example.aichallengeapp.core.database.domain.model.Constraint
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class ValidateConstraintsUseCaseTest : FunSpec({

    val useCase = ValidateConstraintsUseCase()

    test("returns empty list when no constraints") {
        useCase("any response", emptyList()).shouldBeEmpty()
    }

    test("returns empty list when response does not violate constraints") {
        val constraints = listOf(
            Constraint(
                name = "No profanity",
                description = "Must not contain bad words",
                regexPattern = "badword",
                matchMeansViolation = true
            )
        )
        useCase("This is a clean response", constraints).shouldBeEmpty()
    }

    test("detects violation when match means violation and pattern matches") {
        val constraint = Constraint(
            name = "No numbers",
            description = "Must not contain numbers",
            regexPattern = "\\d+",
            matchMeansViolation = true
        )
        val violations = useCase("Response with 123 numbers", listOf(constraint))
        violations.shouldHaveSize(1)
        violations.shouldContainExactly(constraint)
    }

    test("detects violation when match does NOT mean violation and pattern does not match") {
        val constraint = Constraint(
            name = "Must greet",
            description = "Must contain greeting",
            regexPattern = "hello|hi|привет",
            matchMeansViolation = false
        )
        val violations = useCase("Response without greeting", listOf(constraint))
        violations.shouldHaveSize(1)
    }

    test("no violation when matchMeansViolation=false and pattern matches") {
        val constraint = Constraint(
            name = "Must greet",
            description = "Must contain greeting",
            regexPattern = "hello|hi",
            matchMeansViolation = false
        )
        useCase("hello world", listOf(constraint)).shouldBeEmpty()
    }

    test("skips constraint with invalid regex pattern") {
        val constraint = Constraint(
            name = "Bad regex",
            description = "Invalid pattern",
            regexPattern = "[invalid",
            matchMeansViolation = true
        )
        useCase("any response", listOf(constraint)).shouldBeEmpty()
    }

    test("handles multiple constraints with mixed results") {
        val constraints = listOf(
            Constraint("No English", "No english", "\\b[a-zA-Z]+\\b", matchMeansViolation = true),
            Constraint("Has content", "Not empty", ".+", matchMeansViolation = false)
        )
        val violations = useCase("Hello мир", constraints)
        violations.shouldHaveSize(1)
        violations.first().name shouldBe "No English"
    }
})
