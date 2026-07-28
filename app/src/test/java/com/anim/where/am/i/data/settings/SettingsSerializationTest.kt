package com.anim.where.am.i.data.settings

import com.anim.where.am.i.domain.model.Accuracy
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsSerializationTest {
    @Test fun accuracyKeyRoundTrips() {
        for (a in Accuracy.entries) {
            assertEquals(a, accuracyFromKey(accuracyToKey(a)))
        }
    }

    @Test fun unknownAccuracyDefaultsToMedium() {
        assertEquals(Accuracy.MEDIUM, accuracyFromKey("bogus"))
        assertEquals(Accuracy.MEDIUM, accuracyFromKey(null))
    }
}
