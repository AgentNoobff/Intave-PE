package de.jpx3.intave.packet.reader;

import org.jetbrains.annotations.NotNull;

public final class CombatEventReader extends AbstractPacketReader implements EntityIterable {
  // TODO(pe-migration): the legacy single COMBAT_EVENT packet was split by PacketEvents into
  // END/ENTER/DEATH combat-event wrappers. The entity ids were only consumed by the disabled
  // entity-decoy remapping, so this reader no longer extracts them.
  public int firstEntityId() {
    return -1;
  }

  public int secondEntityId() {
    return -1;
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
