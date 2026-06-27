package de.jpx3.intave.share;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import de.jpx3.intave.codec.StreamCodec;
import de.jpx3.intave.packet.Relative;
import io.netty.buffer.ByteBuf;

import java.util.Set;

public final class PositionMoveRotation {
  public static final StreamCodec<ByteBuf, ByteBuf, PositionMoveRotation> STREAM_CODEC = StreamCodec.compound(
    Position.STREAM_CODEC,
    PositionMoveRotation::position,
    Motion.STREAM_CODEC,
    PositionMoveRotation::motion,
    Rotation.STREAM_CODEC,
    PositionMoveRotation::rotation,
    PositionMoveRotation::new
  );
  private final Position position;
  private final Motion motion;
  private final Rotation rotation;

  public PositionMoveRotation(
    Position position, Motion motion, Rotation rotation
  ) {
    this.position = position.mutable();
    this.motion = motion;
    this.rotation = rotation;
  }

  public Position position() {
    return position;
  }

  public Motion motion() {
    return motion;
  }

  public Rotation rotation() {
    return rotation;
  }

  public static PositionMoveRotation firstFrom(
    ProtocolPacketEvent event
  ) {
    WrapperPlayServerPlayerPositionAndLook wrapper =
      new WrapperPlayServerPlayerPositionAndLook((PacketSendEvent) event);
    // Pre-1.21.2 teleports carry no delta motion; PacketEvents normalizes position/rotation getters.
    return new PositionMoveRotation(
      new Position(wrapper.getX(), wrapper.getY(), wrapper.getZ()),
      Motion.newEmpty(),
      new Rotation(wrapper.getYaw(), wrapper.getPitch())
    );
  }

  // If a teleport flag is set, we use the old value and add the change to it
  // Otherwise the change is absolute, and we use it as is
  public static PositionMoveRotation merge(
    PositionMoveRotation current,
    PositionMoveRotation change,
    Set<Relative> relativeSet
  ) {
    Position keepFromOldPosition = current.position().filtered(relativeSet);
    Position newPosition = keepFromOldPosition.add(change.position()).mutable();
    Rotation keepFromOldRotation = current.rotation().filtered(relativeSet);
    Rotation newRotation = keepFromOldRotation.add(change.rotation());
    Motion keepFromOldMotion = current.motion().filtered(relativeSet);
    Motion newMotion = keepFromOldMotion.add(change.motion());
    return new PositionMoveRotation(newPosition, newMotion, newRotation);
  }

  @Override
  public String toString() {
    return "PositionMoveRotation{" +
      "position=" + position +
      ", motion=" + motion +
      ", rotation=" + rotation +
      '}';
  }
}
