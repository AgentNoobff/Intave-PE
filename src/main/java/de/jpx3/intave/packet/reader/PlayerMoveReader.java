package de.jpx3.intave.packet.reader;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientVehicleMove;
import de.jpx3.intave.annotate.Nullable;
import de.jpx3.intave.share.Position;
import de.jpx3.intave.share.Rotation;

public final class PlayerMoveReader extends AbstractPacketReader {
	private WrapperPlayClientPlayerFlying flying;
	private WrapperPlayClientVehicleMove vehicle;

	@Override
	public void release() {
		flying = null;
		vehicle = null;
		super.release();
	}

	private WrapperPlayClientPlayerFlying flying() {
		if (flying == null) {
			flying = new WrapperPlayClientPlayerFlying((PacketReceiveEvent) event());
		}
		return flying;
	}

	private WrapperPlayClientVehicleMove vehicle() {
		if (vehicle == null) {
			vehicle = new WrapperPlayClientVehicleMove((PacketReceiveEvent) event());
		}
		return vehicle;
	}

	private void markChanged() {
		event().markForReEncode(true);
	}

	public boolean isVehicleMove() {
		return event().getPacketType() == PacketType.Play.Client.VEHICLE_MOVE;
	}

	public double positionX() {
		return isVehicleMove() ? vehicle().getPosition().getX() : flying().getLocation().getX();
	}

	public double positionY() {
		return isVehicleMove() ? vehicle().getPosition().getY() : flying().getLocation().getY();
	}

	public double positionZ() {
		return isVehicleMove() ? vehicle().getPosition().getZ() : flying().getLocation().getZ();
	}

	public @Nullable Position position() {
		if (!hasMovement()) {
			return null;
		}
		return new Position(positionX(), positionY(), positionZ());
	}

	public float yaw() {
		return isVehicleMove() ? vehicle().getYaw() : flying().getLocation().getYaw();
	}

	public float pitch() {
		return isVehicleMove() ? vehicle().getPitch() : flying().getLocation().getPitch();
	}

	public @Nullable Rotation rotation() {
		if (!hasRotation()) {
			return null;
		}
		return new Rotation(yaw(), pitch());
	}

	public boolean onGround() {
		return isVehicleMove() ? vehicle().isOnGround() : flying().isOnGround();
	}

	public void setOnGround(boolean onGround) {
		if (isVehicleMove()) {
			vehicle().setOnGround(onGround);
		} else {
			flying().setOnGround(onGround);
		}
		markChanged();
	}

	public void setPositionX(double x) {
		setPosition(x, positionY(), positionZ());
	}

	public void setPositionY(double y) {
		setPosition(positionX(), y, positionZ());
	}

	public void setPositionZ(double z) {
		setPosition(positionX(), positionY(), z);
	}

	public void setPosition(Position position) {
		setPosition(position.getX(), position.getY(), position.getZ());
	}

	private void setPosition(double x, double y, double z) {
		if (isVehicleMove()) {
			vehicle().setPosition(new Vector3d(x, y, z));
		} else {
			Location location = flying().getLocation();
			flying().setLocation(new Location(new Vector3d(x, y, z), location.getYaw(), location.getPitch()));
		}
		markChanged();
	}

	public void setYaw(float yaw) {
		if (isVehicleMove()) {
			vehicle().setYaw(yaw);
		} else {
			flying().getLocation().setYaw(yaw);
		}
		markChanged();
	}

	public void setPitch(float pitch) {
		if (isVehicleMove()) {
			vehicle().setPitch(pitch);
		} else {
			flying().getLocation().setPitch(pitch);
		}
		markChanged();
	}

	public boolean hasMovement() {
		return isVehicleMove() || flying().hasPositionChanged();
	}

	public boolean hasRotation() {
		return isVehicleMove() || flying().hasRotationChanged();
	}

	public boolean anyNaNOrInfiniteValue() {
		if (hasMovement()) {
			double[] values = {positionX(), positionY(), positionZ()};
			for (double value : values) {
				if (Double.isNaN(value) || Double.isInfinite(value)) {
					return true;
				}
			}
		}
		if (hasRotation()) {
			float[] values = {yaw(), pitch()};
			for (float value : values) {
				if (Float.isNaN(value) || Float.isInfinite(value)) {
					return true;
				}
			}
		}
		return false;
	}
}
