package de.jpx3.intave.packet.reader;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerExplosion;
import de.jpx3.intave.share.Motion;

public final class ExplosionReader extends AbstractPacketReader {

	public Motion motion() {
		// PacketEvents exposes the knockback applied to the player as getPlayerMotion() across all
		// versions (on 1.21.3+ it may be absent because the server sends it in a separate packet).
		Vector3f playerMotion = new WrapperPlayServerExplosion((PacketSendEvent) event()).getPlayerMotion();
		if (playerMotion == null) {
			return null;
		}
		return new Motion(playerMotion.getX(), playerMotion.getY(), playerMotion.getZ());
	}
}
