package dev.createport.client;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.PerspectiveMapWrapper;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;

/** Adapts the supplied NeoForge OBJ descriptors to Forge 1.12's OBJ loader. */
public final class LegacyObjLoader implements ICustomModelLoader {
    private final Set<String> models = new HashSet<>();
    private IResourceManager resources;

    @Override public void onResourceManagerReload(IResourceManager manager) {
        resources = manager;
        models.clear();
        for (JsonElement value : ModelCatalog.readCatalog(manager).getAsJsonArray("obj_models"))
            models.add("models/" + value.getAsString());
        for (JsonElement value : ModelCatalog.readCatalog(manager).getAsJsonArray("composite_models"))
            models.add("models/" + value.getAsString());
    }

    @Override public boolean accepts(ResourceLocation location) {
        return location.getResourceDomain().equals("create") && models.contains(location.getResourcePath());
    }

    @Override public IModel loadModel(ResourceLocation location) throws Exception {
        JsonObject descriptor;
        try (IResource resource = resources.getResource(new ResourceLocation("create", location.getResourcePath() + ".json"));
             InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            descriptor = new JsonParser().parse(reader).getAsJsonObject();
        }
        ItemCameraTransforms display = ModelBlock.deserialize(descriptor.toString()).getAllTransforms();
        ImmutableMap.Builder<ItemCameraTransforms.TransformType, TRSRTransformation> transforms = ImmutableMap.builder();
        for (ItemCameraTransforms.TransformType type : ItemCameraTransforms.TransformType.values())
            transforms.put(type, new TRSRTransformation(display.getTransform(type)));
        if (descriptor.get("loader").getAsString().equals("createport:composite")) {
            List<IModel> parts = new ArrayList<>();
            List<BlockRenderLayer> layers = new ArrayList<>();
            List<ResourceLocation> dependencies = new ArrayList<>();
            for (Map.Entry<String, JsonElement> entry : descriptor.getAsJsonObject("children").entrySet()) {
                JsonObject child = entry.getValue().getAsJsonObject();
                ResourceLocation part = new ResourceLocation(child.get("model").getAsString());
                dependencies.add(part);
                parts.add(ModelLoaderRegistry.getModel(part));
                layers.add(BlockRenderLayer.valueOf(child.get("layer").getAsString()));
            }
            return new DisplayModel(new CompositeModel(parts, layers, dependencies), transforms.build());
        }
        ImmutableMap.Builder<String, String> replacements = ImmutableMap.builder();
        JsonObject textures = descriptor.getAsJsonObject("textures");
        for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
            String value = entry.getValue().getAsString();
            Set<String> visited = new HashSet<>();
            while (value.startsWith("#")) {
                if (!visited.add(value) || !textures.has(value.substring(1)))
                    throw new IllegalArgumentException("Unresolved OBJ texture " + value + " in " + location);
                value = textures.get(value.substring(1)).getAsString();
            }
            replacements.put("#" + entry.getKey(), value);
        }
        IModel obj = OBJLoader.INSTANCE.loadModel(new ResourceLocation(descriptor.get("model").getAsString()))
            .process(ImmutableMap.of("flip-v", descriptor.has("flip_v") ? descriptor.get("flip_v").getAsString() : "false"))
            .retexture(replacements.build());
        return new DisplayModel(obj, transforms.build());
    }

    private static final class CompositeModel implements IModel {
        private final List<IModel> parts;
        private final List<BlockRenderLayer> layers;
        private final List<ResourceLocation> dependencies;
        CompositeModel(List<IModel> parts, List<BlockRenderLayer> layers, List<ResourceLocation> dependencies) {
            this.parts = parts; this.layers = layers; this.dependencies = dependencies;
        }
        @Override public Collection<ResourceLocation> getDependencies() { return dependencies; }
        @Override public Collection<ResourceLocation> getTextures() {
            Set<ResourceLocation> textures = new HashSet<>();
            for (IModel part : parts) textures.addAll(part.getTextures());
            return textures;
        }
        @Override public IBakedModel bake(IModelState state, VertexFormat format,
                Function<ResourceLocation, TextureAtlasSprite> textures) {
            List<IBakedModel> models = new ArrayList<>();
            for (IModel part : parts) models.add(part.bake(state, format, textures));
            return new CompositeBakedModel(models, layers);
        }
    }

    private static final class CompositeBakedModel extends BakedModelWrapper<IBakedModel> {
        private final List<IBakedModel> parts;
        private final List<BlockRenderLayer> layers;
        CompositeBakedModel(List<IBakedModel> parts, List<BlockRenderLayer> layers) {
            super(parts.get(0)); this.parts = parts; this.layers = layers;
        }
        @Override public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long seed) {
            BlockRenderLayer current = MinecraftForgeClient.getRenderLayer();
            List<BakedQuad> result = new ArrayList<>();
            for (int i = 0; i < parts.size(); i++)
                if (current == null || current == layers.get(i)) result.addAll(parts.get(i).getQuads(state, side, seed));
            return result;
        }
    }

    private static final class DisplayModel implements IModel {
        private final IModel geometry;
        private final ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation> display;
        DisplayModel(IModel geometry, ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation> display) {
            this.geometry = geometry; this.display = display;
        }
        @Override public Collection<ResourceLocation> getDependencies() { return geometry.getDependencies(); }
        @Override public Collection<ResourceLocation> getTextures() { return geometry.getTextures(); }
        @Override public IModelState getDefaultState() { return geometry.getDefaultState(); }
        @Override public IBakedModel bake(IModelState state, VertexFormat format,
                Function<ResourceLocation, TextureAtlasSprite> textures) {
            return new PerspectiveMapWrapper(geometry.bake(state, format, textures), display);
        }
    }
}
