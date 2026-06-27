package de.jpx3.intave.packet.reader;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCloseWindow;

public final class WindowCloseReader extends AbstractPacketReader {
  public int container() {
    return new WrapperPlayClientCloseWindow((PacketReceiveEvent) event()).getWindowId();
  }
}
