package com.example.aichallengeapp.feature.chat.domain.usecase

import com.example.aichallengeapp.core.database.domain.model.Constraint

class ValidateConstraintsUseCase {
    operator fun invoke(response: String, constraints: List<Constraint>): List<Constraint> {
        return constraints.filter { constraint ->
            val regex = runCatching { Regex(constraint.regexPattern) }.getOrNull() ?: return@filter false
            val matches = regex.containsMatchIn(response)
            if (constraint.matchMeansViolation) matches else !matches
        }
    }
}
