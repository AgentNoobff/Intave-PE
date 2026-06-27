package de.jpx3.intave.packet.reader;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class EntityDestroyReader extends AbstractPacketReader implements EntityIterable {
  // PacketEvents normalizes the 1.8 array, 1.17.0 single-int and 1.17.1+ var-int-array encodings.
  private int[] entityIds() {
    return new WrapperPlayServerDestroyEntities((PacketSendEvent) event()).getEntityIds();
  }

  @Override
  public void forEach(Consumer<? super Integer> action) {
    for (int entityID : entityIds()) {
      action.accept(entityID);
    }
  }

  @NotNull
  @Override
  public SubstitutionIterator<Integer> iterator() {
    int[] entityIDs = entityIds();
    return new SubstitutionIterator<Integer>() {
      int index = 0;

      @Override
      public void set(Integer integer) {
        // entity-id remapping (decoy feature) is disabled; mutating the local copy has no effect.
        entityIDs[index - 1] = integer;
      }

      @Override
      public boolean hasNext() {
        return index < entityIDs.length;
      }

      @Override
      public Integer next() {
        return entityIDs[index++];
      }
    };
  }
}
