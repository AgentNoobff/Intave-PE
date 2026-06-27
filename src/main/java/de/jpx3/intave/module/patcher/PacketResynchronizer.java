package de.jpx3.intave.module.patcher;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import de.jpx3.intave.diagnostic.PacketSynchronizations;
import de.jpx3.intave.executor.Synchronizer;
import de.jpx3.intave.module.Module;
import de.jpx3.intave.module.linker.packet.ListenerPriority;
import de.jpx3.intave.module.linker.packet.PacketSubscription;
import de.jpx3.intave.packet.PacketSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

import static de.jpx3.intave.module.linker.packet.PacketId.Server.*;

public final class PacketResynchronizer extends Module {
  @PacketSubscription(
    priority = ListenerPriority.LOWEST,
    packetsOut = {
      ABILITIES_OUT, ATTACH_ENTITY, /*CLOSE_WINDOW*/ ENTITY_DESTROY, ENTITY_LOOK, ENTITY_METADATA,
      ENTITY_MOVE_LOOK, ENTITY_STATUS, ENTITY_TELEPORT, MOUNT, NAMED_ENTITY_SPAWN,
      /*OPEN_WINDOW,*/ PLAYER_INFO, PLAYER_LIST_HEADER_FOOTER, POSITION, REL_ENTITY_MOVE, REL_ENTITY_MOVE_LOOK,
      REMOVE_ENTITY_EFFECT, RESPAWN, SPAWN_ENTITY, SPAWN_ENTITY_LIVING, /*WINDOW_ITEMS,*/ WORLD_BORDER
    }
  )
  public void catchDesynchronized(ProtocolPacketEvent event) {
    if (isInInvalidThread()) {
      event.setCancelled(true);
      Player player = event.getPlayer();
      // Retain a copy of the in-flight buffer so the packet can be re-sent on the main thread after
      // this event is cancelled (the original event buffer is released once processing returns).
      PacketWrapper<?> source = new PacketWrapper<>((PacketSendEvent) event);
      io.netty.buffer.ByteBuf buffer = (io.netty.buffer.ByteBuf) source.getBuffer();
      io.netty.buffer.ByteBuf retained = buffer.copy(buffer.readerIndex(), buffer.readableBytes());
      PacketWrapper<?> resend = new PacketWrapper<>(event.getPacketType());
      resend.setBuffer(retained);
      Synchronizer.synchronize(() -> sendPacket(player, resend));
      PacketSynchronizations.enterResynchronization(event.getPacketType());
    }
  }

  private final Map<String, Boolean> cache = new HashMap<>();

  private boolean isInInvalidThread() {
    return cache.computeIfAbsent(Thread.currentThread().getName(), s -> s.startsWith("Netty "));
  }

  private void sendPacket(Player player, PacketWrapper<?> packet) {
    PacketSender.sendServerPacket(player, packet);
  }
}
