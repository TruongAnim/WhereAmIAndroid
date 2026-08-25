package com.anim.where.am.i.domain.model

enum class Accuracy { HIGHEST, HIGH, MEDIUM, LOW }

/**
 * Roughly how far a fix at this setting can be wrong, in metres.
 *
 * Used to warn when the distance filter is set below the error of the very
 * measurement it filters on. At that point the receiver's own wandering
 * clears the threshold and a phone lying on a desk reports a journey.
 */
val Accuracy.typicalErrorMeters: Int
    get() = when (this) {
        Accuracy.HIGHEST -> 10
        Accuracy.HIGH -> 15
        Accuracy.MEDIUM -> 60
        Accuracy.LOW -> 500
    }
