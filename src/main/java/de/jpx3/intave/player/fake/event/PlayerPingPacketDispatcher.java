package de.jpx3.intave.player.fake.event;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import de.jpx3.intave.IntavePlugin;
import de.jpx3.intave.module.Modules;
import de.jpx3.intave.module.linker.packet.PacketEventSubscriber;
import de.jpx3.intave.module.linker.packet.PacketSubscription;
import de.jpx3.intave.player.fake.FakePlayer;
import de.jpx3.intave.user.User;
import de.jpx3.intave.user.UserRepository;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.List;

import static de.jpx3.intave.module.linker.packet.PacketId.Server.PLAYER_INFO;

public final class PlayerPingPacketDispatcher implements PacketEventSubscriber {
  private static final long MIN_TIME_BETWEEN_PLAYER_INFO_UPDATE = 10_000;

  public PlayerPingPacketDispatcher(IntavePlugin plugin) {
    Modules.linker().packetEvents().linkSubscriptionsIn(this);
  }

  @PacketSubscription(
    packetsOut = {
      PLAYER_INFO
    }
  )
  public void onPacketSending(ProtocolPacketEvent event) {
    Player player = event.getPlayer();
    if (player == null) {
      return;
    }
    User user = UserRepository.userOf(player);
    FakePlayer fakePlayer = user.meta().attack().fakePlayer();
    if (fakePlayer == null) {
      return;
    }
    if (System.currentTimeMillis() - fakePlayer.lastPingPacketSent < MIN_TIME_BETWEEN_PLAYER_INFO_UPDATE) {
      return;
    }
    if (event.getPacketType() == PacketType.Play.Server.PLAYER_INFO_UPDATE) {
      appendModern(fakePlayer, (PacketSendEvent) event);
    } else if (event.getPacketType() == PacketType.Play.Server.PLAYER_INFO) {
      appendLegacy(fakePlayer, (PacketSendEvent) event);
    }
  }

  private void appendLegacy(FakePlayer fakePlayer, PacketSendEvent event) {
    WrapperPlayServerPlayerInfo packet = new WrapperPlayServerPlayerInfo(event);
    if (packet.getAction() != WrapperPlayServerPlayerInfo.Action.UPDATE_LATENCY) {
      return;
    }
    int latency = fakePlayer.nextLatency();
    UserProfile profile = fakePlayer.profile();
    WrapperPlayServerPlayerInfo.PlayerData playerData = new WrapperPlayServerPlayerInfo.PlayerData(
      Component.text(profile.getName()), profile, fakePlayer.gameMode(), latency
    );
    List<WrapperPlayServerPlayerInfo.PlayerData> dataList = packet.getPlayerDataList();
    dataList.add(playerData);
    packet.setPlayerDataList(dataList);
    fakePlayer.lastPingPacketSent = System.currentTimeMillis();
  }

  private void appendModern(FakePlayer fakePlayer, PacketSendEvent event) {
    WrapperPlayServerPlayerInfoUpdate packet = new WrapperPlayServerPlayerInfoUpdate(event);
    if (!packet.getActions().contains(WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LATENCY)) {
      return;
    }
    int latency = fakePlayer.nextLatency();
    UserProfile profile = fakePlayer.profile();
    WrapperPlayServerPlayerInfoUpdate.PlayerInfo info = new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
      profile, true, latency, fakePlayer.gameMode(), Component.text(profile.getName()), null
    );
    List<WrapperPlayServerPlayerInfoUpdate.PlayerInfo> entries = packet.getEntries();
    entries.add(info);
    packet.setEntries(entries);
    fakePlayer.lastPingPacketSent = System.currentTimeMillis();
  }
}
