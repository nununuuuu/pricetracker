package tw.pricecompare

/** The 14-platform registry mirrored from the upstream MCP service (Shopee excluded). */
fun defaultPlatforms(): Map<PlatformId, BasePlatform> = listOf(
    MomoPlatform(), PChomePlatform(), CoupangPlatform(), EtmallPlatform(), RakutenPlatform(),
    YahooShoppingPlatform(), YahooAuctionPlatform(), CostcoPlatform(), PxboxPlatform(),
    UniProsperityPlatform(), BooksPlatform(), RutenPlatform(), Buy123Platform(), PineconePlatform()
).associateBy { it.id }
