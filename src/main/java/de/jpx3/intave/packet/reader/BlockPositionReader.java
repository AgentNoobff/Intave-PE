package de.jpx3.intave.packet.reader;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

public class BlockPositionReader extends AbstractPacketReader {

  public de.jpx3.intave.share.BlockPosition nativeBlockPosition() {
    Vector3i blockPosition = blockPosition();
    if (blockPosition == null) {
      return null;
    }
    return new de.jpx3.intave.share.BlockPosition(
      blockPosition.getX(), blockPosition.getY(), blockPosition.getZ()
    );
  }

  public Vector3i blockPosition() {
    PacketTypeCommon type = event().getPacketType();
    if (type == PacketType.Play.Client.PLAYER_DIGGING) {
      return new WrapperPlayClientPlayerDigging((PacketReceiveEvent) event()).getBlockPosition();
    } else if (type == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
      return new WrapperPlayClientPlayerBlockPlacement((PacketReceiveEvent) event()).getBlockPosition();
    }
    return null;
  }
}
