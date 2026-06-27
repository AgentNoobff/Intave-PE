package de.jpx3.intave.packet.reader;

import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import de.jpx3.intave.module.linker.packet.PacketId;
import de.jpx3.intave.module.linker.packet.PacketTypeTranslator;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static de.jpx3.intave.module.linker.packet.PacketId.Client;
import static de.jpx3.intave.module.linker.packet.PacketId.Client.*;
import static de.jpx3.intave.module.linker.packet.PacketId.Client.CLOSE_WINDOW;
import static de.jpx3.intave.module.linker.packet.PacketId.Client.VEHICLE_MOVE;
import static de.jpx3.intave.module.linker.packet.PacketId.Server;
import static de.jpx3.intave.module.linker.packet.PacketId.Server.*;
import static de.jpx3.intave.module.linker.packet.PacketId.Server.POSITION;

public final class PacketReaders {
  private static final Map<PacketTypeCommon, ThreadLocal<? extends PacketReader>> readerLocals = new HashMap<>();

  public static void setup() {
    setup(ABILITIES_OUT, AbilityOutReader::new);
    setup(ANIMATION, EntityReader::new);
    setup(ATTACH_ENTITY, AttachEntityReader::new);
    setup(BLOCK_ACTION, BlockActionReader::new);
    setup(BLOCK_CHANGE, SingleBlockChangeReader::new);
    setup(BLOCK_BREAK, SingleBlockChangeReader::new);
    setup(BLOCK_BREAK_ANIMATION, EntityReader::new);
    setup(CAMERA, EntityReader::new);
    setup(COLLECT, EntityReader::new);
    setup(COMBAT_EVENT, CombatEventReader::new);
    setup(ENTITY, EntityReader::new);
    setup(ENTITY_DESTROY, EntityDestroyReader::new);
    setup(ENTITY_EFFECT, EntityEffectReader::new);
    setup(ENTITY_EQUIPMENT, EntityReader::new);
    setup(ENTITY_HEAD_ROTATION, EntityReader::new);
    setup(ENTITY_LOOK, EntityReader::new);
    setup(ENTITY_METADATA, EntityMetadataReader::new);
    setup(ENTITY_MOVE_LOOK, EntityReader::new);
    setup(ENTITY_STATUS, EntityReader::new);
    setup(ENTITY_SOUND, EntityReader::new);
    setup(ENTITY_TELEPORT, EntityReader::new);
    setup(ENTITY_VELOCITY, EntityVelocityReader::new);
    setup(EXPLOSION, ExplosionReader::new);
    setup(GAME_STATE_CHANGE, GameStateChangeReader::new);
    setup(LOGIN, EntityReader::new);
    setup(LOOK_AT, EntityReader::new);
    setup(MAP_CHUNK, MapChunkReader::new);
    setup(MAP_CHUNK_BULK, MapChunkBulkReader::new);
    setup(MOUNT, MountEntityReader::new);
    setup(MULTI_BLOCK_CHANGE, MultiBlockChangeReader::new);
    setup(NAMED_ENTITY_SPAWN, EntityReader::new);
    setup(OPEN_WINDOW, WindowOpenReader::new);
    setup(OPEN_WINDOW_HORSE, WindowOpenReader::new);
    setup(POSITION, PlayerTeleportReader::new);
    setup(CLOSE_WINDOW, WindowCloseReader::new);
    setup(PLAYER_INFO, PlayerInfoReader::new);
    setup(PLAYER_INFO_REMOVE, PlayerInfoRemoveReader::new);
    setup(REMOVE_ENTITY_EFFECT, EntityReader::new);
    setup(REL_ENTITY_MOVE, EntityReader::new);
    setup(REL_ENTITY_MOVE_LOOK, EntityReader::new);
    setup(SPAWN_ENTITY, EntityReader::new);
    setup(SPAWN_ENTITY_LIVING, EntityReader::new);
    setup(SPAWN_ENTITY_PAINTING, EntityReader::new);
    setup(SPAWN_ENTITY_WEATHER, EntityReader::new);
    setup(SPAWN_ENTITY_EXPERIENCE_ORB, EntityReader::new);
    setup(UPDATE_ATTRIBUTES, EntityReader::new);
    setup(UPDATE_ENTITY_NBT, EntityReader::new);
    setup(USE_BED, EntityReader::new);

    setup(ATTACK_ENTITY, EntityUseReader::new);
    setup(ABILITIES_IN, AbilityInReader::new);
    setup(BLOCK_DIG, BlockPositionReader::new);
    setup(BLOCK_PLACE, BlockInteractionReader::new);
    setup(CUSTOM_PAYLOAD_IN, PayloadInReader::new);
    setup(ENTITY_ACTION_IN, PlayerActionReader::new);
    setup(USE_ITEM, BlockInteractionReader::new);
    setup(USE_ITEM_ON, BlockInteractionReader::new);
    setup(USE_ENTITY, EntityUseReader::new);
    setup(FLYING, PlayerMoveReader::new);
    setup(Client.POSITION, PlayerMoveReader::new);
    setup(POSITION_LOOK, PlayerMoveReader::new);
    setup(LOOK, PlayerMoveReader::new);
    setup(VEHICLE_MOVE, PlayerMoveReader::new);
    setup(WINDOW_ITEMS, WindowBulkItemReader::new);
    setup(WINDOW_CLICK, WindowClickReader::new);
    setup(SET_SLOT, WindowSingleItemReader::new);

    // for some
  }

  private static void setup(Server serverPacket, Supplier<? extends PacketReader> supplier) {
    for (PacketTypeCommon packetType : PacketTypeTranslator.translate(serverPacket)) {
      readerLocals.put(packetType, ThreadLocal.withInitial(supplier));
    }
  }

  private static void setup(Client clientPacket, Supplier<? extends PacketReader> supplier) {
    for (PacketTypeCommon packetType : PacketTypeTranslator.translate(clientPacket)) {
      readerLocals.put(packetType, ThreadLocal.withInitial(supplier));
    }
  }

  public static <T extends PacketReader> T readerOf(ProtocolPacketEvent event) {
    PacketTypeCommon type = event.getPacketType();
    ThreadLocal<? extends PacketReader> readerThreadLocal = readerLocals.get(type);
    if (readerThreadLocal == null) {
      throw new IllegalStateException("No reader available for type " + type.getName());
    }
    PacketReader interpreter = readerThreadLocal.get();
    interpreter.enter(event);
    //noinspection unchecked
    return (T) interpreter;
  }

  public static boolean hasReader(PacketTypeCommon type) {
    return readerLocals.containsKey(type);
  }
}
