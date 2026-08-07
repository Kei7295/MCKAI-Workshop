package com.mckai.app.domain.tools

import kotlinx.serialization.json.*

fun registerMinecraftTools(r: ToolRegistry) {
    r.register(ToolMetadata(
        name = "mc_version_info",
        description = "获取 Minecraft 版本信息，包括支持的模组加载器版本",
        parameters = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {
                put("version", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("MC 版本号，如 1.21.4")) })
            })
            put("required", buildJsonArray { add(JsonPrimitive("version")) })
        },
        category = "minecraft"
    ), handler = { args ->
        val version = args["version"]?.jsonPrimitive?.content ?: return@register "请提供 version 参数"
        when (version) {
            "1.21.4" -> """
                |Minecraft $version
                |Fabric: Loader 0.16.10, API 0.116.0, Loom 1.17.12
                |Forge: 50.1.0
                |NeoForge: 21.1.x
                |Gradle: 9.5, Java: 21
            """.trimMargin()
            "1.21.1" -> "Minecraft $version | NeoForge 21.1.x | Fabric Loader 0.16.x"
            "1.20.1" -> "Minecraft $version | Forge 47.3.0 | Fabric Loader 0.15.x"
            "1.21" -> "Minecraft $version | Bedrock Edition"
            "1.20.10" -> "Minecraft $version | NetEase Edition"
            else -> "版本 $version 的详细信息请参考 https://minecraft.wiki"
        }
    })

    r.register(ToolMetadata(
        name = "mc_biome_data",
        description = "获取 Minecraft 生物群系信息",
        parameters = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {
                put("biome", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("生物群系名称")) })
            })
            put("required", buildJsonArray { add(JsonPrimitive("biome")) })
        },
        category = "minecraft"
    ), handler = { args ->
        val biome = args["biome"]?.jsonPrimitive?.content ?: return@register "请提供 biome 参数"
        val data = mapOf(
            "plains" to "温度: 0.8, 湿度: 0.4, 草方块, 向日葵, 牛, 羊, 猪",
            "forest" to "温度: 0.7, 湿度: 0.8, 橡木, 桦木, 狐狸, 狼",
            "desert" to "温度: 2.0, 湿度: 0.0, 沙漠神殿, 仙人掌, 枯萎的灌木",
            "jungle" to "温度: 0.95, 湿度: 0.9, 丛林木, 可可果, 美西螈, 鹦鹉",
            "ocean" to "温度: 0.5, 湿度: 0.5, 海带, 珊瑚, 鳍, 海豚",
            "mountains" to "温度: 0.2, 湿度: 0.3, 石头, 铁矿, 钻石矿, 山羊",
            "taiga" to "温度: 0.25, 湿度: 0.8, 云杉木, 驯鹿, 狼",
            "swamp" to "温度: 0.8, 湿度: 0.9, 橡木, 淤泥, 女巫小屋, 萤火虫"
        )
        data[biome.lowercase()] ?: "未知生物群系 '$biome'。已知：${data.keys.joinToString(", ")}"
    })

    r.register(ToolMetadata(
        name = "mc_recipe_check",
        description = "检查 Minecraft 合成配方",
        parameters = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {
                put("item", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("物品名称")) })
            })
            put("required", buildJsonArray { add(JsonPrimitive("item")) })
        },
        category = "minecraft"
    ), handler = { args ->
        val item = args["item"]?.jsonPrimitive?.content ?: return@register "请提供 item 参数"
        val recipes = mapOf(
            "diamond_sword" to "钻石剑: 2x 钻石 + 1x 木棍（形状合成，竖排）",
            "enchanted_golden_apple" to "附魔金苹果: 8x 金块 + 1x 苹果（无法合成，仅战利品）",
            "crafting_table" to "工作台: 4x 木板（2x2）",
            "furnace" to "熔炉: 8x 圆石（3x3，中间空）",
            "chest" to "箱子: 8x 木板（3x3，中间空）",
            "torch" to "火把: 1x 煤炭/木炭 + 1x 木棍（竖排）",
            "pickaxe" to "镐: 3x 材料 + 2x 木棍（T 形）"
        )
        recipes[item.lowercase().replace(" ", "_")] ?: "未找到 '$item' 的合成配方。已知物品：${recipes.keys.joinToString(", ")}"
    })

    r.register(ToolMetadata(
        name = "mc_mod_template",
        description = "获取 Minecraft 模组模板代码",
        parameters = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {
                put("type", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("模板类型：block/item/entity/food/tool/armor/recipe")) })
                put("platform", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("平台：fabric/forge/neoforge")) })
                put("name", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("名称（英文，PascalCase）")) })
            })
            put("required", buildJsonArray { add(JsonPrimitive("type")); add(JsonPrimitive("platform")); add(JsonPrimitive("name")) })
        },
        category = "minecraft"
    ), handler = { args ->
        val type = args["type"]?.jsonPrimitive?.content ?: return@register "请提供 type 参数"
        val platform = args["platform"]?.jsonPrimitive?.content ?: "fabric"
        val name = args["name"]?.jsonPrimitive?.content ?: return@register "请提供 name 参数"
        when (type.lowercase()) {
            "block" -> generateBlockTemplate(platform, name)
            "item" -> generateItemTemplate(platform, name)
            "entity" -> generateEntityTemplate(platform, name)
            "food" -> generateFoodTemplate(platform, name)
            "tool" -> generateToolTemplate(platform, name)
            "armor" -> generateArmorTemplate(platform, name)
            "recipe" -> "配方模板需要与具体物品配合使用"
            else -> "未知模板类型 '$type'。支持：block, item, entity, food, tool, armor, recipe"
        }
    })
}

private fun generateBlockTemplate(platform: String, name: String): String = when (platform) {
    "fabric" -> """
        |package com.example.mod;
        |
        |import net.minecraft.block.Block;
        |import net.minecraft.block.MapColor;
        |import net.minecraft.item.BlockItem;
        |import net.minecraft.item.Item;
        |import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
        |import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
        |
        |public class ${name}Block {
        |    public static final Block INSTANCE = new Block(FabricBlockSettings.create()
        |        .mapColor(MapColor.STONE_GRAY)
        |        .strength(3.0f, 3.0f)
        |        .requiresTool());
        |
        |    public static final BlockItem ITEM = new BlockItem(INSTANCE, new FabricItemSettings());
        |
        |    public static void register() {
        |        // Register block and item in your mod initializer
        |    }
        |}
    """.trimMargin()
    else -> "// $platform block template for $name"
}

private fun generateItemTemplate(platform: String, name: String): String = """
    |// ${platform.uppercase()} Item Template: $name
    |// Create an Item class extending net.minecraft.world.item.Item
    |// Register in your mod initializer with appropriate settings
""".trimMargin()

private fun generateEntityTemplate(platform: String, name: String): String = """
    |// ${platform.uppercase()} Entity Template: $name
    |// Create an Entity class extending LivingEntity or AnimalEntity
    |// Register entity type, renderer, and AI goals
""".trimMargin()

private fun generateFoodTemplate(platform: String, name: String): String = """
    |// ${platform.uppercase()} Food Template: $name
    |// Create an Item with FoodComponent/Properties
    |// Set nutrition, saturation, and effects
""".trimMargin()

private fun generateToolTemplate(platform: String, name: String): String = """
    |// ${platform.uppercase()} Tool Template: $name
    |// Create a ToolItem with ToolMaterial
    |// Define attack damage, mining speed, durability
""".trimMargin()

private fun generateArmorTemplate(platform: String, name: String): String = """
    |// ${platform.uppercase()} Armor Template: $name
    |// Create ArmorItem with ArmorMaterial
    |// Define protection values and durability for each slot
""".trimMargin()
