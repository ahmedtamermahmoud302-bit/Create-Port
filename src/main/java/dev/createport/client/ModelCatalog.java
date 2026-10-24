package dev.createport.client;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelBlockDefinition;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.EnumFacing;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;

/** Makes the converted block geometry available to current and future block renderers. */
public final class ModelCatalog {
    public static final ModelCatalog INSTANCE = new ModelCatalog();
    private final Map<ResourceLocation, IModel> unbaked = new LinkedHashMap<>();
    private final Map<ResourceLocation, IBakedModel> baked = new LinkedHashMap<>();
    private final Map<String, String> failures = new LinkedHashMap<>();
    private int abstractCount;

    static JsonObject readCatalog(IResourceManager manager) {
        try (IResource resource = manager.getResource(new ResourceLocation("createport", "render_catalog.json"));
             InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            return new JsonParser().parse(reader).getAsJsonObject();
        } catch (FileNotFoundException earlyRegistration) {
            // ModelRegistryEvent registers loaders before FML installs mod resource packs.
            try (InputStream stream = ModelCatalog.class.getResourceAsStream("/assets/createport/render_catalog.json")) {
                if (stream == null) throw new FileNotFoundException("Bundled render catalog");
                return new JsonParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (Exception e) { throw new IllegalStateException("Cannot read bundled model catalog", e); }
        } catch (Exception e) { throw new IllegalStateException("Cannot read converted model catalog", e); }
    }

    @SubscribeEvent public void textures(TextureStitchEvent.Pre event) {
        if (event.getMap() != Minecraft.getMinecraft().getTextureMapBlocks()) return;
        unbaked.clear(); baked.clear(); failures.clear();
        JsonObject catalog = readCatalog(Minecraft.getMinecraft().getResourceManager());
        Set<String> templates = new HashSet<>();
        for (JsonElement value : catalog.getAsJsonArray("abstract_models")) templates.add(value.getAsString());
        abstractCount = templates.size();
        for (JsonElement value : catalog.getAsJsonArray("blockstates")) {
            ResourceLocation location = new ResourceLocation("create", "blockstates/" + value.getAsString() + ".json");
            try (IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(location);
                 InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                ModelBlockDefinition.parseFromReader(reader, location);
            } catch (Exception e) { failures.put("blockstate:" + value.getAsString(), e.toString()); }
        }
        for (JsonElement value : catalog.getAsJsonArray("models")) {
            ResourceLocation location = new ResourceLocation(value.getAsString());
            if (!location.getResourcePath().startsWith("block/")) continue;
            if (templates.contains(location.toString())) continue;
            try {
                IModel model = ModelLoaderRegistry.getModel(location);
                unbaked.put(location, model);
                for (ResourceLocation texture : model.getTextures()) event.getMap().registerSprite(texture);
            } catch (Exception e) { failures.put(location.toString(), e.toString()); }
        }
    }

    @SubscribeEvent public void bake(ModelBakeEvent event) {
        Map<String, Integer> missingSprites = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, IModel> entry : unbaked.entrySet()) {
            try {
                IBakedModel model = entry.getValue().bake(entry.getValue().getDefaultState(), DefaultVertexFormats.ITEM,
                    texture -> Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(texture.toString()));
                baked.put(entry.getKey(), model);
                int missing = 0;
                for (int face = -1; face < 6; face++) {
                    for (BakedQuad quad : model.getQuads(null, face < 0 ? null : EnumFacing.values()[face], 0))
                        if (quad.getSprite().getIconName().contains("missingno")) missing++;
                }
                if (missing > 0) missingSprites.put(entry.getKey().toString(), missing);
            } catch (Exception e) { failures.put(entry.getKey().toString(), e.toString()); }
        }
        JsonObject report = new JsonObject();
        report.addProperty("loaded", unbaked.size());
        report.addProperty("abstract_templates", abstractCount);
        report.addProperty("baked", baked.size());
        report.add("failures", new GsonBuilder().create().toJsonTree(failures));
        report.add("models_with_missing_sprites", new GsonBuilder().create().toJsonTree(missingSprites));
        try {
            Files.write(Paths.get("model-audit.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) { LogManager.getLogger("CreatePort").error("Cannot save model audit", e); }
        LogManager.getLogger("CreatePort").info("Converted block model bake: {} baked, {} failures", baked.size(), failures.size());
    }

    public IBakedModel get(String path) { return baked.get(new ResourceLocation("create", "block/" + path)); }
}
