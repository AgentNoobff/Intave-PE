package de.jpx3.intave.module.linker.packet;

public enum ListenerPriority {
  LOWEST(1),
  LOW(2),
  NORMAL(3),
  HIGH(4),
  HIGHEST(5),
  MONITOR(6);

  final int slot;

  ListenerPriority(int slot) {
    this.slot = slot;
  }

  public int slot() {
    return slot;
  }

  public com.github.retrooper.packetevents.event.PacketListenerPriority toPacketEventsPriority() {
    switch (this) {
      case LOWEST:
        return com.github.retrooper.packetevents.event.PacketListenerPriority.LOWEST;
      case LOW:
        return com.github.retrooper.packetevents.event.PacketListenerPriority.LOW;
      case NORMAL:
        return com.github.retrooper.packetevents.event.PacketListenerPriority.NORMAL;
      case HIGH:
        return com.github.retrooper.packetevents.event.PacketListenerPriority.HIGH;
      case HIGHEST:
        return com.github.retrooper.packetevents.event.PacketListenerPriority.HIGHEST;
      case MONITOR:
        return com.github.retrooper.packetevents.event.PacketListenerPriority.MONITOR;
    }
    return null;
  }
}
