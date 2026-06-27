package de.jpx3.intave.packet.reader;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerAbilities;

public class AbilityOutReader extends AbstractPacketReader {
  private WrapperPlayServerPlayerAbilities wrapper() {
    return new WrapperPlayServerPlayerAbilities((PacketSendEvent) event());
  }

  public float flyingSpeed() {
    return wrapper().getFlySpeed();
  }

  public float walkingSpeed() {
    return wrapper().getFOVModifier();
  }

  public boolean flyingAllowed() {
    return wrapper().isFlightAllowed();
  }
}
