package de.jpx3.intave.packet.converter;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;

public final class PlayerActionResolver {

  public static PlayerAction resolveActionFromPacket(ProtocolPacketEvent event) {
    WrapperPlayClientEntityAction.Action action =
      new WrapperPlayClientEntityAction((PacketReceiveEvent) event).getAction();
    if (action == null) {
      return null;
    }
    switch (action) {
      case START_SNEAKING:
        return PlayerAction.START_SNEAKING;
      case STOP_SNEAKING:
        return PlayerAction.STOP_SNEAKING;
      case LEAVE_BED:
        return PlayerAction.STOP_SLEEPING;
      case START_SPRINTING:
        return PlayerAction.START_SPRINTING;
      case STOP_SPRINTING:
        return PlayerAction.STOP_SPRINTING;
      case START_JUMPING_WITH_HORSE:
        return PlayerAction.START_RIDING_JUMP;
      case STOP_JUMPING_WITH_HORSE:
        return PlayerAction.STOP_RIDING_JUMP;
      case OPEN_HORSE_INVENTORY:
        return PlayerAction.OPEN_INVENTORY;
      case START_FLYING_WITH_ELYTRA:
        return PlayerAction.START_FALL_FLYING;
      default:
        return null;
    }
  }
}
