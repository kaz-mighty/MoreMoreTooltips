package com.flier268.more_tooltips;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.block.ComposterBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ToolItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.BaseText;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class TooltipEventHandler {
    private static DecimalFormat Formatter = new DecimalFormat("###.#");

    private static List<Text> splitToolTip(TextRenderer renderer, String text, int maxWidth) {
        return splitToolTip(renderer, text, maxWidth, null);
    }

    private static List<Text> splitToolTip(TextRenderer renderer, String text, int maxWidth, Style style) {
        List<Text> output = new ArrayList<Text>();
        int width = renderer.getWidth(text);
        if (width > maxWidth) {
            int skipEnd = 0;
            int added = 0;
            while (true) {
                int lastSpaceIndex = text.lastIndexOf(" ", text.length() - skipEnd - 1);
                if (added <= lastSpaceIndex) {
                    String textPart = text.substring(added, lastSpaceIndex);
                    int textPartWidth = renderer.getWidth(textPart);
                    if (textPartWidth <= maxWidth || textPart.indexOf(" ") == -1) {
                        output.add(TrySetStyle(new LiteralText(textPart), style));
                        added += textPart.length() + 1;
                        skipEnd = 0;
                    } else {
                        skipEnd = text.length() - lastSpaceIndex;
                    }
                } else {
                    output.add(TrySetStyle(new LiteralText(text.substring(added, text.length())), style));
                    break;
                }
            }
        } else {
            output.add(TrySetStyle(new LiteralText(text), style));
        }
        return output;
    }

    private static String LimitStringLength(String source, int maxLength) {
        if (source.length() > maxLength)
            return source.substring(0, maxLength) + "...";
        else
            return source;
    }

    private static Text TrySetStyle(BaseText text, Style style) {
        if (style != null)
            return text.setStyle(style);
        return text;
    }

    public static void addMoreTooltip() {
        ItemTooltipCallback.EVENT.register((itemStack, tooltipContext, list) -> {
            ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
            if (!config.isEnable)
                return;

            boolean isShiftDown = Screen.hasShiftDown();

            Style DARK_GRAY = Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.DARK_GRAY));
            Style AQUA = Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.AQUA));
            // Retrieve the ItemStack and Item

            // If item stack empty do nothing
            if (itemStack.isEmpty()) {
                return;
            }
            Item item = itemStack.getItem();
            Identifier itemId = Registry.ITEM.getKey(item).get().getValue();

            var clientInstance = MinecraftClient.getInstance();
            int threshold = clientInstance.getWindow().getScaledWidth() / 2;
            if (clientInstance.currentScreen == null || clientInstance.currentScreen instanceof TitleScreen
                    || clientInstance.currentScreen instanceof DownloadingTerrainScreen)
                return;

            // Tooltip - Burn Time
            if (config.BurnTime.isShown(isShiftDown, config.debug)) {
                Integer burnTime = FuelRegistry.INSTANCE.get(item);
                if (burnTime != null && burnTime > 0) {
                    String string = new TranslatableText("tooltip.more_tooltips.burnTime", burnTime).getString();
                    string = LimitStringLength(string, config.TextMaxLength);
                    list.addAll(splitToolTip(clientInstance.textRenderer, string, threshold, DARK_GRAY));
                }
            }

            // Tooltip - MiningLevel
            if (config.MiningLevel.isShown(isShiftDown, config.debug)) {
                if (item instanceof ToolItem) {
                    int miningLevel = ((ToolItem) item).getMaterial().getMiningLevel();
                    String string = new TranslatableText("tooltip.more_tooltips.MiningLevel", miningLevel).getString();
                    string = LimitStringLength(string, config.TextMaxLength);
                    list.addAll(splitToolTip(clientInstance.textRenderer, string, threshold));
                }
            }

            // Tooltip - MiningSpeed
            if (config.MiningSpeed.isShown(isShiftDown, config.debug)) {
                if (item instanceof ToolItem) {
                    float miningSpeed = ((ToolItem) item).getMaterial().getMiningSpeedMultiplier();
                    String string = new TranslatableText("tooltip.more_tooltips.MiningSpeed",
                            Formatter.format(miningSpeed)).getString();
                    string = LimitStringLength(string, config.TextMaxLength);
                    list.addAll(splitToolTip(clientInstance.textRenderer, string, threshold));
                }
            }

            // Tooltip - Durability
            if (config.Durability.isShown(isShiftDown, config.debug)) {
                int maxDamage = itemStack.getMaxDamage();
                int currentDamage = maxDamage - itemStack.getDamage();
                if (maxDamage > 0) {
                    String string = new TranslatableText("tooltip.more_tooltips.durability", currentDamage, maxDamage)
                            .getString();
                    string = LimitStringLength(string, config.TextMaxLength);
                    list.addAll(splitToolTip(clientInstance.textRenderer, string, threshold));
                }
            }

            // Tooltip - Hunger / Saturation
            if (config.Food.isShown(isShiftDown, config.debug)) {
                if (itemStack.isFood()) {
                    FoodComponent foodComponent = item.getFoodComponent();
                    int healVal = foodComponent.getHunger();
                    float satVal = healVal * (foodComponent.getSaturationModifier()) * 2;
                    String string = new TranslatableText("tooltip.more_tooltips.hunger", healVal,
                            Formatter.format(satVal)).getString();
                    string = LimitStringLength(string, config.TextMaxLength);
                    list.addAll(splitToolTip(clientInstance.textRenderer, string, threshold, DARK_GRAY));
                }
            }

            // Tooltip - NBT Data
            if (config.NBT.isShown(isShiftDown, config.debug)) {
                NbtCompound nbtData = itemStack.getNbt();
                if (nbtData != null) {
                    String string = new TranslatableText("tooltip.more_tooltips.nbtTagData", nbtData.asString())
                            .getString();
                    string = LimitStringLength(string, config.TextMaxLength);
                    list.addAll(splitToolTip(clientInstance.textRenderer, string, threshold, DARK_GRAY));
                }
            }

            // Tooltip - Registry Name
            if (config.ID.isShown(isShiftDown, config.debug)) {
                String string = new TranslatableText("tooltip.more_tooltips.registryName",
                        Registry.ITEM.getId(item).toString()).getString();
                string = LimitStringLength(string, config.TextMaxLength);
                list.addAll(splitToolTip(clientInstance.textRenderer, string, threshold, DARK_GRAY));
            }

            // Tooltip - Max Stack Size
            if (config.MaxStackSize.isShown(isShiftDown, config.debug)) {
                if (itemStack.isStackable()) {
                    String string = new TranslatableText("tooltip.more_tooltips.maxStackSize", itemStack.getMaxCount())
                            .getString();
                    string = LimitStringLength(string, config.TextMaxLength);
                    list.addAll(splitToolTip(clientInstance.textRenderer, string, threshold, DARK_GRAY));
                }
            }

            // Tooltip - Translation Key
            if (config.TranslationKey.isShown(isShiftDown, config.debug)) {
                String string = new TranslatableText("tooltip.more_tooltips.translationKey",
                        itemStack.getTranslationKey()).getString();
                string = LimitStringLength(string, config.TextMaxLength);
                list.addAll(splitToolTip(clientInstance.textRenderer, string, threshold, DARK_GRAY));
            }

            // Tooltip - Repair Cost
            if (config.RepairCost.isShown(isShiftDown, config.debug)) {
                int repairCost = itemStack.getRepairCost();
                if (repairCost > 0 || itemStack.isDamageable()) {
                    String string = new TranslatableText("tooltip.more_tooltips.RepairCost", repairCost).getString();
                    string = LimitStringLength(string, config.TextMaxLength);
                    list.addAll(splitToolTip(clientInstance.textRenderer, string, threshold, DARK_GRAY));
                }
            }
            // Tooltip - Enchantability
            if (config.Enchantability.isShown(isShiftDown, config.debug)) {
                if (itemStack.isEnchantable()) {
                    String string = new TranslatableText("tooltip.more_tooltips.Enchantability",
                            item.getEnchantability()).getString();
                    string = LimitStringLength(string, config.TextMaxLength);
                    list.addAll(splitToolTip(clientInstance.textRenderer, string, threshold, DARK_GRAY));
                }
            }

            // Tooltip - Light level
            if (config.LightLevel.isShown(isShiftDown, config.debug)) {
                int luminance = Registry.BLOCK.get(itemId).getDefaultState().getLuminance();
                if (luminance > 0) {
                    String string = new TranslatableText("tooltip.more_tooltips.LightLevel", luminance).getString();
                    string = LimitStringLength(string, config.TextMaxLength);
                    list.addAll(splitToolTip(clientInstance.textRenderer, string, threshold, DARK_GRAY));
                }
            }

            // Tooltip - Composting chance
            if (config.CompostingChance.isShown(isShiftDown, config.debug)) {
                float chance = ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.getFloat(item);

                if (chance > 0.0) {
                    String string = new TranslatableText("tooltip.more_tooltips.CompostingChance",
                            Formatter.format(chance * 100)).getString();
                    string = LimitStringLength(string, config.TextMaxLength);
                    list.addAll(splitToolTip(clientInstance.textRenderer, string, threshold, DARK_GRAY));
                }
            }
            
            // Tooltip - Bee Nest / Beehive information
            if (config.BeeNest.isShown(isShiftDown, config.debug)) {
                if (item == Items.BEE_NEST || item == Items.BEEHIVE) {
                    NbtCompound nbtData1 = itemStack.getNbt();
                    if (nbtData1 != null) {
                        // honey level
                        // Data type is string (when survival) or int (when creative).
                        // Also, when pick block in creative, it doesn't exist.
                        NbtCompound nbtData2 = nbtData1.getCompound("BlockStateTag");
                        if (nbtData2 != null && nbtData2.contains("honey_level")) {
                            String string = new TranslatableText("tooltip.more_tooltips.HoneyLevel", 
                                    nbtData2.get("honey_level").asString()).getString();
                            string = LimitStringLength(string, config.TextMaxLength);
                            list.addAll(splitToolTip(clientInstance.textRenderer, string, threshold, DARK_GRAY));
                        }
                        // bees num
                        NbtCompound nbtDate3 = nbtData1.getCompound("BlockEntityTag");
                        if (nbtDate3 != null) {
                            String string = new TranslatableText("tooltip.more_tooltips.BeeCount",
                                    nbtDate3.getList("Bees", NbtElement.COMPOUND_TYPE).size()).getString();
                            string = LimitStringLength(string, config.TextMaxLength);
                            list.addAll(splitToolTip(clientInstance.textRenderer, string, threshold, DARK_GRAY));
                        }
                    }
                }
            }

            if (isShiftDown && config.debug) {
                String string = new LiteralText("Debugging").getString();
                list.addAll(splitToolTip(clientInstance.textRenderer, string, threshold, AQUA));
            }
        });
    }
}
