package com.mckai.app.domain.tools

import com.mckai.app.domain.workshop.template.*
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
        name = "mc_item_lookup",
        description = "查询内置 Minecraft 物品数据：ID、中文名、堆叠、分类、用途",
        parameters = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {
                put("name", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("物品 ID 或中文名，如 diamond / 钻石")) })
            })
            put("required", buildJsonArray { add(JsonPrimitive("name")) })
        },
        category = "minecraft"
    ), handler = { args ->
        val name = args["name"]?.jsonPrimitive?.content ?: return@register "请提供 name 参数"
        McItemData.lookup(name)
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
            "recipe" -> generateRecipeTemplate(name)
            else -> "未知模板类型 '$type'。支持：block, item, entity, food, tool, armor, recipe"
        }
    })
r.register(ToolMetadata(
        name = "ask_clarification",
        description = "当用户需求不明确时调用此工具请求澄清。参数 question 是要问的问题，options 是候选选项。调用后会暂停生成并向用户展示问题。",
        parameters = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {
                put("question", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("需要澄清的问题，必须具体")) })
                put("options", buildJsonObject { put("type", JsonPrimitive("array")); put("description", JsonPrimitive("候选选项（2-4个）")) })
            })
            put("required", buildJsonArray { add(JsonPrimitive("question")) })
        },
        category = "minecraft"
    ), handler = { args ->
        val question = args["question"]?.jsonPrimitive?.content ?: "需要澄清"
        val options = args["options"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
        // 从历史 tool 消息回传给模型；此处返回原始参数（模型会继续对话）
        buildString {
            appendLine("【需要澄清】$question")
            options.forEachIndexed { i, opt -> appendLine("${i + 1}. $opt") }
        }
    })
r.register(ToolMetadata(
        name = "fabric_template_generate",
        description = "生成可直接写入项目的 Fabric 模组模板文件集（Java 类 + JSON 资源 + lang 条目）。返回结构化文件内容，用 write_file 工具逐个落盘；比 mc_mod_template 的文本参考更精确，生成的是可编译代码。",
        parameters = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {
                put("template", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("模板类型：BLOCK/ITEM/FOOD/TOOL/ARMOR/ENTITY/RECIPE/MOD_CONFIG")) })
                put("className", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("英文类名（PascalCase，如 MagicSword）")) })
                put("modId", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("模组 ID（小写，如 mymod）")) })
                put("packageName", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("Java 包名，默认 com.example.mod")) })
                put("displayName", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("中文显示名（写入 lang 文件）")) })
                put("toolKind", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("TOOL 模板：pickaxe/sword/axe/shovel/hoe")) })
                put("recipeType", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("RECIPE 模板：shaped/shapeless/smelting/blasting/stonecutting")) })
                put("recipePattern", buildJsonObject { put("type", JsonPrimitive("array")); put("description", JsonPrimitive("RECIPE shaped：图案行，如 [\"AAA\",\"ABA\",\"AAA\"]")) })
                put("recipeKeys", buildJsonObject { put("type", JsonPrimitive("object")); put("description", JsonPrimitive("RECIPE：图案键到物品 ID 的映射，如 {\"A\":\"minecraft:iron_ingot\"}")) })
                put("resultItem", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("RECIPE：产物物品 ID，默认 <modId>:<className 蛇形>")) })
            })
            put("required", buildJsonArray { add(JsonPrimitive("template")); add(JsonPrimitive("className")); add(JsonPrimitive("modId")) })
        },
        category = "minecraft"
    ), handler = { args ->
        val typeName = args["template"]?.jsonPrimitive?.content ?: return@register "请提供 template 参数"
        val type = ModTemplateType.fromName(typeName) ?: return@register "未知模板类型 '$typeName'。支持：${ModTemplateType.entries.joinToString(", ") { it.name }}"
        val className = args["className"]?.jsonPrimitive?.content ?: return@register "请提供 className 参数"
        val modId = args["modId"]?.jsonPrimitive?.content ?: return@register "请提供 modId 参数"
        val p = TemplateParams(
            type = type,
            modId = modId,
            packageName = args["packageName"]?.jsonPrimitive?.content ?: "com.example.mod",
            className = className,
            displayName = args["displayName"]?.jsonPrimitive?.content ?: "",
            toolKind = args["toolKind"]?.jsonPrimitive?.content ?: "pickaxe",
            recipeType = args["recipeType"]?.jsonPrimitive?.content ?: "shaped",
            recipePattern = args["recipePattern"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
            recipeKeys = args["recipeKeys"]?.jsonObject?.mapValues { it.value.jsonPrimitive.content } ?: emptyMap(),
            resultItem = args["resultItem"]?.jsonPrimitive?.content ?: ""
        )
        val result = TemplateEngine.generate(p)
        buildString {
            appendLine("【模板摘要】${result.summary}")
            appendLine()
            result.files.forEach { (path, content) ->
                appendLine("=== 文件: $path ===")
                appendLine(content)
                appendLine()
            }
            if (result.langEntries.isNotEmpty()) {
                appendLine("=== lang 条目（合并进 assets/$modId/lang/zh_cn.json，用 lang_merge 工具或手动合并）===")
                result.langEntries.forEach { (k, v) -> appendLine("$k: $v") }
                appendLine()
            }
            if (result.registerSnippet.isNotBlank()) {
                appendLine("=== 注册代码（合并进模组主类 onInitialize）===")
                appendLine(result.registerSnippet)
                appendLine()
            }
        }.trimEnd()
    })

    r.register(ToolMetadata(
        name = "lang_merge",
        description = "将新条目深合并进指定 lang 文件（不覆盖已有值）。参数 path 是 lang JSON 文件路径，entries 是键值映射。需先读取原文件内容。",
        parameters = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {
                put("path", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("lang 文件路径，如 src/main/resources/assets/mymod/lang/zh_cn.json")) })
                put("entries", buildJsonObject { put("type", JsonPrimitive("object")); put("description", JsonPrimitive("要合并的键值对，如 {\"item.mymod.sword\":\"神剑\"}")) })
            })
            put("required", buildJsonArray { add(JsonPrimitive("path")); add(JsonPrimitive("entries")) })
        },
        category = "minecraft"
    ), handler = { args ->
        val path = args["path"]?.jsonPrimitive?.content ?: return@register "请提供 path 参数"
        val entries = args["entries"]?.jsonObject?.mapValues { it.value.jsonPrimitive.content } ?: return@register "请提供 entries 参数"
        val merged = LangMerger.merge(existing = null, newEntries = entries)
        buildString {
            appendLine("=== 文件: $path（lang 合并结果，用 write_file 覆盖写入）===")
            append(merged)
        }
    })

    r.register(ToolMetadata(
        name = "recipe_generate",
        description = "生成任意合成配方 JSON（shaped/shapeless/smelting/blasting/stonecutting），返回可直接写入 data/<modid>/recipe/ 的文件内容。",
        parameters = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {
                put("type", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("配方类型：shaped/shapeless/smelting/blasting/stonecutting")) })
                put("pattern", buildJsonObject { put("type", JsonPrimitive("array")); put("description", JsonPrimitive("shaped 图案行，如 [\"AAA\",\"ABA\",\"AAA\"]")) })
                put("keys", buildJsonObject { put("type", JsonPrimitive("object")); put("description", JsonPrimitive("图案键到物品 ID 映射，如 {\"A\":\"minecraft:iron_ingot\"}；shapeless 时为原料列表")) })
                put("resultItem", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("产物物品 ID")) })
                put("resultCount", buildJsonObject { put("type", JsonPrimitive("integer")); put("description", JsonPrimitive("产物数量，默认 1")) })
            })
            put("required", buildJsonArray { add(JsonPrimitive("type")); add(JsonPrimitive("resultItem")) })
        },
        category = "minecraft"
    ), handler = { args ->
        val type = args["type"]?.jsonPrimitive?.content ?: return@register "请提供 type 参数"
        val resultItem = args["resultItem"]?.jsonPrimitive?.content ?: return@register "请提供 resultItem 参数"
        val json = RecipeGenerator.generate(
            type = type,
            pattern = args["pattern"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
            keys = args["keys"]?.jsonObject?.mapValues { it.value.jsonPrimitive.content } ?: emptyMap(),
            resultItem = resultItem,
            resultCount = args["resultCount"]?.jsonPrimitive?.intOrNull ?: 1
        )
        "=== 文件: data/<modid>/recipe/<名称>.json（用 write_file 写入）===\n$json"
    })
}

/** 澄清机制（ModCrafting ask_clarification）：生成结构化澄清请求 JSON */
private const val CLARIFY_TEMPLATE = """
{
  "clarification": {
    "question": "这里写需要澄清的问题",
    "options": ["选项A", "选项B", "选项C"],
    "reason": "为什么需要澄清（可选）"
  }
}
"""

private fun generateRecipeTemplate(itemName: String): String = """
    |// 合成配方 JSON（Fabric 路径：src/main/resources/data/<modid>/recipe/<name>.json）
    |{
    |  "type": "minecraft:crafting_shaped",
    |  "pattern": [
    |    "AAA",
    |    "ABA",
    |    "AAA"
    |  ],
    |  "key": {
    |    "A": { "item": "minecraft:iron_ingot" },
    |    "B": { "item": "minecraft:${itemName.lowercase()}" }
    |  },
    |  "result": {
    |    "item": "<modid>:${itemName.lowercase()}",
    |    "count": 1
    |  }
    |}
    |
    |// 熔炼配方（smelting）
    |{
    |  "type": "minecraft:smelting",
    |  "ingredient": { "item": "minecraft:raw_iron" },
    |  "result": { "id": "minecraft:iron_ingot" },
    |  "experience": 0.7,
    |  "cookingtime": 200
    |}
""".trimMargin()

private fun generateBlockTemplate(platform: String, name: String): String {
    if (platform != "fabric") return "// $platform 暂只支持 Fabric 完整模板"
    val lower = name.lowercase()
    return """
        |=== Java 主类（src/main/java/com/example/mod/${name}Block.java）===
        |package com.example.mod;
        |
        |import net.minecraft.block.AbstractBlock;
        |import net.minecraft.block.Block;
        |import net.minecraft.block.MapColor;
        |import net.minecraft.item.BlockItem;
        |import net.minecraft.item.Item;
        |import net.minecraft.registry.Registries;
        |import net.minecraft.registry.Registry;
        |import net.minecraft.util.Identifier;
        |
        |public class ${name}Block extends Block {
        |    public ${name}Block() {
        |        super(AbstractBlock.Settings.create()
        |            .mapColor(MapColor.STONE_GRAY)
        |            .strength(3.0f, 6.0f)
        |            .requiresTool());
        |    }
        |
        |    public static final Block INSTANCE = new ${name}Block();
        |    public static final Item ITEM = new BlockItem(INSTANCE, new Item.Settings());
        |
        |    public static void register() {
        |        Registry.register(Registries.BLOCK, Identifier.of("<modid>", "$lower"), INSTANCE);
        |        Registry.register(Registries.ITEM, Identifier.of("<modid>", "$lower"), ITEM);
        |    }
        |}
        |
        |=== 配方（src/main/resources/data/<modid>/recipe/${lower}.json）===
        |{
        |  "type": "minecraft:crafting_shaped",
        |  "pattern": ["AAA", "ABA", "AAA"],
        |  "key": {
        |    "A": { "item": "minecraft:stone" },
        |    "B": { "item": "minecraft:iron_ingot" }
        |  },
        |  "result": { "item": "<modid>:$lower" }
        |}
        |
        |=== 模型（src/main/resources/assets/<modid>/models/block/${lower}.json）===
        |{ "parent": "minecraft:block/cube_all", "textures": { "all": "<modid>:block/$lower" } }
        |
        |=== 物品模型（assets/<modid>/models/item/${lower}.json）===
        |{ "parent": "<modid>:block/$lower" }
        |
        |=== Blockstate（assets/<modid>/blockstates/${lower}.json）===
        |{
        |  "variants": {
        |    "": { "model": "<modid>:block/$lower" }
        |  }
        |}
    """.trimMargin()
}

private fun generateItemTemplate(platform: String, name: String): String {
    if (platform != "fabric") return "// $platform 暂只支持 Fabric 完整模板"
    val lower = name.lowercase()
    return """
        |=== Java 类（src/main/java/com/example/mod/${name}Item.java）===
        |package com.example.mod;
        |
        |import net.minecraft.item.Item;
        |import net.minecraft.registry.Registries;
        |import net.minecraft.registry.Registry;
        |import net.minecraft.util.Identifier;
        |
        |public class ${name}Item extends Item {
        |    public ${name}Item() {
        |        super(new Item.Settings().maxCount(64));
        |    }
        |
        |    public static final Item INSTANCE = new ${name}Item();
        |
        |    public static void register() {
        |        Registry.register(Registries.ITEM, Identifier.of("<modid>", "$lower"), INSTANCE);
        |    }
        |}
        |
        |=== 物品模型（assets/<modid>/models/item/${lower}.json）===
        |{
        |  "parent": "minecraft:item/generated",
        |  "textures": { "layer0": "<modid>:item/$lower" }
        |}
        |
        |=== 语言文件（assets/<modid>/lang/zh_cn.json 追加）===
        |{ "item.<modid>.$lower": "${name}物品名" }
    """.trimMargin()
}

private fun generateEntityTemplate(platform: String, name: String): String {
    if (platform != "fabric") return "// $platform 暂只支持 Fabric 完整模板"
    val lower = name.lowercase()
    return """
        |=== 实体类（src/main/java/com/example/mod/${name}Entity.java）===
        |package com.example.mod;
        |
        |import net.minecraft.entity.EntityType;
        |import net.minecraft.entity.mob.PathAwareEntity;
        |import net.minecraft.world.World;
        |
        |public class ${name}Entity extends PathAwareEntity {
        |    public ${name}Entity(EntityType<? extends PathAwareEntity> type, World world) {
        |        super(type, world);
        |    }
        |
        |    public static final EntityType<${name}Entity> TYPE = EntityType.Builder
        |        .create(${name}Entity::new, SpawnGroup.CREATURE)
        |        .dimensions(0.6f, 1.8f)
        |        .build();
        |
        |    public static void register() {
        |        Registry.register(Registries.ENTITY_TYPE,
        |            Identifier.of("<modid>", "$lower"), TYPE);
        |        // 还需 FabricDefaultBiomeModifications.addSpawn 控制生成
        |    }
        |}
        |
        |=== 实体模型/贴图（assets/<modid>/models/entity/${lower}.json）===
        |{ "texture_size": [64, 32], "textures": { "main": "<modid>:textures/entity/$lower.png" }, "elements": [] }
        |
        |=== 生成配置（Fabric 主类中调用）===
        |FabricDefaultBiomeModifications.addSpawn(
        |    SpawnContext.of(SpawnGroup.CREATURE),
        |    SpawnRestriction.Location.ON_GROUND,
        |    TrackedEntity.create(0.5, 2, 16),
        |    (biome, context) -> true
        |);
    """.trimMargin()
}

private fun generateFoodTemplate(platform: String, name: String): String {
    if (platform != "fabric") return "// $platform 暂只支持 Fabric 完整模板"
    val lower = name.lowercase()
    return """
        |=== Java 类（src/main/java/com/example/mod/${name}Item.java）===
        |package com.example.mod;
        |
        |import net.minecraft.component.type.FoodComponent;
        |import net.minecraft.item.Item;
        |import net.minecraft.registry.Registries;
        |import net.minecraft.registry.Registry;
        |import net.minecraft.util.Identifier;
        |
        |public class ${name}Item extends Item {
        |    public ${name}Item() {
        |        super(new Item.Settings()
        |            .food(new FoodComponent.Builder()
        |                .nutrition(4)          // 恢复 4 格饥饿
        |                .saturationModifier(0.3f)  // 饱和度系数
        |                .snack()               // 小吃：可快速食用
        |                .build()));
        |    }
        |
        |    public static final Item INSTANCE = new ${name}Item();
        |
        |    public static void register() {
        |        Registry.register(Registries.ITEM, Identifier.of("<modid>", "$lower"), INSTANCE);
        |    }
        |}
        |
        |=== 物品模型（assets/<modid>/models/item/${lower}.json）===
        |{
        |  "parent": "minecraft:item/generated",
        |  "textures": { "layer0": "<modid>:item/$lower" }
        |}
    """.trimMargin()
}

private fun generateToolTemplate(platform: String, name: String): String {
    if (platform != "fabric") return "// $platform 暂只支持 Fabric 完整模板"
    val lower = name.lowercase()
    return """
        |=== 工具材料（src/main/java/com/example/mod/${name}Material.java）===
        |package com.example.mod;
        |
        |import net.minecraft.item.ToolMaterial;
        |import net.minecraft.registry.tag.BlockTags;
        |import net.minecraft.registry.tag.ItemTags;
        |
        |public class ${name}Material implements ToolMaterial {
        |    public static final ${name}Material INSTANCE = new ${name}Material();
        |
        |    @Override public int getDurability() { return 512; }
        |    @Override public float getMiningSpeed() { return 6.0f; }
        |    @Override public float getAttackDamage() { return 3.0f; }
        |    @Override public int getMiningLevel() { return 3; }
        |    @Override public int getEnchantability() { return 14; }
        |    @Override public Ingredient getRepairIngredient() {
        |        return Ingredient.ofItems(/* 修复物品，如 Items.DIAMOND */);
        |    }
        |}
        |
        |=== 工具类（${name}Tool.java）===
        |package com.example.mod;
        |
        |import net.minecraft.item.PickaxeItem;
        |import net.minecraft.item.ToolMaterial;
        |
        |public class ${name}Tool extends PickaxeItem {
        |    public ${name}Tool(ToolMaterial material, float attackDamage,
        |                      float attackSpeed, Settings settings) {
        |        super(material, attackDamage, attackSpeed, settings);
        |    }
        |}
        |// 注册：
        |// Registry.register(Registries.ITEM, Identifier.of("<modid>", "$lower"),
        |//     new ${name}Tool(${name}Material.INSTANCE, 1.0f, -2.8f,
        |//         new Item.Settings().maxDamage(512)));
    """.trimMargin()
}

private fun generateArmorTemplate(platform: String, name: String): String {
    if (platform != "fabric") return "// $platform 暂只支持 Fabric 完整模板"
    val lower = name.lowercase()
    return """
        |=== 护甲材料（src/main/java/com/example/mod/${name}ArmorMaterial.java）===
        |package com.example.mod;
        |
        |import net.minecraft.item.ArmorItem;
        |import net.minecraft.item.ArmorMaterial;
        |import net.minecraft.recipe.Ingredient;
        |import net.minecraft.registry.Registries;
        |import net.minecraft.registry.Registry;
        |import net.minecraft.registry.entry.RegistryEntry;
        |import net.minecraft.sound.SoundEvents;
        |import net.minecraft.util.Identifier;
        |
        |public class ${name}ArmorMaterial {
        |    public static final RegistryEntry<ArmorMaterial> INSTANCE =
        |        Registry.registerReference(Registries.ARMOR_MATERIAL,
        |            Identifier.of("<modid>", "$lower"),
        |            new ArmorMaterial(
        |                // 各部位护甲值: 头盔/胸甲/护腿/靴子
        |                new int[]{3, 6, 5, 3},
        |                // 附魔能力
        |                15,
        |                SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND,
        |                () -> Ingredient.ofItems(/* 修复物品 */),
        |                List.of(new ArmorMaterial.Layer(Identifier.of("<modid>", "$lower"))),
        |                0f, 0f));
        |}
        |
        |=== 注册示例（胸甲）===
        |// Registry.register(Registries.ITEM, Identifier.of("<modid>", "${lower}_chestplate"),
        |//     new ArmorItem(${name}ArmorMaterial.INSTANCE, ArmorItem.Type.CHESTPLATE,
        |//         new Item.Settings().maxDamage(ArmorItem.Type.CHESTPLATE.getMaxDamage(15))));
        |
        |=== 护甲贴图层目录 ===
        |assets/<modid>/textures/models/armor/${lower}_layer_1.png
        |assets/<modid>/textures/models/armor/${lower}_layer_2.png
    """.trimMargin()
}
