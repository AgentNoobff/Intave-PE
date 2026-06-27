package de.jpx3.intave.module.linker.packet;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import de.jpx3.intave.IntavePlugin;
import de.jpx3.intave.user.User;
import de.jpx3.intave.user.UserRepository;
import org.bukkit.entity.Player;

import java.util.Collection;

public final class ForwardingPacketAdapter extends WeakReferencePacketAdapter {
  private final PacketTypeCommon packetType;
  private final Collection<FilteringPacketAdapter> targetList;

  public ForwardingPacketAdapter(
    IntavePlugin plugin,
    PacketTypeCommon packetType,
    Collection<FilteringPacketAdapter> targetList
  ) {
    super(plugin, PacketListenerPriority.LOWEST, new PacketTypeCommon[]{packetType});
    this.packetType = packetType;
    this.targetList = targetList;
  }

  @Override
  public void onPacketSend(PacketSendEvent event) {
    if (event.getPacketType() != packetType) {
      return;
    }
    // PacketEvents fires events during LOGIN/CONFIGURATION too; gate to in-game players only.
    Player player = event.getPlayer();
    if (player == null || event.getConnectionState() != ConnectionState.PLAY) {
      return;
    }
    User user = UserRepository.userOf(player);
    if (user.shouldIgnoreNextOutboundPacket()) {
      return;
    }
    for (FilteringPacketAdapter filteringPacketAdapter : targetList) {
      filteringPacketAdapter.onPacketSend(event);
    }
  }

  @Override
  public void onPacketReceive(PacketReceiveEvent event) {
    if (event.getPacketType() != packetType) {
      return;
    }
    Player player = event.getPlayer();
    if (player == null || event.getConnectionState() != ConnectionState.PLAY) {
      return;
    }
    User user = UserRepository.userOf(player);
    if (user.shouldIgnoreNextInboundPacket()) {
      user.receiveNextInboundPacketAgain();
      return;
    }
    for (FilteringPacketAdapter filteringPacketAdapter : targetList) {
      filteringPacketAdapter.onPacketReceive(event);
    }
  }

  @Override
  public String toString() {
    return targetList.toString();
  }
}
