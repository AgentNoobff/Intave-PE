package de.jpx3.intave.packet.reader;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.potion.PotionType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEffect;

public final class EntityEffectReader extends EntityReader {

  private WrapperPlayServerEntityEffect effect() {
    return new WrapperPlayServerEntityEffect((PacketSendEvent) event());
  }

  public int effectType() {
    PotionType potionType = effect().getPotionType();
    return potionType == null ? 0 : potionType.getId(event().getClientVersion());
  }

  public int effectAmplifier() {
    return effect().getEffectAmplifier();
  }

  public int effectDuration() {
    return effect().getEffectDurationTicks();
  }
}
