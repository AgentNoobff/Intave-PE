package de.jpx3.intave.packet.reader;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerAbilities;

public class AbilityInReader extends AbstractPacketReader {
  public boolean requestedFlying() {
    return new WrapperPlayClientPlayerAbilities((PacketReceiveEvent) event()).isFlying();
  }
}
