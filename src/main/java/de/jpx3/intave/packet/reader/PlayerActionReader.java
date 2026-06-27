package de.jpx3.intave.packet.reader;

import de.jpx3.intave.packet.converter.PlayerAction;
import de.jpx3.intave.packet.converter.PlayerActionResolver;

public final class PlayerActionReader extends AbstractPacketReader {
  public PlayerAction playerAction() {
    return PlayerActionResolver.resolveActionFromPacket(event());
  }
}
