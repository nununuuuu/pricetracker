package com.example.engine

data class FalsePriceCheckResult(
    val isSuspected: Boolean,
    val matchedKeywords: List<String>,
    val penaltyScore: Int,
    val reason: String
)

/**
 * 假低價/配件過濾器。
 * 支援根據「個別商品監控規則 (MonitorRule)」獨立配置排除字典，不再強制共用單一全域排除字典。
 */
object FalsePriceFilter {

    // 系統預設通用排除字典庫（可於設定頁面自由添加/刪除）
    val DEFAULT_UNIVERSAL_DICTIONARY = listOf(
        "耳罩", "耳機架", "保護套", "保護殼", "線材", "空盒", "原廠盒", "副廠線", "替換耳塞",
        "收納包", "散熱片", "散熱墊", "風扇支架", "顯卡支架", "延長線", "電源線", "導熱膏",
        "螺絲包", "擋板", "保護貼", "鏡頭貼", "掛繩", "充電線", "退卡針", "模型機",
        "展示品", "零件機", "拆機屏幕", "訂金", "定金", "預付", "租借", "出租",
        "故障", "瑕疵", "壞掉", "不亮", "點不亮", "維修", "報廢", "零件"
    )

    // 依據商品類別提供建議排除字詞庫（方便使用者在個別商品設定時一鍵套用，而非強制共用）
    val CATEGORY_PRESET_SUGGESTIONS = mapOf(
        "3C/耳機/鍵盤" to listOf("耳罩", "耳機架", "保護套", "保護殼", "線材", "空盒", "原廠盒", "副廠線", "替換耳塞", "收納包", "航太線", "鍵帽", "拔軸器"),
        "顯卡/電腦零組件" to listOf("空盒", "原廠盒", "散熱片", "散熱墊", "風扇支架", "顯卡支架", "延長線", "電源線", "導熱膏", "螺絲包", "擋板"),
        "手機/平板" to listOf("保護貼", "保護殼", "鏡頭貼", "掛繩", "充電線", "退卡針", "空盒", "模型機", "展示品", "零件機", "拆機屏幕"),
        "通用交易風險" to listOf("訂金", "定金", "預付", "租借", "出租", "故障", "瑕疵", "壞掉", "不亮", "點不亮", "維修", "報廢", "零件")
    )

    // 預設快速排除選項
    val QUICK_EXCLUDE_CHIPS = listOf(
        "耳罩", "保護套", "線材", "空盒", "零件", "故障", "訂金", "租借", "展示品", "模型", "支架", "散熱片"
    )

    /**
     * 檢查指定商品是否命中排除關鍵字（包含全域通用字典與該監控項目專屬排除詞）。
     *
     * @param title 商品標題
     * @param monitorExclusions 該監控項目設定的專屬排除關鍵字列表
     * @param universalExclusions 系統設定中的通用字典庫列表
     */
    fun checkFalsePrice(
        title: String,
        monitorExclusions: List<String> = emptyList(),
        universalExclusions: List<String> = emptyList()
    ): FalsePriceCheckResult {
        val lowerTitle = title.lowercase()
        val matched = mutableListOf<String>()

        // 整合通用字典與個別專屬字典
        val allExclusions = (monitorExclusions + universalExclusions).distinct()

        allExclusions.forEach { keyword ->
            val trimmed = keyword.trim()
            if (trimmed.isNotBlank() && lowerTitle.contains(trimmed.lowercase()) && !matched.contains(trimmed)) {
                matched.add(trimmed)
            }
        }

        val isSuspected = matched.isNotEmpty()
        val penalty = if (isSuspected) 60 else 0
        val reason = if (isSuspected) {
            "標題命中排除字典詞: [${matched.joinToString(", ")}]"
        } else {
            "通過排除規則檢核"
        }

        return FalsePriceCheckResult(
            isSuspected = isSuspected,
            matchedKeywords = matched,
            penaltyScore = penalty,
            reason = reason
        )
    }
}
