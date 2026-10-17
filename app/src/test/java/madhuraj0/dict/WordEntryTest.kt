package madhuraj0.dict

import madhuraj0.dict.data.model.WordEntry
import madhuraj0.dict.data.model.WordSense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordEntryTest {

    @Test
    fun testWordEntryCreation() {
        val sense1 = WordSense(
            pos = "adjective",
            definition = "Feeling or showing pleasure or contentment.",
            examples = listOf("a happy mood"),
            synonyms = listOf("cheerful", "contented", "glad")
        )
        val entry = WordEntry(
            word = "happy",
            senses = listOf(sense1)
        )

        assertEquals("happy", entry.word)
        assertEquals(1, entry.senses.size)
        assertEquals("adjective", entry.senses[0].pos)
        assertTrue(entry.senses[0].synonyms.contains("cheerful"))
        assertEquals("a happy mood", entry.senses[0].examples[0])
    }
}
