package icu.azim.wagrapple;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import icu.azim.wagrapple.blocks.DungeonBlock;
import icu.azim.wagrapple.components.GrappledPlayerComponent;
import icu.azim.wagrapple.entity.GrappleLineEntity;
import icu.azim.wagrapple.item.GrappleItem;
import icu.azim.wagrapple.item.enchantments.BoostPowerEnchantment;
import icu.azim.wagrapple.item.enchantments.RopeLengthEnchantment;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.registry.Registry;


public class WAGrappleMod implements ModInitializer{
    public static final String modid = "wagrapple";
    public static double maxLength = 24;

    //TODO sort all that stuff so it looks more presentable
    public static EntityType<GrappleLineEntity> GRAPPLE_LINE;

    public static ItemGroup ITEM_GROUP;

    public static GrappleItem GRAPPLE_ITEM;

    public static Enchantment LINE_LENGTH_ENCHANTMENT;
    public static Enchantment BOOST_POWER_ENCHANTMENT;

    public static ComponentKey<GrappledPlayerComponent> GRAPPLE_COMPONENT;

    public static Block DUNGEON_BLOCK;

    public static Identifier DETACH_LINE_PACKET_ID = new Identifier(modid, "detach_line");
    public static Identifier UPDATE_LINE_PACKET_ID = new Identifier(modid, "update_line");
    public static Identifier CREATE_LINE_PACKET_ID = new Identifier(modid, "create_line");
    public static Identifier UPDATE_LINE_LENGTH_PACKET_ID = new Identifier(modid, "update_line_length");

    public static Identifier LINE_LENGTH_ENCHANTMENT_ID = new Identifier(modid, "rope_length");
    public static Identifier BOOST_POWER_ENCHANTMENT_ID = new Identifier(modid, "boost_power");

    public static Identifier DUNGEON_BLOCK_ID = new Identifier(modid, "dungeon_block");

    @Override
    public void onInitialize() {
        generateDefaultConfig();

        ITEM_GROUP = FabricItemGroupBuilder.build(new Identifier(modid, "general"), () -> new ItemStack(WAGrappleMod.GRAPPLE_ITEM));
        GRAPPLE_ITEM = new GrappleItem(new Item.Settings().group(ITEM_GROUP).maxCount(1).rarity(Rarity.EPIC).maxDamage(690));
        DUNGEON_BLOCK = new DungeonBlock(FabricBlockSettings.of(Material.METAL));


        LINE_LENGTH_ENCHANTMENT = Registry.register(
                Registry.ENCHANTMENT,
            LINE_LENGTH_ENCHANTMENT_ID,
            new RopeLengthEnchantment(
                Enchantment.Rarity.RARE,
                EnchantmentTarget.WEAPON, // TODO
                new EquipmentSlot[] {
                EquipmentSlot.MAINHAND,
                EquipmentSlot.OFFHAND
                }
            ));

        BOOST_POWER_ENCHANTMENT = Registry.register(
                Registry.ENCHANTMENT,
            BOOST_POWER_ENCHANTMENT_ID,
            new BoostPowerEnchantment(
                Enchantment.Rarity.RARE,
                EnchantmentTarget.WEAPON, // TODO
                new EquipmentSlot[] {
                EquipmentSlot.MAINHAND,
                EquipmentSlot.OFFHAND
                }
            ));

        GRAPPLE_COMPONENT = ComponentRegistry.getOrCreate(
                new Identifier(modid, "grapple_component"),
                GrappledPlayerComponent.class);

        GRAPPLE_LINE = Registry.register(
                Registry.ENTITY_TYPE,
                new Identifier(modid, "grapple_line"),
                FabricEntityTypeBuilder.create(
                    SpawnGroup.MISC, (EntityType.EntityFactory<GrappleLineEntity>) GrappleLineEntity::new).dimensions(EntityDimensions.fixed(0.2F, 0.2F)).build());

        Registry.register(Registry.ITEM, new Identifier(modid, "grapple"), GRAPPLE_ITEM);

        Registry.register(Registry.BLOCK, DUNGEON_BLOCK_ID , DUNGEON_BLOCK);
        Registry.register(Registry.ITEM, DUNGEON_BLOCK_ID, new BlockItem(DUNGEON_BLOCK, new Item.Settings().group(ITEM_GROUP)));

//        generateDungeonBlockPattern();

//        RESOURCE_PACK.dump();
        System.out.println("init general");
    }

    public void generateDefaultConfig() {
        Path folder = FabricLoader.getInstance().getConfigDir().resolve(modid);
        Properties config = new Properties();
        try {
            Files.createDirectories(folder);
            Path f = folder.resolve("wagrapple.properties");
            if (Files.exists(f)) {
                InputStream in = Files.newInputStream(f);
                config.load(in);
                in.close();
            }
            if (config.getProperty("maxLength") == null) {
                WAGrappleMod.maxLength = 24;
                config.setProperty("maxLength", "24.0");
                OutputStream out = Files.newOutputStream(f);
                config.store(out, null);
                out.close();
            } else{
                WAGrappleMod.maxLength = Double.valueOf(config.getProperty("maxLength"));
            }
        } catch(Exception ignored) {
            WAGrappleMod.maxLength = 24;
            ignored.printStackTrace();
        }
    }
    // TODO add resource pack support - load this after resource packs are created
}





