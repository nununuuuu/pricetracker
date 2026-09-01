package tw.pricecompare

import org.json.JSONArray
import org.json.JSONObject

/** Kotlin ports of the respective upstream platform parsers. */
class CostcoPlatform : BasePlatform() {
    override val id = PlatformId.COSTCO
    override suspend fun fetch(query: String, poolSize: Int, includeAuction: Boolean): List<Candidate> {
        val url = "https://www.costco.com.tw/rest/v2/taiwan/products/search?query=${encoded(query)}&fields=FULL&lang=zh_TW&curr=TWD&sort=price-asc&pageSize=100"
        val products = JSONObject(get(url, mapOf("Referer" to "https://www.costco.com.tw/search")) ?: return emptyList()).optJSONArray("products") ?: return emptyList()
        return products.mapObjects { item ->
            val code = item.optString("code"); val name = item.optString("name"); val path = item.optString("url")
            if (code.isBlank() || name.isBlank() || path.isBlank()) null else Candidate(code, name, item.optJSONObject("price")?.opt("value"), "https://www.costco.com.tw$path")
        }
    }
}

class PxboxPlatform : BasePlatform() {
    override val id = PlatformId.PXBOX
    override suspend fun fetch(query: String, poolSize: Int, includeAuction: Boolean): List<Candidate> {
        val payload = JSONObject().put("search_setting_type", 2).put("keyword", query).put("sort_type", 3).put("sort_order", 1)
            .put("page_index", 1).put("page_size", 100).put("filters", JSONArray()).toString()
        val json = JSONObject(post("https://api-pxbox.es.pxmart.com.tw/app/2.0/spu/single_search", payload, mapOf("Origin" to "https://pxbox.es.pxmart.com.tw", "Referer" to "https://pxbox.es.pxmart.com.tw/")) ?: return emptyList())
        if (json.optString("code") != "0000") return emptyList()
        return (json.optJSONObject("data")?.optJSONArray("product_list") ?: return emptyList()).mapObjects { item ->
            if (item.optBoolean("is_sold_out") || item.optBoolean("is_ad")) null else {
                val productId = item.opt("id")?.toString().orEmpty(); val name = item.optString("product_name")
                if (productId.isBlank() || name.isBlank()) null else Candidate(productId, name, item.opt("sale_price"), "https://pxbox.es.pxmart.com.tw/product/$productId")
            }
        }
    }
}

class Buy123Platform : BasePlatform() {
    override val id = PlatformId.BUY123
    private val nextData = Regex("<script id=\"__NEXT_DATA__\"[^>]*>(.*?)</script>", setOf(RegexOption.DOT_MATCHES_ALL))
    override suspend fun fetch(query: String, poolSize: Int, includeAuction: Boolean): List<Candidate> {
        val html = get("https://www.buy123.com.tw/search?q=${encoded(query)}") ?: return emptyList()
        val root = JSONObject(nextData.find(html)?.groupValues?.get(1) ?: return emptyList())
        return (root.optJSONObject("props")?.optJSONObject("pageProps")?.optJSONObject("searchCommodities")?.optJSONArray("commodities") ?: return emptyList()).mapObjects { item ->
            val itemId = item.opt("id")?.toString().orEmpty(); val displayId = item.opt("display_id")?.toString().orEmpty(); val name = item.optString("name")
            if (itemId.isBlank() || displayId.isBlank() || name.isBlank()) null else Candidate(itemId, name, item.opt("price"), "https://www.buy123.com.tw/site/sku/$displayId")
        }
    }
}

class BooksPlatform : BasePlatform() {
    override val id = PlatformId.BOOKS
    private val name = Regex("title=\"([^\"]+)\"")
    private val price = Regex("優惠價:.*?<b>([\\d,]+)</b>\\s*元", RegexOption.DOT_MATCHES_ALL)
    override suspend fun fetch(query: String, poolSize: Int, includeAuction: Boolean): List<Candidate> {
        val html = get("https://search.books.com.tw/search/query/cat/all/sort/8/key/${encoded(query)}") ?: return emptyList()
        return html.split("<div class=\"table-td\" id=\"prod-itemlist-").drop(1).mapNotNull { block ->
            val productId = Regex("^([^\"]+)\"").find(block)?.groupValues?.get(1); val productName = name.find(block)?.groupValues?.get(1); val productPrice = price.find(block)?.groupValues?.get(1)
            if (productId.isNullOrBlank() || productName.isNullOrBlank() || productPrice.isNullOrBlank()) null else Candidate(productId, decodeHtml(productName), productPrice, "https://www.books.com.tw/products/$productId")
        }
    }
}

class UniProsperityPlatform : BasePlatform() {
    override val id = PlatformId.UNI_PROSPERITY
    private val tile = Regex("<a class=\"gtm-product-alink\"([^>]*)>")
    override suspend fun fetch(query: String, poolSize: Int, includeAuction: Boolean): List<Candidate> {
        val html = get("https://online.uni-prosperity.com.tw/on/demandware.store/Sites-Uniprosperity-Site/default/Search-UpdateGrid?q=q%3D${encoded(query)}") ?: return emptyList()
        return tile.findAll(html).mapNotNull { match ->
            val attrs = match.groupValues[1]; if ("data-ifavailable=\"false\"" in attrs) return@mapNotNull null
            val productId = attribute(attrs, "data-pid"); val name = attribute(attrs, "data-name"); val productPrice = attribute(attrs, "data-price"); val href = attribute(attrs, "href")
            if (productId == null || name == null || productPrice == null || href == null) null else Candidate(productId, decodeHtml(name), productPrice, "https://online.uni-prosperity.com.tw${decodeHtml(href)}")
        }.toList()
    }
}

private fun JSONArray.mapObjects(transform: (JSONObject) -> Candidate?): List<Candidate> = buildList { for (index in 0 until length()) transform(optJSONObject(index) ?: continue)?.let(::add) }
private fun attribute(attrs: String, key: String) = Regex("$key=\"([^\"]*)\"").find(attrs)?.groupValues?.get(1)
private fun decodeHtml(value: String) = value.replace("&quot;", "\"").replace("&amp;", "&").replace("&#39;", "'")

class PChomePlatform : BasePlatform() {
    override val id = PlatformId.PCHOME
    override suspend fun fetch(query: String, poolSize: Int, includeAuction: Boolean): List<Candidate> = (1..((poolSize + 19) / 20).coerceAtMost(5)).flatMap { page ->
        val json = get("https://ecshweb.pchome.com.tw/search/v3.3/all/results?q=${encoded(query)}&page=$page&sort=prc/ac") ?: return@flatMap emptyList()
        (JSONObject(json).optJSONArray("prods") ?: JSONArray()).mapObjects { item ->
            val productId = item.optString("Id"); val name = item.optString("name")
            if (productId.isBlank() || name.isBlank() || "【加價購】" in name) null else Candidate(productId, name, item.opt("price"), "https://24h.pchome.com.tw/prod/$productId")
        }
    }
}

class EtmallPlatform : BasePlatform() {
    override val id = PlatformId.ETMALL
    override suspend fun fetch(query: String, poolSize: Int, includeAuction: Boolean): List<Candidate> = (0 until ((poolSize + 39) / 40).coerceAtMost(5)).flatMap { page ->
        val json = get("https://www.etmall.com.tw/Search/Get?Keyword=${encoded(query)}&SortType=4&PageSize=40&PageIndex=$page", mapOf("Referer" to "https://www.etmall.com.tw/")) ?: return@flatMap emptyList()
        val products = JSONObject(json).optJSONObject("SearchProductResult")?.optJSONArray("products") ?: JSONArray()
        products.mapObjects { item ->
            val productId = item.opt("id")?.toString().orEmpty(); val name = item.optString("title"); val link = item.optString("pageLink")
            if (productId.isBlank() || name.isBlank()) null else Candidate(productId, name, item.opt("finalPrice"), if (link.startsWith("http")) link else "https://www.etmall.com.tw${link.ifBlank { "/i/$productId" }}")
        }
    }
}

class MomoPlatform : BasePlatform() {
    override val id = PlatformId.MOMO
    override suspend fun fetch(query: String, poolSize: Int, includeAuction: Boolean): List<Candidate> = (1..((poolSize + 19) / 20).coerceAtMost(5)).flatMap { page ->
        val body = JSONObject().put("host", "ecmobile").put("flag", "searchEngine").put("data", JSONObject().put("maxPage", 30).put("cateLevel", -1).put("serviceCode", "MT01").put("platform", 16).put("has3P", "Y").put("searchValue", query).put("curPage", page)).toString()
        val json = JSONObject(post("https://apisearch.momoshop.com.tw/momoSearchCloud/moec/textSearch", body, mapOf("Origin" to "https://m.momoshop.com.tw", "Referer" to "https://m.momoshop.com.tw/")) ?: return@flatMap emptyList())
        if (!json.optBoolean("success")) return@flatMap emptyList()
        (json.optJSONObject("rtnSearchData")?.optJSONArray("goodsInfoList") ?: JSONArray()).mapObjects { item ->
            val productId = item.opt("goodsCode")?.toString().orEmpty(); val name = item.optString("goodsName")
            if (productId.isBlank() || name.isBlank()) null else Candidate(productId, name, item.opt("SALE_PRICE"), "https://www.momoshop.com.tw/goods/GoodsDetail.jsp?i_code=$productId")
        }
    }
}

class CoupangPlatform : BasePlatform() {
    override val id = PlatformId.COUPANG
    private val productStart = "<li class=\"ProductUnit_productUnit__"
    private val vendorId = Regex("data-id=\"(\\d+)\"")
    private val productLink = Regex("href=\"(/products/[^\"?]+)\\?[^\"]*itemId=(\\d+)")
    private val name = Regex("<div class=\"ProductUnit_productNameV2__[^\"]*\">([^<]+)</div>")
    private val price = Regex("<span translate=\"no\">\\$([\\d,]+)</span>")
    override suspend fun fetch(query: String, poolSize: Int, includeAuction: Boolean): List<Candidate> {
        val html = get("https://www.tw.coupang.com/np/search?q=${encoded(query)}&sorter=salePriceAsc&listSize=60") ?: return emptyList()
        return html.split(productStart).drop(1).mapNotNull { block ->
            val vendor = vendorId.find(block)?.groupValues?.get(1); val link = productLink.find(block); val title = name.find(block)?.groupValues?.get(1); val sale = price.find(block)?.groupValues?.get(1)
            if (vendor == null || link == null || title == null || sale == null) null else Candidate(vendor, decodeHtml(title), sale, "https://www.tw.coupang.com${decodeHtml(link.groupValues[1])}?itemId=${link.groupValues[2]}&vendorItemId=$vendor")
        }
    }
}

class RakutenPlatform : BasePlatform() {
    override val id = PlatformId.RAKUTEN
    override suspend fun fetch(query: String, poolSize: Int, includeAuction: Boolean): List<Candidate> {
        val gql = "query fetchSearchPageResults(\$parameters: GspInputType!) { searchPage(parameters: \$parameters) { result { items { itemId itemName itemUrl itemPrice { min max } } } } }"
        val hits = when { poolSize <= 20 -> "Twenty"; poolSize <= 40 -> "Forty"; poolSize <= 60 -> "Sixty"; else -> "Hundred" }
        val json = JSONObject().put("operationName", "fetchSearchPageResults").put("query", gql).put("variables", JSONObject().put("parameters", JSONObject().put("itemHits", hits).put("sort", "LowestPrice").put("keyword", query))).toString()
        val items = JSONObject(post("https://www.rakuten.com.tw/graphql", json, mapOf("Origin" to "https://www.rakuten.com.tw", "Referer" to "https://www.rakuten.com.tw/search/")) ?: return emptyList())
            .optJSONObject("data")?.optJSONObject("searchPage")?.optJSONObject("result")?.optJSONArray("items") ?: return emptyList()
        return items.mapObjects { item -> val key = item.opt("itemId")?.toString().orEmpty(); val title = item.optString("itemName"); val url = item.optString("itemUrl"); val p = item.optJSONObject("itemPrice"); if (key.isBlank() || title.isBlank() || url.isBlank()) null else Candidate(key, title, p?.opt("min"), url, p?.opt("max")) }
    }
}

class YahooShoppingPlatform : YahooPlatform(PlatformId.YAHOO_SHOPPING, "shopping", "price", 2092115029, "shopping_cb")
class YahooAuctionPlatform : YahooPlatform(PlatformId.YAHOO_AUCTION, "auction", "curp", 2092111218, "auction_pic_cb") {
    override fun accept(item: JSONObject, includeAuction: Boolean): Boolean = includeAuction || item.optDouble("ec_buyprice") > 0
    override fun price(item: JSONObject, includeAuction: Boolean): Any? = if (includeAuction) item.optDouble("ec_buyprice").takeIf { it > 0 } ?: item.opt("ec_price") else item.opt("ec_buyprice")
}
open class YahooPlatform(private val platformId: PlatformId, private val property: String, private val sort: String, private val spaceId: Int, private val chain: String) : BasePlatform() {
    override val id get() = platformId
    protected open fun accept(item: JSONObject, includeAuction: Boolean) = true
    protected open fun price(item: JSONObject, includeAuction: Boolean): Any? = item.opt("ec_price")
    override suspend fun fetch(query: String, poolSize: Int, includeAuction: Boolean): List<Candidate> {
        val hash = if (platformId == PlatformId.YAHOO_AUCTION) "9e8c95a7bd216439855a6dcb580387b180713a20260a89c26096fbe4dd30133f" else "2a0c2518414ba006e0a42b5bc640a76bbb533e99a336d55027f6e3b4a796aafd"
        val variables = JSONObject().put("property", property).put("cid", "0").put("clv", "0").put("p", query).put("pg", "1").put("psz", poolSize.coerceAtMost(60).toString()).put("qt", "product").put("sort", sort).put("isTestStoreIncluded", "0").put("spaceId", spaceId).put("searchChain", chain).put("source", "pc")
        val body = JSONObject().put("variables", variables).put("extensions", JSONObject().put("persistedQuery", JSONObject().put("version", 1).put("sha256Hash", hash))).toString()
        val hits = JSONObject(post("https://graphql.ec.yahoo.com/graphql", body) ?: return emptyList()).optJSONObject("data")?.optJSONObject("getUther")?.optJSONArray("hits") ?: return emptyList()
        return hits.mapObjects { item -> val key = item.optString("ec_productid"); val title = item.optString("ec_title"); val url = item.optString("ec_item_url"); if (key.isBlank() || title.isBlank() || url.isBlank() || !accept(item, includeAuction)) null else Candidate(key, title, price(item, includeAuction), if (url.startsWith("http")) url else "https://tw.bid.yahoo.com$url", item.opt("ec_max_price")) }
    }
}

class PineconePlatform : BasePlatform() {
    override val id = PlatformId.PCONE
    override suspend fun fetch(query: String, poolSize: Int, includeAuction: Boolean): List<Candidate> {
        val body = JSONObject().put("count", 100).put("page", 1).put("seed", JSONObject.NULL).put("kw", query).toString()
        val products = JSONObject(post("https://webapi.pcone.com.tw/api/products/search", body, mapOf("Origin" to "https://pcone.com.tw", "Referer" to "https://pcone.com.tw/search")) ?: return emptyList()).optJSONObject("data")?.optJSONArray("products") ?: return emptyList()
        return products.mapObjects { item -> val key = item.opt("display_id")?.toString().orEmpty(); val title = item.optString("name"); val url = item.optString("link_url"); if (key.isBlank() || title.isBlank() || url.isBlank()) null else Candidate(key, title, item.opt("price"), url) }
    }
}

class RutenPlatform : BasePlatform() {
    override val id = PlatformId.RUTEN
    override suspend fun fetch(query: String, poolSize: Int, includeAuction: Boolean): List<Candidate> {
        val type = if (includeAuction) "" else "&type=direct"
        val rows = JSONObject(get("https://rtapi.ruten.com.tw/api/search/v3/index.php/core/prod?q=${encoded(query)}&limit=100$type", mapOf("Referer" to "https://www.ruten.com.tw/")) ?: return emptyList()).optJSONArray("Rows") ?: return emptyList()
        val ids = (0 until rows.length()).mapNotNull { rows.optJSONObject(it)?.optString("Id")?.takeIf(String::isNotBlank) }; if (ids.isEmpty()) return emptyList()
        val items = JSONObject(get("https://rapi.ruten.com.tw/api/items/v2/list?gno=${encoded(ids.joinToString(","))}") ?: return emptyList()).optJSONArray("data") ?: return emptyList()
        return items.mapObjects { item -> if (!includeAuction && item.optString("mode") != "B") null else { val key = item.optString("id"); val title = item.optString("name"); if (key.isBlank() || title.isBlank()) null else Candidate(key, title, item.opt("goods_price"), "https://www.ruten.com.tw/item/show?$key") } }
    }
}
