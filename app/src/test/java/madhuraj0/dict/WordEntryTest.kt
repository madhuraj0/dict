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

    @Test
    fun testStemCandidatesVerbPastAndContinuous() {
        val walkedStems = madhuraj0.dict.data.DictionaryDatabaseHelper.getStemCandidates("walked")
        assertTrue("walked should stem to walk", walkedStems.contains("walk"))

        val stoppedStems = madhuraj0.dict.data.DictionaryDatabaseHelper.getStemCandidates("stopped")
        assertTrue("stopped should stem to stop", stoppedStems.contains("stop"))

        val dancedStems = madhuraj0.dict.data.DictionaryDatabaseHelper.getStemCandidates("danced")
        assertTrue("danced should stem to dance", dancedStems.contains("dance"))

        val runningStems = madhuraj0.dict.data.DictionaryDatabaseHelper.getStemCandidates("running")
        assertTrue("running should stem to run", runningStems.contains("run"))

        val writingStems = madhuraj0.dict.data.DictionaryDatabaseHelper.getStemCandidates("writing")
        assertTrue("writing should stem to write", writingStems.contains("write"))
    }

    @Test
    fun testStemCandidatesPlurals() {
        val dogStems = madhuraj0.dict.data.DictionaryDatabaseHelper.getStemCandidates("dogs")
        assertTrue("dogs should stem to dog", dogStems.contains("dog"))

        val boxesStems = madhuraj0.dict.data.DictionaryDatabaseHelper.getStemCandidates("boxes")
        assertTrue("boxes should stem to box", boxesStems.contains("box"))

        val partiesStems = madhuraj0.dict.data.DictionaryDatabaseHelper.getStemCandidates("parties")
        assertTrue("parties should stem to party", partiesStems.contains("party"))

        val tomatoesStems = madhuraj0.dict.data.DictionaryDatabaseHelper.getStemCandidates("tomatoes")
        assertTrue("tomatoes should stem to tomato", tomatoesStems.contains("tomato"))
    }

    @Test
    fun testStemCandidatesIrregularsAndEdgeCases() {
        val doesStems = madhuraj0.dict.data.DictionaryDatabaseHelper.getStemCandidates("does")
        assertEquals(listOf("do"), doesStems)

        val wentStems = madhuraj0.dict.data.DictionaryDatabaseHelper.getStemCandidates("went")
        assertEquals(listOf("go"), wentStems)

        val childrenStems = madhuraj0.dict.data.DictionaryDatabaseHelper.getStemCandidates("children")
        assertEquals(listOf("child"), childrenStems)

        // Blocked words shouldn't stem to false roots
        val newsStems = madhuraj0.dict.data.DictionaryDatabaseHelper.getStemCandidates("news")
        assertTrue("news should not stem to new", newsStems.isEmpty())

        val oddsStems = madhuraj0.dict.data.DictionaryDatabaseHelper.getStemCandidates("odds")
        assertTrue("odds should not stem to odd", oddsStems.isEmpty())

        // Non-sibilant -es words like 'andes' should NOT stem to 'and'
        val andesStems = madhuraj0.dict.data.DictionaryDatabaseHelper.getStemCandidates("andes")
        assertTrue("andes should not stem to and", !andesStems.contains("and"))
    }
}
