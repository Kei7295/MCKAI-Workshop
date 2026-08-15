package com.mckai.app.domain.tools

/**
 * 内置 Minecraft 物品数据表（minecraft-data 精简版，ModCrafting 知识库思路）。
 * 用于 mc_item_lookup 工具：向 LLM 提供常见物品的 ID、堆叠、分类、合成/用途。
 */
object McItemData {

    data class ItemInfo(
        val id: String,
        val nameZh: String,
        val stackSize: Int = 64,
        val category: String,
        val notes: String = ""
    )

    val items: List<ItemInfo> = listOf(
        ItemInfo("diamond", "钻石", category = "矿物", notes = "挖掘钻石矿石获得，附魔与制作钻石工具"),
        ItemInfo("iron_ingot", "铁锭", category = "矿物", notes = "熔炼铁矿石获得"),
        ItemInfo("gold_ingot", "金锭", category = "矿物", notes = "熔炼金矿石/下界金矿石获得"),
        ItemInfo("netherite_ingot", "下界合金锭", category = "矿物", notes = "下界合金碎片+金锭在锻造台合成，火焰免疫掉落物"),
        ItemInfo("copper_ingot", "铜锭", category = "矿物", notes = "熔炼铜矿石获得"),
        ItemInfo("redstone", "红石", category = "红石", stackSize = 16, notes = "红石电路基础，64→16 堆叠"),
        ItemInfo("sugar_cane", "甘蔗", category = "植物", notes = "河流旁生成，制作纸/糖"),
        ItemInfo("cactus", "仙人掌", category = "植物", notes = "沙漠生成，熔炼绿色染料"),
        ItemInfo("ender_pearl", "末影珍珠", category = "末地", stackSize = 16, notes = "末影人掉落，传送/合成末影之眼"),
        ItemInfo("ender_eye", "末影之眼", category = "末地", stackSize = 16, notes = "末影珍珠+烈焰粉合成"),
        ItemInfo("blaze_rod", "烈焰棒", category = "下界", stackSize = 16, notes = "烈焰人掉落，熔炼为燃料/制作烈焰粉"),
        ItemInfo("ghast_tear", "恶魂之泪", category = "下界", notes = "恶魂掉落，制作再生药水"),
        ItemInfo("shulker_shell", "潜影壳", category = "末地", notes = "潜影贝掉落，合成潜影盒"),
        ItemInfo("elytra", "鞘翅", stackSize = 1, category = "末地", notes = "末地船战利品，飞行装备"),
        ItemInfo("totem_of_undying", "不死图腾", stackSize = 1, category = "稀有", notes = "林地府邸/掠夺者队长掉落，免死一次"),
        ItemInfo("trident", "三叉戟", stackSize = 1, category = "武器", notes = "溺尸稀有掉落，附魔引雷/忠诚"),
        ItemInfo("mace", "重锤", stackSize = 1, category = "武器", notes = "1.21 新武器，下落攻击伤害翻倍"),
        ItemInfo("sculk", "幽匿块", category = "方块", notes = "幽匿系列，1.19 深暗之域"),
        ItemInfo("amethyst_shard", "紫水晶碎片", category = "矿物", notes = "紫水晶簇收割，合成望远镜"),
        ItemInfo("echo_shard", "回响碎片", category = "稀有", notes = "远古城市战利品，合成追溯指针"),
        ItemInfo("recovery_compass", "追溯指针", stackSize = 1, category = "工具", notes = "回响碎片+指南针合成，指向死亡地点"),
        ItemInfo("heart_of_the_sea", "海洋之心", stackSize = 1, category = "海洋", notes = "沉船/水下遗迹，合成潮涌核心"),
        ItemInfo("nautilus_shell", "鹦鹉螺壳", category = "海洋", notes = "钓鱼/溺尸掉落，合成潮涌核心"),
        ItemInfo("prismarine_shard", "海晶碎片", category = "海洋", notes = "守卫者掉落"),
        ItemInfo("slime_ball", "粘液球", category = "红石", notes = "史莱姆掉落，粘性活塞/栓绳"),
        ItemInfo("lead", "拴绳", category = "工具", notes = "4线+1粘液球"),
        ItemInfo("name_tag", "命名牌", category = "工具", notes = "铁砧命名，村庄/钓鱼/箱子获得"),
        ItemInfo("nether_star", "下界之星", stackSize = 1, category = "稀有", notes = "凋灵掉落，合成信标"),
        ItemInfo("dragon_egg", "龙蛋", stackSize = 1, category = "稀有", notes = "末影龙首杀掉落，无法直接采集"),
        ItemInfo("wither_skeleton_skull", "凋灵骷髅头颅", stackSize = 1, category = "稀有", notes = "凋灵骷髅罕见掉落，召唤凋灵"),
        ItemInfo("honey_bottle", "蜂蜜瓶", stackSize = 16, category = "食物", notes = "解除中毒，糖的替代品"),
        ItemInfo("glow_ink_sac", "发光墨囊", category = "材料", notes = "发光鱿鱼掉落，发光告示牌"),
        ItemInfo("quartz", "下界石英", category = "下界", notes = "下界石英矿石掉落，红色比较器等"),
        ItemInfo("paper", "纸", category = "材料", notes = "3甘蔗合成，书/地图/爆竹"),
        ItemInfo("book", "书", category = "材料", notes = "纸+皮革，合成书架/附魔台"),
        ItemInfo("writable_book", "书与笔", stackSize = 1, category = "材料", notes = "书+墨囊+羽毛"),
        ItemInfo("golden_apple", "金苹果", category = "食物", notes = "8金锭+苹果，吸收/回复"),
        ItemInfo("enchanted_golden_apple", "附魔金苹果", stackSize = 1, category = "食物", notes = "8金块+苹果，不可合成（1.20.2后）"),
        ItemInfo("ender_chest", "末影箱", category = "方块", notes = "8黑曜石+末影之眼，跨维度存取"),
        ItemInfo("shulker_box", "潜影盒", stackSize = 1, category = "方块", notes = "潜影壳+箱子，染料染色"),
        ItemInfo("bed", "床", stackSize = 1, category = "方块", notes = "3羊毛+3木板，设置重生点/睡觉"),
        ItemInfo("respawn_anchor", "重生锚", category = "方块", notes = "下界设置重生点，萤石充能"),
        ItemInfo("lodestone", "磁石", category = "方块", notes = "下界合金锭+雕纹石砖，磁石指南针"),
        ItemInfo("crying_obsidian", "哭泣的黑曜石", category = "方块", notes = "猪灵交易/废弃传送门"),
        ItemInfo("ancient_debris", "远古残骸", category = "矿物", notes = "下界深层生成，熔炼下界合金碎片")
    )

    private val byId = items.associateBy { it.id }

    fun lookup(query: String): String {
        val q = query.trim().lowercase().replace(" ", "_")
        val direct = byId[q]
        if (direct != null) return format(direct)
        val fuzzy = items.filter {
            it.id.contains(q) || it.nameZh.contains(query.trim())
        }
        return if (fuzzy.isEmpty()) "未找到物品 '$query'。试试这些 ID：${items.take(12).joinToString(", ") { it.id }}..."
        else fuzzy.joinToString("\n---\n") { format(it) }
    }

    fun listAll(): String = items.joinToString("\n") { "${it.id} (${it.nameZh}) · ${it.category}" }

    private fun format(it: ItemInfo): String = buildString {
        appendLine("ID: ${it.id}")
        appendLine("名称: ${it.nameZh}")
        appendLine("分类: ${it.category}")
        append("堆叠: ${it.stackSize}")
        if (it.notes.isNotBlank()) appendLine("\n说明: ${it.notes}") else appendLine("")
    }
}