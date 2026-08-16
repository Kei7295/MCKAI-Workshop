package com.mckai.app.domain.workshop.template

/**
 * 模组模板代码生成引擎。
 * 移植自 ModCrafting (template-codegen.ts) 的思路：
 * 输入参数 → 直接产出可编译的 Fabric 1.21.x 文件集（Java 类 + JSON 资源 + lang 条目）。
 * 与 mc_mod_template（文本参考）不同：本引擎输出结构化文件集，AI 或 UI 可直接落盘。
 */
enum class ModTemplateType(val label: String, val description: String) {
    BLOCK("自定义方块", "带硬度/材质/战利品表的方块"),
    ITEM("自定义物品", "基础物品，可设堆叠"),
    FOOD("食物", "带营养值/饱和度的食物"),
    TOOL("工具", "自定义材料 + 镐/剑/斧/铲/锄"),
    ARMOR("护甲", "护甲材料 + 四件套注册"),
    ENTITY("实体", "生物实体 + 自然生成"),
    RECIPE("合成配方", "五种配方：有序/无序/熔炼/高炉/切石"),
    MOD_CONFIG("模组设置界面", "Cloth Config 设置界面 + 配置类");

    companion object {
        fun fromName(name: String): ModTemplateType? =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) || it.label == name }
    }
}

data class TemplateParams(
    val type: ModTemplateType,
    val modId: String,
    val packageName: String = "com.example.mod",
    val className: String,
    val displayName: String = "",
    val description: String = "",
    // BLOCK
    val blockHardness: Float = 3f,
    val blockResistance: Float = 6f,
    val requiresTool: Boolean = true,
    // ITEM
    val itemMaxCount: Int = 64,
    // FOOD
    val nutrition: Int = 4,
    val saturation: Float = 0.3f,
    // TOOL
    val toolKind: String = "pickaxe",
    val durability: Int = 512,
    val miningSpeed: Float = 6f,
    val attackDamage: Float = 3f,
    val repairItem: String = "minecraft:iron_ingot",
    // ARMOR
    val armorProtection: List<Int> = listOf(3, 6, 5, 3),
    val enchantability: Int = 15,
    // ENTITY
    val spawnWeight: Int = 12,
    val spawnMin: Int = 1,
    val spawnMax: Int = 3,
    // RECIPE
    val recipeType: String = "shaped",
    val recipePattern: List<String> = emptyList(),
    val recipeKeys: Map<String, String> = emptyMap(),
    val resultItem: String = "",
    val resultCount: Int = 1,
    val experience: Float = 0.7f,
    val cookingTime: Int = 200
) {
    /** 注册用 snake_case 名，默认由类名派生。 */
    val idName: String
        get() = className
            .replace(Regex("([a-z0-9])([A-Z])"), "$1_$2")
            .lowercase()
}

data class TemplateResult(
    val files: Map<String, String>,
    val langEntries: Map<String, String>,
    val registerSnippet: String,
    val summary: String
)

object TemplateEngine {

    fun generate(p: TemplateParams): TemplateResult = when (p.type) {
        ModTemplateType.BLOCK -> generateBlock(p)
        ModTemplateType.ITEM -> generateItem(p)
        ModTemplateType.FOOD -> generateFood(p)
        ModTemplateType.TOOL -> generateTool(p)
        ModTemplateType.ARMOR -> generateArmor(p)
        ModTemplateType.ENTITY -> generateEntity(p)
        ModTemplateType.RECIPE -> generateRecipe(p)
        ModTemplateType.MOD_CONFIG -> generateModConfig(p)
    }

    /** JSON 字符串转义（与 ModExporter.jstr 一致）。 */
    private fun jstr(s: String): String = buildString {
        append('"')
        for (c in s) {
            when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                '\b' -> append("\\b")
                else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
            }
        }
        append('"')
    }

    private fun pkg(p: TemplateParams) = p.packageName
    private fun cls(p: TemplateParams) = p.className
    private fun id(p: TemplateParams) = p.idName
    private fun modId(p: TemplateParams) = p.modId.lowercase()
    private fun displayName(p: TemplateParams) = p.displayName.ifBlank { p.className }

    private fun itemModelJson(p: TemplateParams) =
        """{"parent": "minecraft:item/generated", "textures": {"layer0": "${modId(p)}:item/${id(p)}"}}"""

    // ---------- BLOCK ----------
    private fun generateBlock(p: TemplateParams): TemplateResult {
        val files = linkedMapOf<String, String>()
        files["src/main/java/${pkg(p).replace('.', '/')}/${cls(p)}Block.java"] = """
            |package ${pkg(p)};
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
            |public class ${cls(p)}Block extends Block {
            |    public ${cls(p)}Block() {
            |        super(AbstractBlock.Settings.create()
            |            .mapColor(MapColor.STONE_GRAY)
            |            .strength(${p.blockHardness}f, ${p.blockResistance}f)
            |            ${if (p.requiresTool) ".requiresTool()" else ""});
            |    }
            |
            |    public static final Block INSTANCE = new ${cls(p)}Block();
            |    public static final Item ITEM = new BlockItem(INSTANCE, new Item.Settings());
            |
            |    public static void register() {
            |        Registry.register(Registries.BLOCK, Identifier.of("${modId(p)}", "${id(p)}"), INSTANCE);
            |        Registry.register(Registries.ITEM, Identifier.of("${modId(p)}", "${id(p)}"), ITEM);
            |    }
            |}
        """.trimMargin() + "\n"
        files["src/main/resources/assets/${modId(p)}/blockstates/${id(p)}.json"] =
            """{"variants": {"": {"model": "${modId(p)}:block/${id(p)}"}}}"""
        files["src/main/resources/assets/${modId(p)}/models/block/${id(p)}.json"] =
            """{"parent": "minecraft:block/cube_all", "textures": {"all": "${modId(p)}:block/${id(p)}"}}"""
        files["src/main/resources/assets/${modId(p)}/models/item/${id(p)}.json"] =
            """{"parent": "${modId(p)}:block/${id(p)}"}"""
        files["src/main/resources/data/${modId(p)}/loot_table/blocks/${id(p)}.json"] =
            """{"type": "minecraft:block", "pools": [{"rolls": 1.0, "entries": [{"type": "minecraft:item", "name": "${modId(p)}:${id(p)}"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]}"""
        val lang = linkedMapOf<String, String>()
        lang["block.${modId(p)}.${id(p)}"] = displayName(p)
        lang["item.${modId(p)}.${id(p)}"] = displayName(p)
        val snippet = "${cls(p)}Block.register(); // 在模组主类的 onInitialize 中调用"
        return TemplateResult(files, lang, snippet,
            "方块 ${displayName(p)}：1 个 Java 类 + 4 个资源文件 + 战利品表。在模组主类调用 ${cls(p)}Block.register()。")
    }

    // ---------- ITEM ----------
    private fun generateItem(p: TemplateParams): TemplateResult {
        val files = linkedMapOf<String, String>()
        files["src/main/java/${pkg(p).replace('.', '/')}/${cls(p)}Item.java"] = """
            |package ${pkg(p)};
            |
            |import net.minecraft.item.Item;
            |import net.minecraft.registry.Registries;
            |import net.minecraft.registry.Registry;
            |import net.minecraft.util.Identifier;
            |
            |public class ${cls(p)}Item extends Item {
            |    public ${cls(p)}Item() {
            |        super(new Item.Settings().maxCount(${p.itemMaxCount}));
            |    }
            |
            |    public static final Item INSTANCE = new ${cls(p)}Item();
            |
            |    public static void register() {
            |        Registry.register(Registries.ITEM, Identifier.of("${modId(p)}", "${id(p)}"), INSTANCE);
            |    }
            |}
        """.trimMargin() + "\n"
        files["src/main/resources/assets/${modId(p)}/models/item/${id(p)}.json"] = itemModelJson(p)
        return TemplateResult(files,
            linkedMapOf("item.${modId(p)}.${id(p)}" to displayName(p)),
            "${cls(p)}Item.register(); // 在模组主类的 onInitialize 中调用",
            "物品 ${displayName(p)}：1 个 Java 类 + 1 个模型。调用 ${cls(p)}Item.register() 注册。")
    }

    // ---------- FOOD ----------
    private fun generateFood(p: TemplateParams): TemplateResult {
        val files = linkedMapOf<String, String>()
        files["src/main/java/${pkg(p).replace('.', '/')}/${cls(p)}Item.java"] = """
            |package ${pkg(p)};
            |
            |import net.minecraft.component.type.FoodComponent;
            |import net.minecraft.item.Item;
            |import net.minecraft.registry.Registries;
            |import net.minecraft.registry.Registry;
            |import net.minecraft.util.Identifier;
            |
            |public class ${cls(p)}Item extends Item {
            |    public ${cls(p)}Item() {
            |        super(new Item.Settings()
            |            .food(new FoodComponent.Builder()
            |                .nutrition(${p.nutrition})
            |                .saturationModifier(${p.saturation}f)
            |                .build()));
            |    }
            |
            |    public static final Item INSTANCE = new ${cls(p)}Item();
            |
            |    public static void register() {
            |        Registry.register(Registries.ITEM, Identifier.of("${modId(p)}", "${id(p)}"), INSTANCE);
            |    }
            |}
        """.trimMargin() + "\n"
        files["src/main/resources/assets/${modId(p)}/models/item/${id(p)}.json"] = itemModelJson(p)
        return TemplateResult(files,
            linkedMapOf("item.${modId(p)}.${id(p)}" to displayName(p)),
            "${cls(p)}Item.register(); // 在模组主类的 onInitialize 中调用",
            "食物 ${displayName(p)}（营养 ${p.nutrition}，饱和度 ${p.saturation}）：1 个 Java 类 + 1 个模型。")
    }

    // ---------- TOOL ----------
    private fun generateTool(p: TemplateParams): TemplateResult {
        val files = linkedMapOf<String, String>()
        files["src/main/java/${pkg(p).replace('.', '/')}/${cls(p)}Material.java"] = """
            |package ${pkg(p)};
            |
            |import net.minecraft.item.ToolMaterial;
            |import net.minecraft.recipe.Ingredient;
            |import net.minecraft.registry.Registries;
            |import net.minecraft.util.Identifier;
            |
            |public class ${cls(p)}Material implements ToolMaterial {
            |    public static final ${cls(p)}Material INSTANCE = new ${cls(p)}Material();
            |
            |    @Override public int getDurability() { return ${p.durability}; }
            |    @Override public float getMiningSpeed() { return ${p.miningSpeed}f; }
            |    @Override public float getAttackDamage() { return ${p.attackDamage}f; }
            |    @Override public int getMiningLevel() { return 3; }
            |    @Override public int getEnchantability() { return 14; }
            |    @Override public Ingredient getRepairIngredient() {
            |        return Ingredient.ofItems(Registries.ITEM.get(
            |            Identifier.ofVanilla("${p.repairItem.removePrefix("minecraft:")}")));
            |    }
            |}
        """.trimMargin() + "\n"
        val toolClass = when (p.toolKind.lowercase()) {
            "sword" -> "SwordItem"
            "axe" -> "AxeItem"
            "shovel" -> "ShovelItem"
            "hoe" -> "HoeItem"
            else -> "PickaxeItem"
        }
        files["src/main/java/${pkg(p).replace('.', '/')}/${cls(p)}Tool.java"] = """
            |package ${pkg(p)};
            |
            |import net.minecraft.item.Item;
            |import net.minecraft.item.ToolMaterial;
            |import net.minecraft.item.$toolClass;
            |import net.minecraft.registry.Registries;
            |import net.minecraft.registry.Registry;
            |import net.minecraft.util.Identifier;
            |
            |public class ${cls(p)}Tool extends $toolClass {
            |    public ${cls(p)}Tool(ToolMaterial material, float attackDamage, float attackSpeed, Settings settings) {
            |        super(material, attackDamage, attackSpeed, settings);
            |    }
            |
            |    public static final Item INSTANCE = new ${cls(p)}Tool(
            |        ${cls(p)}Material.INSTANCE, ${p.attackDamage}f, -2.8f,
            |        new Item.Settings().maxDamage(${p.durability}));
            |
            |    public static void register() {
            |        Registry.register(Registries.ITEM, Identifier.of("${modId(p)}", "${id(p)}"), INSTANCE);
            |    }
            |}
        """.trimMargin() + "\n"
        files["src/main/resources/assets/${modId(p)}/models/item/${id(p)}.json"] =
            """{"parent": "minecraft:item/handheld", "textures": {"layer0": "${modId(p)}:item/${id(p)}"}}"""
        return TemplateResult(files,
            linkedMapOf("item.${modId(p)}.${id(p)}" to displayName(p)),
            "${cls(p)}Tool.register(); // 在模组主类的 onInitialize 中调用",
            "${toolClass} 工具 ${displayName(p)}（耐久 ${p.durability}，修复物品 ${p.repairItem}）：材料类 + 工具类 + 模型。")
    }

    // ---------- ARMOR ----------
    private fun generateArmor(p: TemplateParams): TemplateResult {
        val files = linkedMapOf<String, String>()
        val protection = p.armorProtection.joinToString(", ")
        files["src/main/java/${pkg(p).replace('.', '/')}/${cls(p)}ArmorMaterial.java"] = """
            |package ${pkg(p)};
            |
            |import net.minecraft.item.ArmorItem;
            |import net.minecraft.item.ArmorMaterial;
            |import net.minecraft.recipe.Ingredient;
            |import net.minecraft.registry.Registries;
            |import net.minecraft.registry.Registry;
            |import net.minecraft.registry.entry.RegistryEntry;
            |import net.minecraft.sound.SoundEvents;
            |import net.minecraft.util.Identifier;
            |import java.util.List;
            |
            |public class ${cls(p)}ArmorMaterial {
            |    public static final RegistryEntry<ArmorMaterial> INSTANCE =
            |        Registry.registerReference(Registries.ARMOR_MATERIAL,
            |            Identifier.of("${modId(p)}", "${id(p)}"),
            |            new ArmorMaterial(
            |                new int[]{$protection},
            |                ${p.enchantability},
            |                SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND,
            |                () -> Ingredient.ofItems(Registries.ITEM.get(
            |                    Identifier.ofVanilla("${p.repairItem.removePrefix("minecraft:")}"))),
            |                List.of(new ArmorMaterial.Layer(Identifier.of("${modId(p)}", "${id(p)}"))),
            |                0f, 0f));
            |}
        """.trimMargin() + "\n"
        val snippet = buildString {
            appendLine("// 在模组主类的 onInitialize 中注册四件套：")
            appendLine("registerArmorPiece(\"${id(p)}_helmet\", ArmorItem.Type.HELMET);")
            appendLine("registerArmorPiece(\"${id(p)}_chestplate\", ArmorItem.Type.CHESTPLATE);")
            appendLine("registerArmorPiece(\"${id(p)}_leggings\", ArmorItem.Type.LEGGINGS);")
            appendLine("registerArmorPiece(\"${id(p)}_boots\", ArmorItem.Type.BOOTS);")
            appendLine("// 辅助方法：")
            appendLine("private static void registerArmorPiece(String id, ArmorItem.Type type) {")
            appendLine("    Registry.register(Registries.ITEM, Identifier.of(\"${modId(p)}\", id),")
            appendLine("        new ArmorItem(${cls(p)}ArmorMaterial.INSTANCE, type,")
            appendLine("            new Item.Settings().maxDamage(type.getMaxDamage(15))));")
            appendLine("}")
        }
        val lang = linkedMapOf<String, String>()
        listOf("helmet" to "头盔", "chestplate" to "胸甲", "leggings" to "护腿", "boots" to "靴子").forEach { (part, zh) ->
            lang["item.${modId(p)}.${id(p)}_$part"] = "${displayName(p)}$zh"
        }
        return TemplateResult(files, lang, snippet,
            "护甲 ${displayName(p)}（防护值 [$protection]）：材料类 + 四件套注册片段 + 4 条 lang。")
    }

    // ---------- ENTITY ----------
    private fun generateEntity(p: TemplateParams): TemplateResult {
        val files = linkedMapOf<String, String>()
        files["src/main/java/${pkg(p).replace('.', '/')}/${cls(p)}Entity.java"] = """
            |package ${pkg(p)};
            |
            |import net.minecraft.entity.EntityType;
            |import net.minecraft.entity.SpawnGroup;
            |import net.minecraft.entity.mob.PathAwareEntity;
            |import net.minecraft.registry.Registries;
            |import net.minecraft.registry.Registry;
            |import net.minecraft.util.Identifier;
            |import net.minecraft.world.World;
            |
            |public class ${cls(p)}Entity extends PathAwareEntity {
            |    public ${cls(p)}Entity(EntityType<? extends PathAwareEntity> type, World world) {
            |        super(type, world);
            |    }
            |
            |    public static final EntityType<${cls(p)}Entity> TYPE = EntityType.Builder
            |        .create(${cls(p)}Entity::new, SpawnGroup.CREATURE)
            |        .dimensions(0.6f, 1.8f)
            |        .build();
            |
            |    public static void register() {
            |        Registry.register(Registries.ENTITY_TYPE,
            |            Identifier.of("${modId(p)}", "${id(p)}"), TYPE);
            |    }
            |}
        """.trimMargin() + "\n"
        val snippet = buildString {
            appendLine("${cls(p)}Entity.register(); // 在模组主类的 onInitialize 中调用")
            appendLine("// 自然生成（Fabric API）：")
            appendLine("FabricDefaultBiomeModifications.addSpawn(")
            appendLine("    SpawnContext.of(SpawnGroup.CREATURE),")
            appendLine("    SpawnRestriction.Location.ON_GROUND,")
            appendLine("    TrackedEntity.create(${p.spawnWeight}, ${p.spawnMin}, ${p.spawnMax}),")
            appendLine("    (biome, context) -> true);")
        }
        return TemplateResult(files,
            linkedMapOf("entity.${modId(p)}.${id(p)}" to displayName(p)),
            snippet,
            "实体 ${displayName(p)}（生成权重 ${p.spawnWeight}，${p.spawnMin}-${p.spawnMax} 只/群）：实体类 + 生成配置。")
    }

    // ---------- RECIPE ----------
    private fun generateRecipe(p: TemplateParams): TemplateResult {
        val result = p.resultItem.ifBlank { "${modId(p)}:${id(p)}" }
        val json = RecipeGenerator.generate(
            type = p.recipeType,
            pattern = p.recipePattern,
            keys = p.recipeKeys,
            resultItem = result,
            resultCount = p.resultCount,
            experience = p.experience,
            cookingTime = p.cookingTime
        )
        val files = linkedMapOf<String, String>()
        files["src/main/resources/data/${modId(p)}/recipe/${id(p)}.json"] = json
        return TemplateResult(files, emptyMap(),
            "// 配方已生成，无需注册代码",
            "${p.recipeType} 配方：产物 $result ×${p.resultCount}，文件 data/${modId(p)}/recipe/${id(p)}.json。")
    }

    // ---------- MOD CONFIG ----------
    private fun generateModConfig(p: TemplateParams): TemplateResult {
        val files = linkedMapOf<String, String>()
        files["src/main/java/${pkg(p).replace('.', '/')}/${cls(p)}Config.java"] = """
            |package ${pkg(p)};
            |
            |import me.shedaniel.autoconfig.AutoConfig;
            |import me.shedaniel.autoconfig.ConfigData;
            |import me.shedaniel.autoconfig.annotation.Config;
            |
            |@Config(name = "${modId(p)}")
            |public class ${cls(p)}Config implements ConfigData {
            |    public boolean enabled = true;
            |    public int someValue = 42;
            |    public String message = "Hello from ${modId(p)}";
            |
            |    public static ${cls(p)}Config get() {
            |        return AutoConfig.getConfigHolder(${cls(p)}Config.class).getConfig();
            |    }
            |}
        """.trimMargin() + "\n"
        files["src/main/java/${pkg(p).replace('.', '/')}/${cls(p)}ConfigScreen.java"] = """
            |package ${pkg(p)};
            |
            |import me.shedaniel.autoconfig.AutoConfig;
            |import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
            |import me.shedaniel.clothconfig2.api.ConfigBuilder;
            |import net.minecraft.client.gui.screen.Screen;
            |import net.minecraft.text.Text;
            |
            |public class ${cls(p)}ConfigScreen {
            |    public static Screen create(Screen parent) {
            |        return AutoConfig.getConfigScreen(${cls(p)}Config.class, parent).get();
            |    }
            |}
        """.trimMargin() + "\n"
        val snippet = buildString {
            appendLine("// 1. 注册配置（模组主类 onInitialize）：")
            appendLine("AutoConfig.register(${cls(p)}Config.class, me.shedaniel.autoconfig.serializer.JanksonConfigSerializer::new);")
            appendLine("// 2. 打开设置界面（客户端按键或 ModMenu 入口）：")
            appendLine("//    client.getCurrentScreen() -> ${cls(p)}ConfigScreen.create(parent)")
            appendLine("// 3. build.gradle 需添加依赖：")
            appendLine("//    modImplementation(\"me.shedaniel.cloth:cloth-config-fabric:15.0.140\")")
        }
        return TemplateResult(files,
            linkedMapOf("text.autoconfig.${modId(p)}.title" to "${displayName(p)} 设置"),
            snippet,
            "设置界面：配置类 + 界面类（Cloth Config API），需在 build.gradle 添加 cloth-config-fabric 依赖。")
    }
}