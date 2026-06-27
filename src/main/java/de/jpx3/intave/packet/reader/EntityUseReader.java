package de.jpx3.intave.packet.reader;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity.InteractAction;

public final class EntityUseReader extends EntityReader {

  public boolean isAttackPacket() {
    return useAction() == InteractAction.ATTACK;
  }

  public boolean isSecondary() {
    return useAction() == InteractAction.INTERACT_AT;
  }

  public InteractAction useAction() {
    // On 1.21.5+ the dedicated ATTACK packet carries no action enum.
    if (event().getPacketType() == PacketType.Play.Client.ATTACK) {
      return InteractAction.ATTACK;
    }
    return new WrapperPlayClientInteractEntity((PacketReceiveEvent) event()).getAction();
  }
}
