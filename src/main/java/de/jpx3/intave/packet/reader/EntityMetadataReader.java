package de.jpx3.intave.packet.reader;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;

import java.util.List;

public final class EntityMetadataReader extends EntityReader {

  private WrapperPlayServerEntityMetadata metadataWrapper() {
    return new WrapperPlayServerEntityMetadata((PacketSendEvent) event());
  }

  public Object fetchRaw(int requiredIndex) {
    List<EntityData<?>> values = metadataValues();
    if (values == null || values.isEmpty()) {
      return null;
    }
    for (EntityData<?> value : values) {
      if (value.getIndex() == requiredIndex) {
        return value.getValue();
      }
    }
    return null;
  }

  // PacketEvents unifies the pre-1.19.3 WrappedWatchableObject and modern WrappedDataValue forms into
  // a single EntityData list, so the legacy/modern split the ProtocolLib reader carried collapses.
  public List<EntityData<?>> legacyMetadataObjects() {
    return metadataValues();
  }

  public void setLegacyMetadataObjects(List<EntityData<?>> values) {
    WrapperPlayServerEntityMetadata wrapper = metadataWrapper();
    wrapper.setEntityMetadata(values);
    event().markForReEncode(true);
  }

  public List<EntityData<?>> metadataValues() {
    return metadataWrapper().getEntityMetadata();
  }
}
