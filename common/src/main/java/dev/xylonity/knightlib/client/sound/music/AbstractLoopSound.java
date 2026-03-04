package dev.xylonity.knightlib.client.sound.music;

import dev.xylonity.knightlib.api.sound.music.IBossMusicProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

public class AbstractLoopSound extends AbstractTickableSoundInstance {

    private final IBossMusicProvider boss;
    private final int fadeIn, fadeOut;
    private int fadeInCounter, fadeOutCounter;
    private boolean fadingOut;

    public AbstractLoopSound(IBossMusicProvider boss, SoundSource src) {
        super(boss.getBossMusic(), src, SoundInstance.createUnseededRandom());
        this.boss = boss;
        this.looping = true;
        this.relative = true;
        this.volume = 0f;
        this.pitch = 1f;
        this.attenuation = Attenuation.NONE;
        this.fadeIn = boss.getFadeIn();
        this.fadeOut = boss.getFadeOut();
        this.fadeInCounter = fadeIn;
        this.fadeOutCounter = fadeOut;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {

        final LocalPlayer player = Minecraft.getInstance().player;

        if (player == null) {
            return;
        }

        if (!boss.shouldPlayBossMusic(player) && !fadingOut) {
            fadingOut = true;
            fadeOutCounter = fadeOut;
        }

        if (fadingOut) {
            volume = (float) fadeOutCounter / fadeOut;
            if (--fadeOutCounter <= 0) {
                stop();
            }

        }
        else if (fadeInCounter > 0) {
            volume = 1f - (float) fadeInCounter-- / fadeIn;
        }
        else {
            volume = 1f;
        }

    }

}
