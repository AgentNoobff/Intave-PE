package de.jpx3.intave.packet.reader;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;
import de.jpx3.intave.annotate.Unmodifiable;
import de.jpx3.intave.share.Motion;

public class EntityVelocityReader extends EntityReader {
  private WrapperPlayServerEntityVelocity velocity;

  @Override
  public void release() {
    velocity = null;
    super.release();
  }

  // PacketEvents normalizes the legacy fixed-point (value/8000) and modern double encodings into a
  // single Vector3d, so the version-specific branching the ProtocolLib reader needed is gone.
  private WrapperPlayServerEntityVelocity velocity() {
    if (velocity == null) {
      velocity = new WrapperPlayServerEntityVelocity((PacketSendEvent) event());
    }
    return velocity;
  }

  public double motionX() {
    return velocity().getVelocity().getX();
  }

  public double motionY() {
    return velocity().getVelocity().getY();
  }

  public double motionZ() {
    return velocity().getVelocity().getZ();
  }

  public @Unmodifiable Motion motion() {
    Vector3d v = velocity().getVelocity();
    return new Motion(v.getX(), v.getY(), v.getZ());
  }

  public void setMotionX(double motionX) {
    Vector3d v = velocity().getVelocity();
    setVelocity(motionX, v.getY(), v.getZ());
  }

  public void setMotionY(double motionY) {
    Vector3d v = velocity().getVelocity();
    setVelocity(v.getX(), motionY, v.getZ());
  }

  public void setMotionZ(double motionZ) {
    Vector3d v = velocity().getVelocity();
    setVelocity(v.getX(), v.getY(), motionZ);
  }

  public void setMotion(Motion motion) {
    setVelocity(motion.motionX(), motion.motionY(), motion.motionZ());
  }

  private void setVelocity(double x, double y, double z) {
    velocity().setVelocity(new Vector3d(x, y, z));
    event().markForReEncode(true);
  }
}
