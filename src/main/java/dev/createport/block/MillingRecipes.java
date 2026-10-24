package dev.createport.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public final class MillingRecipes {
    private static final List<Recipe> RECIPES = new ArrayList<>();
    public static void load() {
        RECIPES.clear();
        try (InputStreamReader reader = new InputStreamReader(MillingRecipes.class.getResourceAsStream("/assets/createport/gameplay/milling.json"), StandardCharsets.UTF_8)) {
            JsonArray array = new JsonParser().parse(reader).getAsJsonArray();
            for (JsonElement element : array) {
                JsonObject data = element.getAsJsonObject();
                List<ItemStack> outputs = new ArrayList<>(); List<Double> chances = new ArrayList<>();
                for (JsonElement value : data.getAsJsonArray("results")) {
                    JsonObject output = value.getAsJsonObject();
                    outputs.add(stack(output, output.get("count").getAsInt())); chances.add(output.get("chance").getAsDouble());
                }
                RECIPES.add(new Recipe(stack(data.getAsJsonObject("input"), 1), data.get("time").getAsInt(), outputs, chances));
            }
        } catch (Exception e) { throw new IllegalStateException("Cannot load legacy milling recipes", e); }
    }
    private static ItemStack stack(JsonObject object, int count) {
        ResourceLocation id = new ResourceLocation(object.get("item").getAsString());
        if (!ForgeRegistries.ITEMS.containsKey(id)) throw new IllegalArgumentException("Missing milling item " + id);
        Item item = ForgeRegistries.ITEMS.getValue(id);
        return new ItemStack(item, count, object.get("meta").getAsInt());
    }
    public static Recipe find(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (Recipe recipe : RECIPES)
            if (recipe.input.getItem() == stack.getItem() && (recipe.input.getMetadata() == 32767 || recipe.input.getMetadata() == stack.getMetadata())) return recipe;
        return null;
    }
    public static int count() { return RECIPES.size(); }
    public static final class Recipe {
        final ItemStack input; final int time; final List<ItemStack> outputs; final List<Double> chances;
        Recipe(ItemStack input, int time, List<ItemStack> outputs, List<Double> chances) {
            this.input = input; this.time = time; this.outputs = outputs; this.chances = chances;
        }
        List<ItemStack> roll(Random random) {
            List<ItemStack> results = new ArrayList<>();
            for (int i = 0; i < outputs.size(); i++) if (random.nextFloat() < chances.get(i)) results.add(outputs.get(i).copy());
            return results;
        }
    }
}
