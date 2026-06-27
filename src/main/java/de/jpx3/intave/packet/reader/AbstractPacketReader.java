package de.jpx3.intave.packet.reader;

import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import de.jpx3.intave.IntaveControl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractPacketReader implements PacketReader {
  private static final Map<PacketTypeCommon, AtomicLong> MISSING_FLUSHES_BY_TYPE = new HashMap<>();

  private ProtocolPacketEvent event;

  @Override
  public void enter(ProtocolPacketEvent event) {
    if (this.event != null) {
      long val = MISSING_FLUSHES_BY_TYPE.computeIfAbsent(event.getPacketType(), packetType -> new AtomicLong()).incrementAndGet();
      if (val < 5 && IntaveControl.NOTIFY_MISSING_PACKET_FLUSHES) {
        System.out.println("Missing flush for packet " + event.getPacketType().getName() + " (" + val + ")");
        Thread.dumpStack();
      }
    }
    this.event = event;
  }

  @Override
  public void flush() {
  }

  @Override
  public void release() {
    event = null;
  }

  @Override
  public void releaseSafe() {
    if (event == null) {
      return;
    }
    release();
  }

  /** The in-flight packet event; concrete readers construct the relevant PacketWrapper from it. */
  public ProtocolPacketEvent event() {
    return event;
  }
}
