package tw.pricecompare

data class Product(
    val id: String,
    val name: String,
    val price: Int,
    val url: String,
    val platform: PlatformId
)

data class SearchResult(val query: String, val products: List<Product>) {
    val totalCount: Int get() = products.size
}

data class Candidate(
    val id: String,
    val name: String,
    val price: Any?,
    val url: String,
    val priceMax: Any? = null
)

enum class PlatformId {
    MOMO, PCHOME, COUPANG, ETMALL, RAKUTEN, YAHOO_SHOPPING, YAHOO_AUCTION,
    COSTCO, PXBOX, UNI_PROSPERITY, BOOKS, RUTEN, BUY123, PCONE
}

enum class SearchMode { FULL, FAST }

data class SearchRequest(
    val query: String,
    val maxPerPlatform: Int = 100,
    val minPrice: Int = 0,
    val maxPrice: Int = 0,
    /** Groups are AND; terms inside each group are OR. */
    val requiredWordGroups: List<List<String>> = emptyList(),
    val includeAuction: Boolean = false,
    val mode: SearchMode = SearchMode.FULL
)

