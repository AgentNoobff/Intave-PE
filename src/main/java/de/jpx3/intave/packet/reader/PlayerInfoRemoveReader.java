package de.jpx3.intave.packet.reader;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class PlayerInfoRemoveReader extends AbstractPacketReader {
  public List<UUID> playersToRemove() {
    List<UUID> uuids = new WrapperPlayServerPlayerInfoRemove((PacketSendEvent) event()).getProfileIds();
    if (uuids == null) {
      return Collections.emptyList();
    }
    return uuids;
  }
}
