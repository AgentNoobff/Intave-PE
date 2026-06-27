package de.jpx3.intave.player.fake;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataType;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import de.jpx3.intave.packet.PacketSender;
import org.bukkit.entity.Player;

import java.util.List;

public final class MetadataAccess {
  private static final int FLAG_INDEX = 0;
  private static final int HEALTH_INDEX = 6;
  private static final int SPRINT_BYTE = 3;
  private static final int SNEAK_BYTE = 1;
  private static final int INVISIBLE_BYTE = 5;

  public static void setSprinting(
    Player player,
    FakePlayerIdentity identity,
    boolean sprinting
  ) {
    updateFlagBit(identity, SPRINT_BYTE, sprinting);
    updateMetadata(player, identity, identity.dataWatcher());
  }

  public static void setSneaking(
    Player player,
    FakePlayerIdentity identity,
    boolean sneaking
  ) {
    updateFlagBit(identity, SNEAK_BYTE, sneaking);
    updateMetadata(player, identity, identity.dataWatcher());
  }

  public static void updateVisibility(
    Player player,
    FakePlayerIdentity identity,
    boolean invisible
  ) {
    updateFlagBit(identity, INVISIBLE_BYTE, invisible);
    updateMetadata(player, identity, identity.dataWatcher());
  }

  public static void updateHealthFor(
    Player player,
    FakePlayerIdentity identity,
    float newHealth
  ) {
    for (EntityData<?> entityData : identity.dataWatcher()) {
      if (entityData.getIndex() == HEALTH_INDEX) {
        @SuppressWarnings("unchecked")
        EntityData<Float> healthData = (EntityData<Float>) entityData;
        healthData.setValue(newHealth);
      }
    }
    updateMetadata(player, identity, identity.dataWatcher());
  }

  @SuppressWarnings("unchecked")
  private static void updateFlagBit(FakePlayerIdentity identity, int bit, boolean set) {
    for (EntityData<?> entityData : identity.dataWatcher()) {
      if (entityData.getIndex() != FLAG_INDEX) {
        continue;
      }
      EntityData<Byte> flagData = (EntityData<Byte>) entityData;
      byte current = flagData.getValue();
      byte value = set
        ? (byte) (current | 1 << bit)
        : (byte) (current & ~(1 << bit));
      flagData.setValue(value);
    }
  }

  private static void updateMetadata(
    Player player,
    FakePlayerIdentity identity,
    List<EntityData<?>> dataWatcher
  ) {
    WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(
      identity.identifier(),
      dataWatcher
    );
    PacketSender.sendServerPacket(player, packet);
  }

  public static void metadataAccept(List<EntityData<?>> dataWatcher, int index, Class<?> classOfValue, Object value) {
    dataWatcher.add(new EntityData<>(index, typeFor(classOfValue), value));
  }

  @SuppressWarnings("unchecked")
  private static EntityDataType<Object> typeFor(Class<?> classOfValue) {
    EntityDataType<?> type;
    if (classOfValue == Byte.class) {
      type = EntityDataTypes.BYTE;
    } else if (classOfValue == Short.class) {
      type = EntityDataTypes.SHORT;
    } else if (classOfValue == Integer.class) {
      type = EntityDataTypes.INT;
    } else if (classOfValue == Float.class) {
      type = EntityDataTypes.FLOAT;
    } else if (classOfValue == String.class) {
      type = EntityDataTypes.STRING;
    } else if (classOfValue == Boolean.class) {
      type = EntityDataTypes.BOOLEAN;
    } else {
      throw new IllegalArgumentException("Unsupported metadata value type: " + classOfValue);
    }
    return (EntityDataType<Object>) type;
  }
}
