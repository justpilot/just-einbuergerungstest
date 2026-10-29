package io.justpilot.justeinbuergerung.data.catalog

import io.justpilot.justeinbuergerung.domain.model.Category
import io.justpilot.justeinbuergerung.domain.model.FederalState
import io.justpilot.justeinbuergerung.domain.model.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CatalogParserTest {
    private fun parse(json: String) = CatalogParser.parse(json.byteInputStream())

    private fun question(
        id: String = "1",
        state: String? = null,
        options: String = """[{"id":"A","text":{"de":"a"}},{"id":"B","text":{"de":"b"}},{"id":"C","text":{"de":"c"}},{"id":"D","text":{"de":"d"}}]""",
        correct: String = "B",
    ) = """{"id":"$id","number":1,"state":${state?.let { "\"$it\"" }},"category":"${if (state == null) "politics" else "state"}",
        |"image":null,"question":{"de":"Frage?","en":"Question?","tr":"Soru?"},"options":$options,"correct_option":"$correct"}""".trimMargin()

    @Test
    fun `parses a question with partial translations`() {
        val catalog = parse("""{"catalogVersion":"v1","questions":[${question(state = "BY")}]}""")

        assertEquals("v1", catalog.version)
        val q = catalog.questions.single()
        assertEquals(FederalState.BAVARIA, q.state)
        assertEquals(Category.STATE, q.category)
        assertEquals("Question?", q.text[Language.ENGLISH])
        assertNull(q.text[Language.UKRAINIAN])
        assertEquals("Soru?", q.text[Language.TURKISH])
        assertEquals(listOf("A", "B", "C", "D"), q.options.map { it.id })
        assertTrue(q.isCorrect("B"))
    }

    @Test
    fun `rejects questions without exactly four options`() {
        val threeOptions = """[{"id":"A","text":{"de":"a"}},{"id":"B","text":{"de":"b"}},{"id":"C","text":{"de":"c"}}]"""
        assertThrows(IllegalArgumentException::class.java) {
            parse("""{"catalogVersion":"v1","questions":[${question(options = threeOptions)}]}""")
        }
    }

    @Test
    fun `rejects a correct option that does not exist`() {
        assertThrows(IllegalArgumentException::class.java) {
            parse("""{"catalogVersion":"v1","questions":[${question(correct = "E")}]}""")
        }
    }

    @Test
    fun `rejects unknown state codes`() {
        assertThrows(IllegalArgumentException::class.java) {
            parse("""{"catalogVersion":"v1","questions":[${question(state = "XX")}]}""")
        }
    }

    /** Guards the generated asset itself: everything the app relies on must hold for the shipped file. */
    @Test
    fun `bundled catalog is complete and consistent`() {
        val assets = File("src/main/assets")
        val catalog = CatalogParser.parse(File(assets, CatalogParser.ASSET_PATH).inputStream())

        assertEquals(460, catalog.questions.size)
        assertEquals(catalog.questions.size, catalog.questions.map { it.id }.toSet().size)
        assertEquals((1..300).toList(), catalog.questions.filter { it.state == null }.map { it.number })
        for (state in FederalState.entries) {
            val stateQuestions = catalog.questions.filter { it.state == state }
            assertEquals("questions of $state", (1..10).map { "${state.code}-$it" }, stateQuestions.map { it.id })
            assertTrue(stateQuestions.all { it.category == Category.STATE })
        }
        for (question in catalog.questions) {
            assertTrue("${question.id} has no German text", question.text.german.isNotBlank())
            for (language in Language.translations) {
                assertTrue("${question.id} lacks $language", !question.text[language].isNullOrBlank())
                assertTrue("${question.id} options lack $language", question.options.all { !it.text[language].isNullOrBlank() })
            }
            question.imagePath?.let { assertTrue("missing image $it", File(assets, it).isFile) }
        }
    }
}
