package tw.pricecompare

import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformPipelineTest {
    private val platform = object : BasePlatform() {
        override val id = PlatformId.PCHOME
        override suspend fun fetch(query: String, poolSize: Int, includeAuction: Boolean) = emptyList<Candidate>()
    }

    @Test
    fun `groups are AND while terms in each group are OR`() {
        val request = SearchRequest("tv", requiredWordGroups = listOf(listOf("sony", "索尼"), listOf("tv", "電視")))
        val products = platform.build(
            listOf(
                Candidate("1", "SONY 電視", "12,000", "https://example/1"),
                Candidate("2", "SONY 耳機", "3,000", "https://example/2"),
                Candidate("3", "JBL TV", "8,000", "https://example/3")
            ), request
        )
        assertEquals(listOf("1"), products.map { it.id })
    }

    @Test
    fun `pipeline keeps the cheapest duplicate and removes invalid variant ranges`() {
        val products = platform.build(
            listOf(
                Candidate("one", "商品", 2_000, "https://example/a"),
                Candidate("one", "商品", 1_500, "https://example/b"),
                Candidate("range", "商品", 1, "https://example/c", 9)
            ), SearchRequest("商品")
        )
        assertEquals(listOf("one"), products.map { it.id })
        assertEquals(1_500, products.single().price)
    }
}
