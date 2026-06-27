package de.jpx3.intave.player.fake;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.protocol.sound.Sound;
import com.github.retrooper.packetevents.protocol.sound.SoundCategory;
import com.github.retrooper.packetevents.protocol.sound.Sounds;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMoveAndRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSoundEffect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnPlayer;
import com.google.common.base.Preconditions;
import de.jpx3.intave.adapter.MinecraftVersions;
import de.jpx3.intave.block.access.VolatileBlockAccess;
import de.jpx3.intave.block.type.BlockTypeAccess;
import de.jpx3.intave.executor.Synchronizer;
import de.jpx3.intave.packet.PacketSender;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.jpx3.intave.player.fake.FakePlayerAttribute.*;
import static de.jpx3.intave.player.fake.MetadataAccess.metadataAccept;
import static de.jpx3.intave.player.fake.MetadataAccess.updateVisibility;
import static de.jpx3.intave.player.fake.ScoreboardAccessor.sendScoreboard;
import static de.jpx3.intave.player.fake.TablistMutator.addToTabList;
import static de.jpx3.intave.player.fake.TablistMutator.removeFromTabList;

public abstract class FakePlayerBody extends FakePlayerIdentity {
  private static final boolean POSITION_PROCESSING_1_9 = MinecraftVersions.VER1_9_0.atOrAbove();
  private static final boolean MODERN_PLAYER_INFO = MinecraftVersions.VER1_19_3.atOrAbove();

  private static final Map<Integer, Object> METADATA = new HashMap<Integer, Object>() {{
    // Entity
    put(0, (byte) 0);
    put(1, returnShortOrInt(POSITION_PROCESSING_1_9));
    put(2, "");
    put(3, (byte) 0);
    put(4, (byte) 0);
    // EntityLivingBase
    put(6, 1.0f);
    put(7, 0);
    put(8, (byte) 0);
    put(9, (byte) 0);
    // EntityPlayer
    put(16, (byte) 0);
    put(17, 0.0f);
    put(18, 0);
    put(10, (byte) 0);
  }};

  private final Player observer;
  private final String listedPrefix, prefix;
  private final int attributes;

  protected FakePlayerBody(
    Player observer,
    int entityId, int attributes,
    UserProfile profile,
    String listedPrefix, String prefix
  ) {
    super(entityId, profile);
    this.observer = observer;
    this.attributes = attributes;
    this.listedPrefix = listedPrefix;
    this.prefix = prefix;
  }

  protected void spawn(Location spawn) {
    UserProfile profile = profile();
    List<EntityData<?>> dataWatcher = dataWatcher();
    dataWatcher.clear();
    METADATA.forEach((index, object) -> metadataAccept(dataWatcher, index, object.getClass(), object));

    String tabListName = listedPrefix + profile.getName();
    addToTabList(observer, profile, tabListName);

    com.github.retrooper.packetevents.protocol.world.Location peLocation =
      new com.github.retrooper.packetevents.protocol.world.Location(
        spawn.getX(), spawn.getY(), spawn.getZ(), spawn.getYaw(), spawn.getPitch()
      );
    WrapperPlayServerSpawnPlayer spawnPacket = new WrapperPlayServerSpawnPlayer(
      identifier(),
      profile.getUUID(),
      peLocation,
      new ArrayList<>(dataWatcher)
    );
    send(spawnPacket);

    if (!hasAttribute(attributes, IN_TABLIST)) {
      removeFromTabList(observer, profile());
    }
    if (hasAttribute(attributes, INVISIBLE)) {
      updateVisibility(observer, this, true);
    }
  }

  public void despawn() {
    if (hasAttribute(attributes, IN_TABLIST)) {
      TablistMutator.removeFromTabList(observer(), profile());
    }
    send(new WrapperPlayServerDestroyEntities(identifier()));
  }

  public void respawn(Location location) {
    if (threadEscape(() -> respawn(location))) {
      return;
    }
    despawn();
    spawn(location);
  }

  public void setSprinting(boolean sprinting) {
    MetadataAccess.setSprinting(observer, this, sprinting);
  }

  public void setSneaking(boolean sneaking) {
    MetadataAccess.setSneaking(observer, this, sneaking);
  }

  public void movementUpdate(
    Location to,
    Location from,
    boolean onGround
  ) {
    boolean move = safeDistance(to, from) != 0;
    boolean look = rotationChange(to, from);

    PacketWrapper<?> packet = null;
    double deltaX = to.getX() - from.getX();
    double deltaY = to.getY() - from.getY();
    double deltaZ = to.getZ() - from.getZ();
    if (move && look) {
      packet = new WrapperPlayServerEntityRelativeMoveAndRotation(
        identifier(), deltaX, deltaY, deltaZ, to.getYaw(), to.getPitch(), onGround
      );
    } else if (move) {
      packet = new WrapperPlayServerEntityRelativeMove(
        identifier(), deltaX, deltaY, deltaZ, onGround
      );
    } else if (look) {
      packet = new WrapperPlayServerEntityRotation(
        identifier(), to.getYaw(), to.getPitch(), onGround
      );
    }
    if (packet != null) {
      send(packet);
    }
    if (look) {
      rotationUpdate(to.getYaw());
    }
  }

  public double safeDistance(Location location1, Location location2) {
    if (location1.getWorld() != location2.getWorld()) {
      return 0.0;
    }
    return location1.distance(location2);
  }

  private void rotationUpdate(float yaw) {
    send(new WrapperPlayServerEntityHeadLook(identifier(), yaw));
  }

  private static boolean rotationChange(Location location1, Location location2) {
    boolean equalYaw = location1.getYaw() == location2.getYaw();
    boolean equalPitch = location1.getPitch() == location2.getPitch();
    return !equalYaw || !equalPitch;
  }

  public void movementTeleport(Location to, boolean onGround) {
    Preconditions.checkNotNull(to);
    com.github.retrooper.packetevents.protocol.world.Location peLocation =
      new com.github.retrooper.packetevents.protocol.world.Location(
        to.getX(), to.getY(), to.getZ(), to.getYaw(), to.getPitch()
      );
    send(new WrapperPlayServerEntityTeleport(identifier(), peLocation, onGround));
  }

  public void makeWalkingSound(Location location) {
    Sound sound = walkingSoundAt(location);
    float volume = MinecraftVersions.VER1_10_0.atOrAbove() ? 1f : 0.15f;
    float pitch = MinecraftVersions.VER1_10_0.atOrAbove() ? 0.15f : 1f;
    WrapperPlayServerSoundEffect packet = new WrapperPlayServerSoundEffect(
      sound,
      SoundCategory.PLAYER,
      new Vector3d(location.getX(), location.getY(), location.getZ()),
      volume,
      pitch
    );
    send(packet);
  }

  private Sound walkingSoundAt(Location location) {
    Block block = VolatileBlockAccess.blockAccess(location.clone().add(0.0, -1.0, 0.0));
    switch (BlockTypeAccess.typeAccess(block)) {
      case GRASS: {
        return Sounds.BLOCK_GRASS_STEP;
      }
      case GRAVEL: {
        return Sounds.BLOCK_GRAVEL_STEP;
      }
      case WOOD: {
        return Sounds.BLOCK_WOOD_STEP;
      }
      default:
        return Sounds.BLOCK_STONE_STEP;
    }
  }

  public void applyDisplayName() {
    sendScoreboard(observer, randomTeamName(), profile(), hasAttribute(attributes, INVISIBLE));
  }

  private static String randomTeamName() {
    return RandomStringGenerator.randomString();
  }

  public void latencyInitialize() {
    int latency = 0;
    PacketWrapper<?> packet;
    if (MODERN_PLAYER_INFO) {
      WrapperPlayServerPlayerInfoUpdate.PlayerInfo info = new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
        profile(), true, latency, GameMode.SURVIVAL, Component.text(prefix), null
      );
      packet = new WrapperPlayServerPlayerInfoUpdate(
        EnumSet.of(WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LATENCY),
        info
      );
    } else {
      WrapperPlayServerPlayerInfo.PlayerData playerData = new WrapperPlayServerPlayerInfo.PlayerData(
        Component.text(prefix), profile(), GameMode.SURVIVAL, latency
      );
      packet = new WrapperPlayServerPlayerInfo(
        WrapperPlayServerPlayerInfo.Action.UPDATE_LATENCY, playerData
      );
    }
    send(packet);
  }

  private void send(PacketWrapper<?> packet) {
    if (threadEscape(() -> send(packet))) {
      return;
    }
    PacketSender.sendServerPacket(this.observer, packet);
  }

  private boolean threadEscape(Runnable apply) {
    if (Bukkit.isPrimaryThread()) {
      return false;
    }
    Synchronizer.synchronize(apply);
    return true;
  }

  // Do not remove
  private static Object returnShortOrInt(boolean returnInt) {
    if (returnInt) {
      return 300;
    } else {
      return (short) 300;
    }
  }

  public Player observer() {
    return observer;
  }

  public int attributes() {
    return attributes;
  }
}
