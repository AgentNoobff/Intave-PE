package de.jpx3.intave.packet.reader;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;
import org.jetbrains.annotations.NotNull;

public final class MountEntityReader extends EntityReader implements EntityIterable {
  private WrapperPlayServerSetPassengers wrapper() {
    return new WrapperPlayServerSetPassengers((PacketSendEvent) event());
  }

  @Override
  public int entityId() {
    return wrapper().getEntityId();
  }

  public int[] mounts() {
    return wrapper().getPassengers();
  }

  @Override
  public @NotNull SubstitutionIterator<Integer> iterator() {
    int[] mounts = mounts();
    int vehicleId = entityId();
    return new SubstitutionIterator<Integer>() {
      private int slot = 0;

      @Override
      public boolean hasNext() {
        return slot < 1 + mounts.length;
      }

      @Override
      public Integer next() {
        return slot++ == 0 ? vehicleId : mounts[slot - 2];
      }

      @Override
      public void set(Integer integer) {
        // entity-id remapping (decoy feature) is disabled.
      }
    };
  }
}
