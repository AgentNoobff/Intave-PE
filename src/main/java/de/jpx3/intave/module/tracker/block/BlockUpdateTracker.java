package de.jpx3.intave.module.tracker.block;

import com.github.retrooper.packetevents.event.CancellableEvent;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerAcknowledgeBlockChanges;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import de.jpx3.intave.block.cache.BlockCache;
import de.jpx3.intave.block.variant.BlockVariantNativeAccess;
import de.jpx3.intave.check.movement.physics.environment.SimulationEnvironment;
import de.jpx3.intave.module.Module;
import de.jpx3.intave.module.feedback.EmptyFeedbackCallback;
import de.jpx3.intave.module.feedback.PendingCountingFeedbackObserver;
import de.jpx3.intave.module.linker.packet.Engine;
import de.jpx3.intave.module.linker.packet.ListenerPriority;
import de.jpx3.intave.module.linker.packet.PacketSubscription;
import de.jpx3.intave.packet.reader.*;
import de.jpx3.intave.share.Position;
import de.jpx3.intave.user.User;
import de.jpx3.intave.user.UserRepository;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.List;

import static de.jpx3.intave.check.movement.physics.MoveMetric.NEARBY_COLLISION_INACCURACY;
import static de.jpx3.intave.module.feedback.FeedbackOptions.*;
import static de.jpx3.intave.module.linker.packet.PacketId.Client.*;
import static de.jpx3.intave.module.linker.packet.PacketId.Server.*;

public final class BlockUpdateTracker extends Module {
  @PacketSubscription(
    engine = Engine.ASYNC_INTERNAL,
    packetsOut = {
      MAP_CHUNK, MAP_CHUNK_BULK
    }
  )
  public void chunkUpdate(
    User user, Player player, ChunkCoordinateReader coordinates
  ) {
    int[] xCoordinates = coordinates.xCoordinates();
    int[] zCoordinates = coordinates.zCoordinates();
    if (xCoordinates.length != zCoordinates.length) {
      throw new IllegalStateException();
    }
    // MAP_CHUNK_BULK has no PacketEvents wrapper (1.8 only); its coordinate arrays are empty.
    if (xCoordinates.length == 0) {
      return;
    }
    if (xCoordinates.length > 1) {
      user.tickFeedback(
        () -> {
          for (int k = 0; k < xCoordinates.length; k++) {
            BlockUpdateTracker.this.chunkInvalidate(player, xCoordinates[k], zCoordinates[k]);
          }
        },
        APPEND_ON_OVERFLOW | SELF_SYNCHRONIZATION
      );
    } else {
      int chunkX = xCoordinates[0], chunkZ = zCoordinates[0];
      Position position = user.meta().movement().position();
      int playerChunkX = position.chunkX(), playerChunkZ = position.chunkZ();
      double distance = Math.sqrt(
        NumberConversions.square(playerChunkX - chunkX) +
          NumberConversions.square(playerChunkZ - chunkZ)
      );
      boolean relevant = distance <= 4 || user.blockCache().hasOverridesInBounds(chunkX << 4, (chunkX + 1) << 4, chunkZ << 4, (chunkZ + 1) << 4);
      user.tickFeedback(
        () -> BlockUpdateTracker.this.chunkInvalidate(player, chunkX, chunkZ),
        (relevant ? APPEND_ON_OVERFLOW : APPEND) | SELF_SYNCHRONIZATION
      );
    }
  }

  private void chunkInvalidate(Player player, int chunkX, int chunkZ) {
    int chunkXMinPos = chunkX << 4, chunkXMaxPos = chunkXMinPos + 16;
    int chunkZMinPos = chunkZ << 4, chunkZMaxPos = chunkZMinPos + 16;
    BlockCache blockStateAccess = UserRepository.userOf(player).blockCache();
    blockStateAccess.invalidateOverridesInBounds(chunkXMinPos, chunkXMaxPos, chunkZMinPos, chunkZMaxPos);
  }

  @PacketSubscription(
    priority = ListenerPriority.LOWEST,
    packetsIn = {
      BLOCK_DIG, BLOCK_PLACE, USE_ITEM
    }
  )
  public void checkInteractionTarget(
    User user, ProtocolPacketEvent event,
    BlockPositionReader reader, CancellableEvent cancellable
  ) {
    PacketTypeCommon packetType = event.getPacketType();
    boolean check = true;

    if (packetType == PacketType.Play.Client.PLAYER_DIGGING) {
      DiggingAction action = new WrapperPlayClientPlayerDigging((PacketReceiveEvent) event).getAction();
      check = action == DiggingAction.START_DIGGING || action == DiggingAction.FINISHED_DIGGING || action == DiggingAction.CANCELLED_DIGGING;
    } else if (packetType == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
      Vector3i blockPosition = reader.blockPosition();
      if (blockPosition == null) {
        return;
      }
      BlockInteractionReader placeInterpreter = (BlockInteractionReader) reader;
      if (placeInterpreter.enumDirection() == 255 || cancellable.isCancelled()) {
        check = false;
      }
    }

    if (check) {
      SimulationEnvironment movementData = user.meta().movement();
      Vector3i blockPosition = reader.blockPosition();
      if (blockPosition == null) {
        return;
      }
      Vector targetBlock = new Vector(blockPosition.getX(), blockPosition.getY(), blockPosition.getZ());
      Vector playerLocation = new Vector(movementData.lastPositionX(), movementData.lastPositionY(), movementData.lastPositionZ());
      if (playerLocation.distance(targetBlock) > 16) {
        cancellable.setCancelled(true);
      }
    }
  }

  @PacketSubscription(
    packetsOut = {
      BLOCK_BREAK, BLOCK_CHANGE, MULTI_BLOCK_CHANGE
    }
  )
  public void sentBlockUpdate(ProtocolPacketEvent event) {
    Player player = event.getPlayer();
    User user = UserRepository.userOf(player);
    boolean speculativeBlocks = user.meta().protocol().clientSpeculativeBlocks();
    PendingCountingFeedbackObserver pendingBlockUpdates = user.meta().connection().pendingBlockUpdates;

    BlockChanges changes = PacketReaders.readerOf(event);
    List<Vector3i> blockPositions = changes.blockPositions();
    List<WrappedBlockState> blockDataList = changes.blockDataList();
    changes.release();

    World world = player.getWorld();
    EmptyFeedbackCallback process = () -> {
      BlockCache blockCache = user.blockCache();
      Location verifiedLocation = user.meta().movement().verifiedLocation();
      for (int i = 0; i < blockPositions.size(); i++) {
        Vector3i blockPosition = blockPositions.get(i);
        WrappedBlockState blockData = blockDataList.get(i);
        if (distance(verifiedLocation, blockPosition) < 2) {
          user.meta().movement().activeTick(NEARBY_COLLISION_INACCURACY);
        }
        Material material = materialOf(blockData);
        int variant = BlockVariantNativeAccess.variantAccess(blockData);
        int positionX = blockPosition.getX();
        int positionY = blockPosition.getY();
        int positionZ = blockPosition.getZ();
        if (speculativeBlocks && blockCache.isClientSpeculatingAt(positionX, positionY, positionZ)) {
          blockCache.setClientSpeculationValue(world, positionX, positionY, positionZ, material, variant, user.meta().inventory().lastBlockSequenceNumber);
        } else {
          blockCache.unlockOverride(positionX, positionY, positionZ);
          blockCache.override(world, positionX, positionY, positionZ, material, variant, "UPDATE");
          blockCache.invalidateCacheAround(positionX, positionY, positionZ);
        }
      }
    };

    Location location = player.getLocation();
    boolean transactionSynchronize = inDistance(blockPositions, location, 8);
    if (transactionSynchronize) {
      user.tracedPacketTickFeedback(event, process, pendingBlockUpdates);
    } else {
      process.success();
    }
  }

  private static Material materialOf(WrappedBlockState blockData) {
    try {
      return SpigotConversionUtil.toBukkitBlockData(blockData).getMaterial();
    } catch (Throwable legacyOrUnsupported) {
      // Pre-1.13 has no Bukkit BlockData; fall back to matching the block-state type name.
      Material material = Material.matchMaterial(blockData.getType().getName().toUpperCase());
      return material != null ? material : Material.AIR;
    }
  }

  @PacketSubscription(
    packetsOut = {
      BLOCK_CHANGED_ACK
    }
  )
  public void blockChangedAck(ProtocolPacketEvent event) {
    Player player = event.getPlayer();
    User user = UserRepository.userOf(player);
    int sequenceNumber = new WrapperPlayServerAcknowledgeBlockChanges((PacketSendEvent) event).getSequence();
    user.packetTickFeedback(event, () ->
      user.blockCache().moveClientSpeculationsToOverride(player.getWorld(), sequenceNumber)
    );
  }

  private static boolean inDistance(Collection<? extends Vector3i> blockPositions, Location playerLocation, int requiredDistance) {
    for (Vector3i blockPosition : blockPositions) {
      if (distance(playerLocation, blockPosition) < requiredDistance) {
        return true;
      }
    }
    return false;
  }

  private static double distance(Location playerLocation, Vector3i blockPosition) {
    return Math.sqrt(
      NumberConversions.square(playerLocation.getBlockX() - blockPosition.getX()) +
        NumberConversions.square(playerLocation.getBlockY() - blockPosition.getY()) +
        NumberConversions.square(playerLocation.getBlockZ() - blockPosition.getZ())
    );
  }
}
