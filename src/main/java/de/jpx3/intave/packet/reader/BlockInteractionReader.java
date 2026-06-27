package de.jpx3.intave.packet.reader;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import de.jpx3.intave.adapter.MinecraftVersions;
import de.jpx3.intave.annotate.Nullable;
import de.jpx3.intave.share.Direction;
import de.jpx3.intave.user.User;
import org.bukkit.util.Vector;

public final class BlockInteractionReader extends BlockPositionReader {
  private final boolean HAS_SEQUENCE_NUMBER = MinecraftVersions.VER1_19_2.atOrAbove();

  private boolean isBlockPlacement() {
    return event().getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT;
  }

  private WrapperPlayClientPlayerBlockPlacement placement() {
    return new WrapperPlayClientPlayerBlockPlacement((PacketReceiveEvent) event());
  }

  @Nullable
  public Direction direction() {
    int direction = enumDirection();
    if (direction == 255) {
      return null;
    }
    return Direction.values()[direction];
  }

  @Nullable
  public Vector facingVector() {
    if (!isBlockPlacement()) {
      return null;
    }
    Vector3f cursor = placement().getCursorPosition();
    if (cursor == null) {
      return null;
    }
    return new Vector(cursor.getX(), cursor.getY(), cursor.getZ());
  }

  public int enumDirection() {
    if (!isBlockPlacement()) {
      return 255;
    }
    BlockFace face = placement().getFace();
    // getFaceValue() yields the Minecraft wire order (0=DOWN..5=EAST), matching the index space
    // share/Direction.values() was previously addressed with via ProtocolLib's Direction#ordinal.
    return face == null ? 255 : face.getFaceValue();
  }

  private int sequenceNumber = 0;
  private boolean hasArtificialSequenceNumber = false;

  public int sequenceNumber(User user) {
    if (HAS_SEQUENCE_NUMBER && isBlockPlacement()) {
      return placement().getSequence();
    } else if (hasArtificialSequenceNumber) {
      return sequenceNumber;
    } else {
      hasArtificialSequenceNumber = true;
      return sequenceNumber = user.meta().connection().simulatedBlockAckNum++;
    }
  }

  @Override
  public void release() {
    sequenceNumber = 0;
    hasArtificialSequenceNumber = false;
    super.release();
  }
}
