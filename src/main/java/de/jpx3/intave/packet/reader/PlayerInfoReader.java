package de.jpx3.intave.packet.reader;

/**
 * PLAYER_INFO reader.
 *
 * <p>After the PacketEvents migration the PLAYER_INFO consumers build the typed
 * {@code WrapperPlayServerPlayerInfo} / {@code WrapperPlayServerPlayerInfoUpdate} from the event
 * directly (the legacy and 1.19.3+ split are handled there), so this reader no longer exposes the
 * ProtocolLib {@code PlayerInfoData}/{@code PlayerInfoAction} accessors. It remains registered for
 * completeness of the reader registry.
 */
public final class PlayerInfoReader extends AbstractPacketReader {
}
