package de.jpx3.intave.packet.reader;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;
import de.jpx3.intave.adapter.MinecraftVersions;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class WindowOpenReader extends AbstractPacketReader implements EntityIterable {
  private WrapperPlayServerOpenWindow wrapper() {
    return new WrapperPlayServerOpenWindow((PacketSendEvent) event());
  }

  public int containerId() {
    return wrapper().getContainerId();
  }

  public int slots() {
    if (MinecraftVersions.VER1_14_0.atOrAbove()) {
      int menuType = wrapper().getType();
      switch (menuType) {
        case 0:
          return 9;
        case 1:
          return 18;
        case 2:
          return 27;
        case 3:
          return 36;
        case 4:
          return 45;
        case 5:
          return 54;
        default:
          return 27;
      }
    } else {
      return wrapper().getLegacySlots();
    }
  }

  public Optional<Integer> optionalEntityId() {
    // TODO(pe-migration): the horse-window entity id lived in the legacy OPEN_WINDOW packet; the
    // modern PacketEvents wrapper does not expose it (it is the separate OPEN_HORSE_WINDOW packet).
    return Optional.empty();
  }

  @Override
  public @NotNull SubstitutionIterator<Integer> iterator() {
    return new SubstitutionIterator<Integer>() {
      @Override
      public void set(Integer integer) {
      }

      @Override
      public boolean hasNext() {
        return false;
      }

      @Override
      public Integer next() {
        throw new UnsupportedOperationException();
      }
    };
  }
}
