package de.jpx3.intave.module.linker.packet;

import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;

import java.util.EnumMap;
import java.util.Map;

/**
 * Single source of truth translating Intave's {@link PacketId} constants (which carry historical
 * ProtocolLib-style names) into PacketEvents {@link PacketTypeCommon} constants.
 *
 * <p>A {@link PacketId} may map to several PacketEvents types so that a single logical packet stays
 * covered across the whole 1.8 → latest range (e.g. legacy {@code PLAYER_INFO} vs the modern
 * {@code PLAYER_INFO_UPDATE}/{@code PLAYER_INFO_REMOVE} split, or attack via {@code INTERACT_ENTITY}
 * vs the dedicated {@code ATTACK} packet on 1.21.5+). PacketEvents constants always exist at compile
 * time; types absent on a given server version simply never match an incoming packet.
 */
public final class PacketTypeTranslator {
  private static final PacketTypeCommon[] EMPTY = new PacketTypeCommon[0];

  private static final Map<PacketId.Client, PacketTypeCommon[]> CLIENT = new EnumMap<>(PacketId.Client.class);
  private static final Map<PacketId.Server, PacketTypeCommon[]> SERVER = new EnumMap<>(PacketId.Server.class);

  private PacketTypeTranslator() {
  }

  private static void c(PacketId.Client id, PacketTypeCommon... types) {
    CLIENT.put(id, types);
  }

  private static void s(PacketId.Server id, PacketTypeCommon... types) {
    SERVER.put(id, types);
  }

  static {
    // ---- Client (serverbound) ----
    c(PacketId.Client.ABILITIES_IN, PacketType.Play.Client.PLAYER_ABILITIES);
    c(PacketId.Client.ADVANCEMENTS, PacketType.Play.Client.ADVANCEMENT_TAB);
    c(PacketId.Client.ATTACK_ENTITY, PacketType.Play.Client.INTERACT_ENTITY, PacketType.Play.Client.ATTACK);
    c(PacketId.Client.ARM_ANIMATION, PacketType.Play.Client.ANIMATION);
    c(PacketId.Client.AUTO_RECIPE, PacketType.Play.Client.CRAFT_RECIPE_REQUEST);
    c(PacketId.Client.BEACON, PacketType.Play.Client.SET_BEACON_EFFECT);
    c(PacketId.Client.BLOCK_DIG, PacketType.Play.Client.PLAYER_DIGGING);
    c(PacketId.Client.BLOCK_PLACE, PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT);
    c(PacketId.Client.BOAT_MOVE, PacketType.Play.Client.STEER_BOAT);
    c(PacketId.Client.B_EDIT, PacketType.Play.Client.EDIT_BOOK);
    c(PacketId.Client.CHAT_IN, PacketType.Play.Client.CHAT_MESSAGE);
    c(PacketId.Client.CLIENT_COMMAND, PacketType.Play.Client.CLIENT_STATUS);
    c(PacketId.Client.CLIENT_TICK_END, PacketType.Play.Client.CLIENT_TICK_END);
    c(PacketId.Client.CLOSE_WINDOW, PacketType.Play.Client.CLOSE_WINDOW);
    c(PacketId.Client.CUSTOM_PAYLOAD_IN, PacketType.Play.Client.PLUGIN_MESSAGE);
    c(PacketId.Client.DIFFICULTY_CHANGE, PacketType.Play.Client.SET_DIFFICULTY);
    c(PacketId.Client.DIFFICULTY_LOCK, PacketType.Play.Client.LOCK_DIFFICULTY);
    c(PacketId.Client.ENCHANT_ITEM, PacketType.Play.Client.CLICK_WINDOW_BUTTON);
    c(PacketId.Client.ENTITY_ACTION_IN, PacketType.Play.Client.ENTITY_ACTION);
    c(PacketId.Client.ENTITY_NBT_QUERY, PacketType.Play.Client.QUERY_ENTITY_NBT);
    c(PacketId.Client.FLYING, PacketType.Play.Client.PLAYER_FLYING);
    c(PacketId.Client.HELD_ITEM_SLOT_IN, PacketType.Play.Client.HELD_ITEM_CHANGE);
    c(PacketId.Client.ITEM_NAME, PacketType.Play.Client.NAME_ITEM);
    c(PacketId.Client.JIGSAW_GENERATE, PacketType.Play.Client.GENERATE_STRUCTURE);
    c(PacketId.Client.KEEP_ALIVE, PacketType.Play.Client.KEEP_ALIVE);
    c(PacketId.Client.LOOK, PacketType.Play.Client.PLAYER_ROTATION);
    c(PacketId.Client.PICK_ITEM, PacketType.Play.Client.PICK_ITEM);
    c(PacketId.Client.PONG, PacketType.Play.Client.PONG);
    c(PacketId.Client.POSITION, PacketType.Play.Client.PLAYER_POSITION);
    c(PacketId.Client.POSITION_LOOK, PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION);
    c(PacketId.Client.RECIPE_DISPLAYED, PacketType.Play.Client.SET_DISPLAYED_RECIPE);
    c(PacketId.Client.RECIPE_SETTINGS, PacketType.Play.Client.SET_RECIPE_BOOK_STATE);
    c(PacketId.Client.RESOURCE_PACK_STATUS, PacketType.Play.Client.RESOURCE_PACK_STATUS);
    c(PacketId.Client.SETTINGS, PacketType.Play.Client.CLIENT_SETTINGS);
    c(PacketId.Client.SET_COMMAND_BLOCK, PacketType.Play.Client.UPDATE_COMMAND_BLOCK);
    c(PacketId.Client.SET_COMMAND_MINECART, PacketType.Play.Client.UPDATE_COMMAND_BLOCK_MINECART);
    c(PacketId.Client.SET_CREATIVE_SLOT, PacketType.Play.Client.CREATIVE_INVENTORY_ACTION);
    c(PacketId.Client.SET_JIGSAW, PacketType.Play.Client.UPDATE_JIGSAW_BLOCK);
    c(PacketId.Client.SPECTATE, PacketType.Play.Client.SPECTATE);
    c(PacketId.Client.STEER_VEHICLE, PacketType.Play.Client.STEER_VEHICLE);
    c(PacketId.Client.STRUCT, PacketType.Play.Client.UPDATE_STRUCTURE_BLOCK);
    c(PacketId.Client.TAB_COMPLETE_IN, PacketType.Play.Client.TAB_COMPLETE);
    c(PacketId.Client.TELEPORT_ACCEPT, PacketType.Play.Client.TELEPORT_CONFIRM);
    c(PacketId.Client.TILE_NBT_QUERY, PacketType.Play.Client.QUERY_BLOCK_NBT);
    c(PacketId.Client.TRANSACTION, PacketType.Play.Client.WINDOW_CONFIRMATION);
    c(PacketId.Client.TR_SEL, PacketType.Play.Client.SELECT_TRADE);
    c(PacketId.Client.UPDATE_SIGN, PacketType.Play.Client.UPDATE_SIGN);
    c(PacketId.Client.USE_ENTITY, PacketType.Play.Client.INTERACT_ENTITY);
    c(PacketId.Client.USE_ITEM, PacketType.Play.Client.USE_ITEM);
    c(PacketId.Client.USE_ITEM_ON, PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT);
    c(PacketId.Client.VEHICLE_MOVE, PacketType.Play.Client.VEHICLE_MOVE);
    c(PacketId.Client.WINDOW_CLICK, PacketType.Play.Client.CLICK_WINDOW);

    // ---- Server (clientbound) ----
    s(PacketId.Server.ABILITIES_OUT, PacketType.Play.Server.PLAYER_ABILITIES);
    s(PacketId.Server.ADVANCEMENTS, PacketType.Play.Server.UPDATE_ADVANCEMENTS);
    s(PacketId.Server.ANIMATION, PacketType.Play.Server.ENTITY_ANIMATION);
    s(PacketId.Server.ATTACH_ENTITY, PacketType.Play.Server.ATTACH_ENTITY);
    s(PacketId.Server.AUTO_RECIPE, PacketType.Play.Server.CRAFT_RECIPE_RESPONSE);
    s(PacketId.Server.BED, PacketType.Play.Server.USE_BED);
    s(PacketId.Server.BLOCK_ACTION, PacketType.Play.Server.BLOCK_ACTION);
    s(PacketId.Server.BLOCK_BREAK, PacketType.Play.Server.ACKNOWLEDGE_PLAYER_DIGGING);
    s(PacketId.Server.BLOCK_BREAK_ANIMATION, PacketType.Play.Server.BLOCK_BREAK_ANIMATION);
    s(PacketId.Server.BLOCK_CHANGE, PacketType.Play.Server.BLOCK_CHANGE);
    s(PacketId.Server.BLOCK_CHANGED_ACK, PacketType.Play.Server.ACKNOWLEDGE_BLOCK_CHANGES);
    s(PacketId.Server.BOSS, PacketType.Play.Server.BOSS_BAR);
    s(PacketId.Server.CAMERA, PacketType.Play.Server.CAMERA);
    s(PacketId.Server.CHAT_OUT, PacketType.Play.Server.CHAT_MESSAGE, PacketType.Play.Server.SYSTEM_CHAT_MESSAGE);
    s(PacketId.Server.CLOSE_WINDOW, PacketType.Play.Server.CLOSE_WINDOW);
    s(PacketId.Server.COLLECT, PacketType.Play.Server.COLLECT_ITEM);
    s(PacketId.Server.COMBAT_EVENT, PacketType.Play.Server.COMBAT_EVENT, PacketType.Play.Server.END_COMBAT_EVENT, PacketType.Play.Server.ENTER_COMBAT_EVENT, PacketType.Play.Server.DEATH_COMBAT_EVENT);
    s(PacketId.Server.COMMANDS, PacketType.Play.Server.DECLARE_COMMANDS);
    s(PacketId.Server.CRAFT_PROGRESS_BAR, PacketType.Play.Server.WINDOW_PROPERTY);
    s(PacketId.Server.CUSTOM_PAYLOAD_OUT, PacketType.Play.Server.PLUGIN_MESSAGE);
    s(PacketId.Server.CUSTOM_SOUND_EFFECT, PacketType.Play.Server.NAMED_SOUND_EFFECT);
    s(PacketId.Server.ENTITY, PacketType.Play.Server.ENTITY_MOVEMENT);
    s(PacketId.Server.ENTITY_DESTROY, PacketType.Play.Server.DESTROY_ENTITIES);
    s(PacketId.Server.ENTITY_EFFECT, PacketType.Play.Server.ENTITY_EFFECT);
    s(PacketId.Server.ENTITY_EQUIPMENT, PacketType.Play.Server.ENTITY_EQUIPMENT);
    s(PacketId.Server.ENTITY_HEAD_ROTATION, PacketType.Play.Server.ENTITY_HEAD_LOOK);
    s(PacketId.Server.ENTITY_LOOK, PacketType.Play.Server.ENTITY_ROTATION);
    s(PacketId.Server.ENTITY_METADATA, PacketType.Play.Server.ENTITY_METADATA);
    s(PacketId.Server.ENTITY_MOVE_LOOK, PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION);
    s(PacketId.Server.ENTITY_SOUND, PacketType.Play.Server.ENTITY_SOUND_EFFECT);
    s(PacketId.Server.ENTITY_STATUS, PacketType.Play.Server.ENTITY_STATUS);
    s(PacketId.Server.ENTITY_TELEPORT, PacketType.Play.Server.ENTITY_TELEPORT);
    s(PacketId.Server.ENTITY_POSITION_SYNC, PacketType.Play.Server.ENTITY_POSITION_SYNC);
    s(PacketId.Server.ENTITY_VELOCITY, PacketType.Play.Server.ENTITY_VELOCITY);
    s(PacketId.Server.EXPERIENCE, PacketType.Play.Server.SET_EXPERIENCE);
    s(PacketId.Server.EXPLOSION, PacketType.Play.Server.EXPLOSION);
    s(PacketId.Server.GAME_STATE_CHANGE, PacketType.Play.Server.CHANGE_GAME_STATE);
    s(PacketId.Server.HELD_ITEM_SLOT_OUT, PacketType.Play.Server.HELD_ITEM_CHANGE);
    s(PacketId.Server.KEEP_ALIVE, PacketType.Play.Server.KEEP_ALIVE);
    s(PacketId.Server.KICK_DISCONNECT, PacketType.Play.Server.DISCONNECT);
    s(PacketId.Server.LIGHT_UPDATE, PacketType.Play.Server.UPDATE_LIGHT);
    s(PacketId.Server.LOGIN, PacketType.Play.Server.JOIN_GAME);
    s(PacketId.Server.LOOK_AT, PacketType.Play.Server.FACE_PLAYER);
    s(PacketId.Server.MAP, PacketType.Play.Server.MAP_DATA);
    s(PacketId.Server.MAP_CHUNK, PacketType.Play.Server.CHUNK_DATA);
    s(PacketId.Server.MAP_CHUNK_BULK, PacketType.Play.Server.MAP_CHUNK_BULK);
    s(PacketId.Server.MOUNT, PacketType.Play.Server.SET_PASSENGERS);
    s(PacketId.Server.MULTI_BLOCK_CHANGE, PacketType.Play.Server.MULTI_BLOCK_CHANGE);
    s(PacketId.Server.NAMED_ENTITY_SPAWN, PacketType.Play.Server.SPAWN_PLAYER);
    s(PacketId.Server.NAMED_SOUND_EFFECT, PacketType.Play.Server.NAMED_SOUND_EFFECT, PacketType.Play.Server.SOUND_EFFECT);
    s(PacketId.Server.NBT_QUERY, PacketType.Play.Server.NBT_QUERY_RESPONSE);
    s(PacketId.Server.OPEN_BOOK, PacketType.Play.Server.OPEN_BOOK);
    s(PacketId.Server.OPEN_SIGN_EDITOR, PacketType.Play.Server.OPEN_SIGN_EDITOR);
    s(PacketId.Server.OPEN_SIGN_ENTITY, PacketType.Play.Server.OPEN_SIGN_EDITOR);
    s(PacketId.Server.OPEN_WINDOW, PacketType.Play.Server.OPEN_WINDOW);
    s(PacketId.Server.OPEN_WINDOW_HORSE, PacketType.Play.Server.OPEN_HORSE_WINDOW);
    s(PacketId.Server.OPEN_WINDOW_MERCHANT, PacketType.Play.Server.MERCHANT_OFFERS);
    s(PacketId.Server.PING, PacketType.Play.Server.PING);
    s(PacketId.Server.PLAYER_INFO, PacketType.Play.Server.PLAYER_INFO, PacketType.Play.Server.PLAYER_INFO_UPDATE);
    s(PacketId.Server.PLAYER_INFO_REMOVE, PacketType.Play.Server.PLAYER_INFO_REMOVE);
    s(PacketId.Server.PLAYER_LIST_HEADER_FOOTER, PacketType.Play.Server.PLAYER_LIST_HEADER_AND_FOOTER);
    s(PacketId.Server.POSITION, PacketType.Play.Server.PLAYER_POSITION_AND_LOOK);
    s(PacketId.Server.RECIPES, PacketType.Play.Server.DECLARE_RECIPES);
    s(PacketId.Server.RECIPE_UPDATE, PacketType.Play.Server.UNLOCK_RECIPES);
    s(PacketId.Server.REL_ENTITY_MOVE, PacketType.Play.Server.ENTITY_RELATIVE_MOVE);
    s(PacketId.Server.REL_ENTITY_MOVE_LOOK, PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION);
    s(PacketId.Server.REMOVE_ENTITY_EFFECT, PacketType.Play.Server.REMOVE_ENTITY_EFFECT);
    s(PacketId.Server.RESOURCE_PACK_SEND, PacketType.Play.Server.RESOURCE_PACK_SEND);
    s(PacketId.Server.RESPAWN, PacketType.Play.Server.RESPAWN);
    s(PacketId.Server.SCOREBOARD_DISPLAY_OBJECTIVE, PacketType.Play.Server.DISPLAY_SCOREBOARD);
    s(PacketId.Server.SCOREBOARD_OBJECTIVE, PacketType.Play.Server.SCOREBOARD_OBJECTIVE);
    s(PacketId.Server.SCOREBOARD_SCORE, PacketType.Play.Server.UPDATE_SCORE);
    s(PacketId.Server.SCOREBOARD_TEAM, PacketType.Play.Server.TEAMS);
    s(PacketId.Server.SELECT_ADVANCEMENT_TAB, PacketType.Play.Server.SELECT_ADVANCEMENTS_TAB);
    s(PacketId.Server.SERVER_DIFFICULTY, PacketType.Play.Server.SERVER_DIFFICULTY);
    s(PacketId.Server.SET_COMPRESSION, PacketType.Play.Server.SET_COMPRESSION);
    s(PacketId.Server.SET_COOLDOWN, PacketType.Play.Server.SET_COOLDOWN);
    s(PacketId.Server.SET_SLOT, PacketType.Play.Server.SET_SLOT);
    s(PacketId.Server.SPAWN_ENTITY, PacketType.Play.Server.SPAWN_ENTITY);
    s(PacketId.Server.SPAWN_ENTITY_EXPERIENCE_ORB, PacketType.Play.Server.SPAWN_EXPERIENCE_ORB);
    s(PacketId.Server.SPAWN_ENTITY_LIVING, PacketType.Play.Server.SPAWN_LIVING_ENTITY);
    s(PacketId.Server.SPAWN_ENTITY_PAINTING, PacketType.Play.Server.SPAWN_PAINTING);
    s(PacketId.Server.SPAWN_ENTITY_WEATHER, PacketType.Play.Server.SPAWN_WEATHER_ENTITY);
    s(PacketId.Server.SPAWN_POSITION, PacketType.Play.Server.SPAWN_POSITION);
    s(PacketId.Server.STATISTIC, PacketType.Play.Server.STATISTICS);
    s(PacketId.Server.STATISTICS, PacketType.Play.Server.STATISTICS);
    s(PacketId.Server.STOP_SOUND, PacketType.Play.Server.STOP_SOUND);
    s(PacketId.Server.TAB_COMPLETE_OUT, PacketType.Play.Server.TAB_COMPLETE);
    s(PacketId.Server.TAGS, PacketType.Play.Server.TAGS);
    s(PacketId.Server.TILE_ENTITY_DATA, PacketType.Play.Server.BLOCK_ENTITY_DATA);
    s(PacketId.Server.TITLE, PacketType.Play.Server.TITLE, PacketType.Play.Server.SET_TITLE_TEXT);
    s(PacketId.Server.TRANSACTION, PacketType.Play.Server.WINDOW_CONFIRMATION);
    s(PacketId.Server.UNLOAD_CHUNK, PacketType.Play.Server.UNLOAD_CHUNK);
    s(PacketId.Server.UPDATE_ATTRIBUTES, PacketType.Play.Server.UPDATE_ATTRIBUTES);
    s(PacketId.Server.UPDATE_ENTITY_NBT, PacketType.Play.Server.UPDATE_ENTITY_NBT);
    s(PacketId.Server.UPDATE_HEALTH, PacketType.Play.Server.UPDATE_HEALTH);
    s(PacketId.Server.UPDATE_SIGN, PacketType.Play.Server.UPDATE_SIGN);
    s(PacketId.Server.UPDATE_TIME, PacketType.Play.Server.TIME_UPDATE);
    s(PacketId.Server.USE_BED, PacketType.Play.Server.USE_BED);
    s(PacketId.Server.VEHICLE_MOVE, PacketType.Play.Server.VEHICLE_MOVE);
    s(PacketId.Server.VIEW_CENTRE, PacketType.Play.Server.UPDATE_VIEW_POSITION);
    s(PacketId.Server.VIEW_DISTANCE, PacketType.Play.Server.UPDATE_VIEW_DISTANCE);
    s(PacketId.Server.WINDOW_DATA, PacketType.Play.Server.WINDOW_PROPERTY);
    s(PacketId.Server.WINDOW_ITEMS, PacketType.Play.Server.WINDOW_ITEMS);
    s(PacketId.Server.WORLD_BORDER, PacketType.Play.Server.WORLD_BORDER, PacketType.Play.Server.INITIALIZE_WORLD_BORDER);
    s(PacketId.Server.WORLD_EVENT, PacketType.Play.Server.EFFECT);
    s(PacketId.Server.WORLD_PARTICLES, PacketType.Play.Server.PARTICLE);
  }

  public static PacketTypeCommon[] translate(PacketId.Client clientPacket) {
    return CLIENT.getOrDefault(clientPacket, EMPTY);
  }

  public static PacketTypeCommon[] translate(PacketId.Server serverPacket) {
    return SERVER.getOrDefault(serverPacket, EMPTY);
  }

  /** First / primary PacketEvents type for a logical packet, or {@code null} if unmapped. */
  public static PacketTypeCommon primary(PacketId.Client clientPacket) {
    PacketTypeCommon[] types = translate(clientPacket);
    return types.length == 0 ? null : types[0];
  }

  public static PacketTypeCommon primary(PacketId.Server serverPacket) {
    PacketTypeCommon[] types = translate(serverPacket);
    return types.length == 0 ? null : types[0];
  }

  public static PacketTypeCommon[] allClientTypes() {
    return PacketType.Play.Client.values();
  }

  public static PacketTypeCommon[] allServerTypes() {
    return PacketType.Play.Server.values();
  }
}
