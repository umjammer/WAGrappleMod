package icu.azim.wagrapple.render;

import icu.azim.wagrapple.WAGrappleMod;
import net.fabricmc.fabric.api.event.client.ClientSpriteRegistryCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;

public class CustomSprites {

    @Deprecated
    public static void init() {
        ClientSpriteRegistryCallback
                .event(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE)
                .register(CustomSprites::registerSprites);
    }

    public static Sprite getBlockSprite(Identifier id) {
        if(id.equals(WAGrappleMod.DUNGEON_BLOCK_ID)) {
        }
        Sprite sprite = MinecraftClient.getInstance().getSpriteAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE).apply(id);

        return sprite;
    }

    private static void registerSprites(SpriteAtlasTexture texture, ClientSpriteRegistryCallback.Registry registry) {
        registry.register(WAGrappleMod.DUNGEON_BLOCK_ID);
    }
}
