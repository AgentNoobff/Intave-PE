package de.jpx3.intave.module.filter;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTabComplete;
import com.google.common.collect.Lists;
import de.jpx3.intave.IntavePlugin;
import de.jpx3.intave.executor.Synchronizer;
import de.jpx3.intave.module.linker.packet.ListenerPriority;
import de.jpx3.intave.module.linker.packet.PacketSubscription;
import de.jpx3.intave.module.linker.packet.PrioritySlot;
import de.jpx3.intave.packet.reader.PacketReaders;
import de.jpx3.intave.packet.reader.PlayerInfoRemoveReader;
import de.jpx3.intave.user.User;
import de.jpx3.intave.user.UserRepository;
import de.jpx3.intave.user.meta.ProtocolMetadata;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static de.jpx3.intave.module.linker.packet.PacketId.Server.*;

public final class VanishFilter extends Filter {
  private final boolean disabled;
  private boolean yukiJoined = false;
  private long lastYukiJoin = 0;

  public VanishFilter(IntavePlugin plugin) {
    super("vanish");
    disabled = plugin.settings().getBoolean("command.fix-tab-kicks", false);

    int taskId = Bukkit.getScheduler().scheduleAsyncRepeatingTask(plugin, () -> {

      if (yukiJoined) {
        // 25% chance that yuki will leave
        if (ThreadLocalRandom.current().nextInt(1, 100) <= 25) {
          Synchronizer.synchronizeDelayed(() -> {
            yukiJoined = false;
          }, 20 * ThreadLocalRandom.current().nextInt(60, 120));
        }
      } else {
        int hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        boolean primeTime = hourOfDay >= 22 || hourOfDay <= 3;
        int chance = primeTime ? 10 : 2;
        if (ThreadLocalRandom.current().nextInt(1, 100) <= chance) {
          Synchronizer.synchronizeDelayed(() -> {
            yukiJoined = true;
            lastYukiJoin = System.currentTimeMillis();
          }, 20 * ThreadLocalRandom.current().nextInt(60, 120));
        }
      }
      // every 15 minutes
    }, 20 * 60 * 15, 20 * 60 * 15);
  }

  @PacketSubscription(
    packetsOut = {PLAYER_INFO}
  )
  public void on(ProtocolPacketEvent event) {
    Player player = event.getPlayer();
    User user = UserRepository.userOf(player);
    ProtocolMetadata protocol = user.meta().protocol();
    Set<UUID> shownPlayers = protocol.shownPlayers;

    if (event.getPacketType() == PacketType.Play.Server.PLAYER_INFO_UPDATE) {
      filterModernPlayerInfo((PacketSendEvent) event, shownPlayers);
    } else {
      filterLegacyPlayerInfo((PacketSendEvent) event, shownPlayers);
    }
  }

  private void filterModernPlayerInfo(PacketSendEvent event, Set<UUID> shownPlayers) {
    WrapperPlayServerPlayerInfoUpdate packet = new WrapperPlayServerPlayerInfoUpdate(event);
    EnumSet<WrapperPlayServerPlayerInfoUpdate.Action> actions = packet.getActions();
    List<WrapperPlayServerPlayerInfoUpdate.PlayerInfo> playerInfos = packet.getEntries();

    for (WrapperPlayServerPlayerInfoUpdate.Action action : actions) {
      if (action == WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER) {
        playerInfos.forEach(data -> shownPlayers.add(data.getProfileId()));
      } else if (action == WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_GAME_MODE
        || action == WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LATENCY) {
        playerInfos.removeIf(info -> !shownPlayers.contains(info.getProfileId()));
      }
    }

    if (playerInfos.isEmpty()) {
      event.setCancelled(true);
    }
    Collections.shuffle(playerInfos);
    packet.setEntries(playerInfos);
  }

  private void filterLegacyPlayerInfo(PacketSendEvent event, Set<UUID> shownPlayers) {
    WrapperPlayServerPlayerInfo packet = new WrapperPlayServerPlayerInfo(event);
    WrapperPlayServerPlayerInfo.Action action = packet.getAction();
    List<WrapperPlayServerPlayerInfo.PlayerData> playerInfos = packet.getPlayerDataList();

    switch (action) {
      case ADD_PLAYER:
        playerInfos.forEach(data -> shownPlayers.add(data.getUserProfile().getUUID()));
        break;
      case UPDATE_GAME_MODE:
      case UPDATE_LATENCY:
        playerInfos.removeIf(info -> !shownPlayers.contains(info.getUserProfile().getUUID()));
        break;
      case REMOVE_PLAYER:
        playerInfos.removeIf(data -> !shownPlayers.remove(data.getUserProfile().getUUID()));
        break;
    }

    if (playerInfos.isEmpty()) {
      event.setCancelled(true);
    }
    Collections.shuffle(playerInfos);
    packet.setPlayerDataList(playerInfos);
  }

  @PacketSubscription(
//    engine = Engine.ASYNC_INTERNAL,
    prioritySlot = PrioritySlot.EXTERNAL,
    priority = ListenerPriority.MONITOR,
    packetsOut = {
      TAB_COMPLETE_OUT
    }
  )
  public void receiveTabComplete(ProtocolPacketEvent event) {
    Player player = event.getPlayer();
    User user = UserRepository.userOf(player);
    ProtocolMetadata protocol = user.meta().protocol();
    Set<UUID> shownPlayers = protocol.shownPlayers;

    WrapperPlayServerTabComplete packet = new WrapperPlayServerTabComplete((PacketSendEvent) event);
    List<WrapperPlayServerTabComplete.CommandMatch> matches = packet.getCommandMatches();
    if (matches != null) {
      List<String> playerNames = Bukkit.getOnlinePlayers().stream()
        .map(Player::getName).collect(Collectors.toList());
      List<String> hiddenPlayers = Lists.newArrayList();
      for (String name : playerNames) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
          continue;
        }
        if (!shownPlayers.contains(target.getUniqueId())) {
          hiddenPlayers.add(name);
        }
      }
      List<WrapperPlayServerTabComplete.CommandMatch> newTabCompletions = matches.stream()
        .filter(match -> !hiddenPlayers.contains(match.getText()))
        .collect(Collectors.toList());
      if (newTabCompletions.size() != matches.size()) {
        packet.setCommandMatches(newTabCompletions);
      }
    }
  }

//  @PacketSubscription(
//    packetsOut = {
//      SCOREBOARD_TEAM
//    }
//  )
//  public void onTeam(PacketEvent event) {
//    Player player = event.getPlayer();
//    PacketContainer packet = event.getPacket();
//    User user = UserRepository.userOf(player);
//    ProtocolMetadata protocol = user.meta().protocol();
//    Set<UUID> shownPlayers = protocol.shownPlayers;
//    String teamName = packet.getStrings().readSafely(0);
//    shownPlayers.removeIf(uuid -> teamName.contains(Bukkit.getPlayer(uuid).getName()));
//  }

  @PacketSubscription(
    packetsOut = {
      PLAYER_INFO_REMOVE
    }
  )
  public void onRemoval(ProtocolPacketEvent event) {
    Player player = event.getPlayer();
    User user = UserRepository.userOf(player);
    ProtocolMetadata protocol = user.meta().protocol();
    Set<UUID> shownPlayers = protocol.shownPlayers;
    PlayerInfoRemoveReader reader = PacketReaders.readerOf(event);
    List<UUID> uuids = reader.playersToRemove();
    uuids.removeIf(uuid -> !shownPlayers.contains(uuid));
    reader.release();
  }

  @Override
  protected boolean enabled() {
    return !disabled && super.enabled();
  }
}
