package tw.pricecompare

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

interface PricePlatform {
    val id: PlatformId
    suspend fun fetch(query: String, poolSize: Int, includeAuction: Boolean): List<Candidate>
}

/** Shared pipeline ported from the upstream BasePlatform. */
abstract class BasePlatform : PricePlatform {
    protected open val maxVariantSpread = 8
    private val poolSize = 100
    private val maxPoolSize = 200

    suspend fun search(request: SearchRequest): List<Product> = withContext(Dispatchers.Default) {
        val candidates = runCatching {
            fetch(request.query, min(poolSize * searchMultiplier(request.requiredWordGroups), maxPoolSize), request.includeAuction)
        }.getOrDefault(emptyList())
        build(candidates, request)
    }

    fun build(candidates: Iterable<Candidate>, request: SearchRequest): List<Product> {
        val cheapest = mutableMapOf<String, Product>()
        candidates.forEach { candidate ->
            val name = candidate.name.trim()
            val price = parsePrice(candidate.price) ?: return@forEach
            val max = parsePrice(candidate.priceMax)
            if (candidate.id.isBlank() || name.isBlank() || price <= 0) return@forEach
            if (max != null && max >= price * maxVariantSpread) return@forEach
            if (request.minPrice > 0 && price < request.minPrice) return@forEach
            if (request.maxPrice > 0 && price > request.maxPrice) return@forEach
            if (!matchesKeywordGroups(name, request.requiredWordGroups)) return@forEach
            val product = Product(candidate.id, name, price, candidate.url, id)
            if (price < (cheapest[candidate.id]?.price ?: Int.MAX_VALUE)) cheapest[candidate.id] = product
        }
        return cheapest.values.sortedBy { it.price }.take(request.maxPerPlatform)
    }
}

fun parsePrice(value: Any?): Int? = when (value) {
    null, is Boolean -> null
    is Number -> value.toDouble().takeIf { it.isFinite() }?.toInt()
    else -> value.toString().replace(Regex("[^0-9.]"), "").toDoubleOrNull()?.takeIf { it.isFinite() }?.toInt()
}

fun matchesKeywordGroups(name: String, groups: List<List<String>>): Boolean {
    val haystack = name.lowercase()
    return groups.filter { it.isNotEmpty() }.all { group ->
        group.any { word -> word.isNotBlank() && word.lowercase() in haystack }
    }
}

private fun searchMultiplier(groups: List<List<String>>) = (1 shl groups.size.coerceAtMost(2)).coerceAtMost(4)

