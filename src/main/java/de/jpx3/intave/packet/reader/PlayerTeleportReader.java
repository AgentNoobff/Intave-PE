package de.jpx3.intave.packet.reader;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import de.jpx3.intave.packet.Relative;
import de.jpx3.intave.share.Motion;
import de.jpx3.intave.share.Position;
import de.jpx3.intave.share.PositionMoveRotation;
import de.jpx3.intave.share.Rotation;

import java.util.Set;

public final class PlayerTeleportReader extends AbstractPacketReader {
  private WrapperPlayServerPlayerPositionAndLook wrapper;
  private PositionMoveRotation positionMoveRotation;
  private boolean mod;

  private WrapperPlayServerPlayerPositionAndLook wrapper() {
    if (wrapper == null) {
      wrapper = new WrapperPlayServerPlayerPositionAndLook((PacketSendEvent) event());
    }
    return wrapper;
  }

  public double positionX() {
    return internalPosMoveRotation().position().getX();
  }

  public void setPositionX(double x) {
    internalPosMoveRotation().position().setX(x);
    mod = true;
  }

  public double positionY() {
    return internalPosMoveRotation().position().getY();
  }

  public void setPositionY(double y) {
    internalPosMoveRotation().position().setY(y);
    mod = true;
  }

  public double positionZ() {
    return internalPosMoveRotation().position().getZ();
  }

  public void setPositionZ(double z) {
    internalPosMoveRotation().position().setZ(z);
    mod = true;
  }

  public Position position() {
    mod = true;
    return internalPosMoveRotation().position();
  }

  public float yaw() {
    return internalPosMoveRotation().rotation().yaw();
  }

  public void setYaw(float yaw) {
    internalPosMoveRotation().rotation().setYaw(yaw);
    mod = true;
  }

  public float pitch() {
    return internalPosMoveRotation().rotation().pitch();
  }

  public void setPitch(float pitch) {
    internalPosMoveRotation().rotation().setPitch(pitch);
    mod = true;
  }

  public Rotation rotation() {
    mod = true;
    return internalPosMoveRotation().rotation();
  }

  public double motionX() {
    return internalPosMoveRotation().motion().motionX();
  }

  public void setMotionX(double x) {
    internalPosMoveRotation().motion().setMotionX(x);
    mod = true;
  }

  public double motionY() {
    return internalPosMoveRotation().motion().motionY();
  }

  public void setMotionY(double y) {
    internalPosMoveRotation().motion().setMotionY(y);
    mod = true;
  }

  public double motionZ() {
    return internalPosMoveRotation().motion().motionZ();
  }

  public void setMotionZ(double z) {
    internalPosMoveRotation().motion().setMotionZ(z);
    mod = true;
  }

  public Motion motion() {
    mod = true;
    return internalPosMoveRotation().motion();
  }

  public PositionMoveRotation positionMoveRotation() {
    mod = true;
    return internalPosMoveRotation();
  }

  private PositionMoveRotation internalPosMoveRotation() {
    if (positionMoveRotation == null) {
      WrapperPlayServerPlayerPositionAndLook w = wrapper();
      Vector3d delta = null;
      try {
        delta = w.getDeltaMovement();
      } catch (Throwable ignored) {
        // delta movement exists only on 1.21.2+
      }
      positionMoveRotation = new PositionMoveRotation(
        Position.mutableOf(w.getX(), w.getY(), w.getZ()),
        delta == null ? new Motion(0, 0, 0) : new Motion(delta.getX(), delta.getY(), delta.getZ()),
        new Rotation(w.getYaw(), w.getPitch())
      );
    }
    return positionMoveRotation;
  }

  private void writePositionMoveRotation(PositionMoveRotation posMoveRot) {
    WrapperPlayServerPlayerPositionAndLook w = wrapper();
    w.setX(posMoveRot.position().getX());
    w.setY(posMoveRot.position().getY());
    w.setZ(posMoveRot.position().getZ());
    w.setYaw(posMoveRot.rotation().yaw());
    w.setPitch(posMoveRot.rotation().pitch());
    Motion motion = posMoveRot.motion();
    try {
      w.setDeltaMovement(new Vector3d(motion.motionX(), motion.motionY(), motion.motionZ()));
    } catch (Throwable ignored) {
      // delta movement exists only on 1.21.2+
    }
    event().markForReEncode(true);
  }

  /*
    Flushing is usually not required, but some very niece packet readers do
    require flushing before the packet is accessed.
   */
  @Override
  public void flush() {
    if (mod) {
      writePositionMoveRotation(positionMoveRotation);
    }
    mod = false;
    super.flush();
  }

  @Override
  public void release() {
    flush();
    positionMoveRotation = null;
    wrapper = null;
    super.release();
  }

  public Set<Relative> flags() {
    return Relative.fromMask(wrapper().getRelativeMask());
  }

  public void setFlags(Set<Relative> flags) {
    wrapper().setRelativeMask((byte) Relative.toMask(flags));
    event().markForReEncode(true);
  }
}
