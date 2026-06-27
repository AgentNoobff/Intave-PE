package de.jpx3.intave.module.tracker.entity;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityPositionSync;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMoveAndRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import de.jpx3.intave.access.IntaveInternalException;
import de.jpx3.intave.adapter.MinecraftVersions;
import de.jpx3.intave.entity.size.HitboxSize;
import de.jpx3.intave.entity.type.EntityTypeData;
import de.jpx3.intave.math.Hypot;
import de.jpx3.intave.module.feedback.FeedbackObserver;
import de.jpx3.intave.module.feedback.PendingCountingFeedbackObserver;
import de.jpx3.intave.share.*;
import de.jpx3.intave.user.User;
import de.jpx3.intave.user.meta.ProtocolMetadata;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import static de.jpx3.intave.math.MathHelper.formatDouble;

public class Entity {
  /*
  Dead entities are used to identify recently removed entities.
  Some packets are synchronized and some are processed immediately so
  this type of entity ensures that the synchrosized packets are handled correctly.
  */
  private static Entity DESTROYED_ENTITY;
  private static final boolean POSITION_PROCESSING_1_9 = MinecraftVersions.VER1_9_0.atOrAbove();
  private static final boolean POSITION_PROCESSING_1_14 = MinecraftVersions.VER1_14_0.atOrAbove();
  private static final boolean POSITION_PROCESSING_1_21_3 = MinecraftVersions.VER1_21_3.atOrAbove();
  private EntityTypeData typeData;

  private final int entityId;

  /**
   * Indicates if the entity position is synchronized with the client
   */
  public volatile boolean clientSynchronized = true;

  /**
   * Indicates that the entity should endure double-verification
   */
  public volatile boolean doubleVerification = false;

  /**
   * This value is used to interpolate the positions of the Entity
   */
  public long serverPosX, serverPosY, serverPosZ;

  // Immediate values
  public long immServerPosX, immServerPosY, immServerPosZ;
  public final Position immediateServerPosition = Position.mutableEmpty();

  public EntityPositionContext position;
  public EntityPositionContext lastPosition;
  public EntityPositionContext alternativePosition;
  public final Deque<String> positionChanges = new ArrayDeque<>(11);
  public final ReentrantLock debugPushLock = new ReentrantLock();

  public PositionDeltaCodec immediateCodec = new PositionDeltaCodec();
  public PositionDeltaCodec codec = new PositionDeltaCodec();

  public HistoryWindow<EntityPositionContext> positionHistory = new HistoryWindow<>(25);
  public boolean dead, fakeDead;
  public boolean verifiedPosition;
  public float health;
  public int ticksAlive;
  public final boolean isPlayer;
  private int deathTime;
  private Entity vehicle;
  private List<Entity> passengers = new ArrayList<>();
  private BoundingBox boundingBox;
  private boolean enabledResponseTracing;
  private boolean ticked;
  private boolean wasTracedLastCycle;

  // experimental stuff
  public int duplicationId;

  /**
   * Internal value - do not change
   */
  public double distanceToPlayerCache;
  private boolean temporaryCopy;

  private final PendingCountingFeedbackObserver feedbackTracker;

  public Entity(
    int entityId,
    @NotNull EntityTypeData typeData,
    boolean isPlayer
  ) {
    this.isPlayer = isPlayer;
    this.entityId = entityId;
    this.typeData = typeData;

    this.position = new EntityPositionContext();
    this.lastPosition = new EntityPositionContext();
    this.alternativePosition = new EntityPositionContext();
    this.feedbackTracker = new PendingCountingFeedbackObserver();
  }

  public boolean hasTypeData() {
    return typeData != null;
  }

  public EntityTypeData typeData() {
    return typeData;
  }

  public void setTypeData(EntityTypeData typeData) {
    this.typeData = typeData;
  }

  public static class EntityPositionContext implements Cloneable {
    public double prevPosX, prevPosY, prevPosZ;
    public double posX, posY, posZ;
    public double newPosX, newPosY, newPosZ;
    public int newPosRotationIncrements;

    public final long created = System.currentTimeMillis();

    @Override
    public EntityPositionContext clone() {
      try {
        return (EntityPositionContext) super.clone();
      } catch (CloneNotSupportedException exception) {
        throw new IntaveInternalException(exception);
      }
    }

    @Override
    public String toString() {
      return "[" + posX + "," + posY + "," + posZ + "]";
    }

    public Position toPosition() {
      return new Position(posX, posY, posZ);
    }
  }

  public void onUpdate() {
    this.ticksAlive++;
    this.onEntityUpdate();
    this.onLivingUpdate();
  }

  /**
   * Updated after a player sends his movement.
   * This method is required because the client is using a player's rotationYaw and rotationPitch
   * which will be sent in the next tick and therefore is not accessible at this moment.
   */
  public void entityPlayerMoveUpdate() {
  }

  /**
   * Interpolates the position of the entity between the position and the new position to make the entity move smoothly.
   * This method applies if the given entity has an instance of LivingEntity. Packets: (All types of movement packets)
   * FLYING, LOOK, POSITION, POSITION_LOOK
   */
  void onLivingUpdate() {
    if (typeData().isLivingEntity() && position.newPosRotationIncrements > 0) {
      double newPosX = position.posX + (position.newPosX - position.posX) / (double) position.newPosRotationIncrements;
      double newPosY = position.posY + (position.newPosY - position.posY) / (double) position.newPosRotationIncrements;
      double shiftedNewY = alternativePosition.posY + (alternativePosition.newPosY - alternativePosition.posY) / (double) position.newPosRotationIncrements;
      double newPosZ = position.posZ + (position.newPosZ - position.posZ) / (double) position.newPosRotationIncrements;

      --position.newPosRotationIncrements;
      setPosition(newPosX, newPosY, newPosZ);
      setShiftedPositionY(shiftedNewY);
      pushDebug("LERP(" + position.newPosRotationIncrements + ") to " + formatDouble(newPosX, 3) + " " + formatDouble(newPosY, 3) + " " + formatDouble(newPosZ, 3));
    }
  }

  public void pushDebug(String message) {
    try {
      debugPushLock.lock();
      if (positionChanges.size() > 10) {
        positionChanges.removeFirst();
      }
      positionChanges.add(message);
    } finally {
      debugPushLock.unlock();
    }
  }

  private void onEntityUpdate() {
    if (this.health <= 0.0) {
      onDeathUpdate();
    }
  }

  private void onDeathUpdate() {
    ++this.deathTime;
    if (this.deathTime == 20) {
      this.dead = true;
    }
  }

  public void immediateEntityTeleport(User user, ProtocolPacketEvent event) {
    // PacketEvents normalizes the teleport position to absolute block coordinates across all versions.
    // TODO(pe-migration): relative-flag teleports (1.21.2+) are treated as absolute here.
    Vector3d teleportPosition = new WrapperPlayServerEntityTeleport((PacketSendEvent) event).getPosition();
    double newPosX = teleportPosition.getX();
    double newPosY = teleportPosition.getY();
    double newPosZ = teleportPosition.getZ();
    immServerPosX = ClientMath.positionLong(newPosX);
    immServerPosY = ClientMath.positionLong(newPosY);
    immServerPosZ = ClientMath.positionLong(newPosZ);
    // Always set on 1.16+ as they removed the threshold
    boolean samePosition =
      Math.abs(immediateServerPosition.getX() - newPosX) < 0.03125d &&
        Math.abs(immediateServerPosition.getY() - newPosY) < 0.015625d &&
        Math.abs(immediateServerPosition.getZ() - newPosZ) < 0.03125d;
    if (samePosition && user.protocolVersion() < 735 /* 1.16 protocol version */) {
      return;
    }
    immediateServerPosition.setX(newPosX);
    immediateServerPosition.setY(newPosY);
    immediateServerPosition.setZ(newPosZ);
  }

  /**
   * Handles a teleportation. Packets: ENTITY_TELEPORT
   *
   * @param user   user which received the packet
   * @param packet contains information about the entity teleportation
   */
  public void handleEntityTeleport(User user, ProtocolPacketEvent event) {
    // PacketEvents normalizes the teleport position to absolute block coordinates across all versions.
    // TODO(pe-migration): relative-flag teleports (1.21.2+) are treated as absolute here.
    Vector3d teleportPosition = new WrapperPlayServerEntityTeleport((PacketSendEvent) event).getPosition();
    double newPosX = teleportPosition.getX();
    double newPosY = teleportPosition.getY();
    double newPosZ = teleportPosition.getZ();
    serverPosX = ClientMath.positionLong(newPosX);
    serverPosY = ClientMath.positionLong(newPosY);
    serverPosZ = ClientMath.positionLong(newPosZ);
    boolean immediateTeleport = POSITION_PROCESSING_1_21_3 && squaredDistanceTo(newPosX, newPosY, newPosZ) > 4096;

    ProtocolMetadata protocol = user.meta().protocol();
    protocol.lastEntityId = entityId;
    protocol.lastEntityPosition = new Position(
      newPosX, newPosY, newPosZ
    );

    if (immediateTeleport) {
      setPosition(newPosX, newPosY, newPosZ);
      pushDebug("TP(Set position) to " + formatDouble(newPosX, 3) + " " + formatDouble(newPosY, 3) + " " + formatDouble(newPosZ, 3));
    } else {
      // Always set on 1.16+ as they removed the threshold
      boolean samePosition =
        Math.abs(position.posX - newPosX) < 0.03125d
          && Math.abs(position.posY - newPosY) < 0.015625d
          && Math.abs(position.posZ - newPosZ) < 0.03125d;
      if (samePosition && user.protocolVersion() < 735 /* 1.16 protocol version */) {
        setPositionAndRotationEntityLiving(position.posX, position.posY, position.posZ, 3);
      } else {
        setPositionAndRotationEntityLiving(newPosX, newPosY, newPosZ, 3);
      }
      pushDebug("TP(Set lerp target) to " + formatDouble(newPosX, 3) + " " + formatDouble(newPosY, 3) + " " + formatDouble(newPosZ, 3));
    }
    double alternativeNewPosY = newPosY + 0.015625d;
    if (Math.abs(position.posX - newPosX) < 0.03125d &&
      Math.abs(alternativePosition.posY - alternativeNewPosY) < 0.015625d &&
      Math.abs(position.posZ - newPosZ) < 0.03125d) {
      setAlternativeYPosition(alternativePosition.posY);
    } else {
      setAlternativeYPosition(alternativeNewPosY);
    }
  }

  public void handleEntityPositionSync(User user, ProtocolPacketEvent event) {
    Vector3d syncPosition = new WrapperPlayServerEntityPositionSync((PacketSendEvent) event).getValues().getPosition();
    Position position = new Position(syncPosition.getX(), syncPosition.getY(), syncPosition.getZ());
    double newPosX = position.getX();
    double newPosY = position.getY();
    double newPosZ = position.getZ();
    codec.setBase(position);
    serverPosX = ClientMath.positionLong(newPosX);
    serverPosY = ClientMath.positionLong(newPosY);
    serverPosZ = ClientMath.positionLong(newPosZ);
    boolean instantTeleport = squaredDistanceTo(newPosX, newPosY, newPosZ) > 4096;
    if (instantTeleport) {
      setPosition(newPosX, newPosY, newPosZ);
      pushDebug("TP(Set position) to " + formatDouble(newPosX, 3) + " " + formatDouble(newPosY, 3) + " " + formatDouble(newPosZ, 3));
    } else {
      setPositionAndRotationEntityLiving(newPosX, newPosY, newPosZ, 3);
      pushDebug("TP(Set lerp target) to " + formatDouble(newPosX, 3) + " " + formatDouble(newPosY, 3) + " " + formatDouble(newPosZ, 3));
    }
    if (entityName().toLowerCase().contains("chicken")) {
      ProtocolMetadata protocol = user.meta().protocol();
      protocol.lastEntityId = entityId;
      protocol.lastEntityPosition = position;
//      System.out.println("A-sync " + (System.currentTimeMillis() % 1000) + " " + entityName() + "/" + entityId + " " + position);
    }
  }

  public void immediateEntityPositionSync(ProtocolPacketEvent event) {
    Vector3d syncPosition = new WrapperPlayServerEntityPositionSync((PacketSendEvent) event).getValues().getPosition();
    Position position = new Position(syncPosition.getX(), syncPosition.getY(), syncPosition.getZ());
    double newPosX = position.getX();
    double newPosY = position.getY();
    double newPosZ = position.getZ();
    immediateCodec.setBase(position);
    immediateServerPosition.setX(newPosX);
    immediateServerPosition.setY(newPosY);
    immediateServerPosition.setZ(newPosZ);
  }

  private double squaredDistanceTo(double newX, double newY, double newZ) {
    double d = newX - position.posX;
    double e = newY - position.posY;
    double f = newZ - position.posZ;
    return d * d + e * e + f * f;
  }

  // PacketEvents reports relative-move deltas as block doubles for every version; positionLong scales
  // them back into the shared 1/4096-block accumulator (exact for the 1.8 1/32 and 1.9+ 1/4096 wire
  // encodings alike).
  private Vector3d relativeMoveDelta(ProtocolPacketEvent event) {
    if (event.getPacketType() == PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION) {
      WrapperPlayServerEntityRelativeMoveAndRotation wrapper = new WrapperPlayServerEntityRelativeMoveAndRotation((PacketSendEvent) event);
      return new Vector3d(wrapper.getDeltaX(), wrapper.getDeltaY(), wrapper.getDeltaZ());
    } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_RELATIVE_MOVE) {
      WrapperPlayServerEntityRelativeMove wrapper = new WrapperPlayServerEntityRelativeMove((PacketSendEvent) event);
      return new Vector3d(wrapper.getDeltaX(), wrapper.getDeltaY(), wrapper.getDeltaZ());
    }
    // ENTITY_ROTATION (look only) carries no positional delta.
    return new Vector3d(0, 0, 0);
  }

  public void immediateEntityMovement(ProtocolPacketEvent event) {
    Vector3d delta = relativeMoveDelta(event);
    this.immServerPosX += ClientMath.positionLong(delta.getX());
    this.immServerPosY += ClientMath.positionLong(delta.getY());
    this.immServerPosZ += ClientMath.positionLong(delta.getZ());
    immediateServerPosition.setX((double) immServerPosX / 4096d);
    immediateServerPosition.setY((double) immServerPosY / 4096d);
    immediateServerPosition.setZ((double) immServerPosZ / 4096d);
  }

  /**
   * Handles relative movement. Packets: REL_ENTITY_MOVE, REL_ENTITY_MOVE_LOOK or ENTITY_LOOK
   *
   * @param packet contains information about the entity movement
   */
  public void handleEntityMovement(User user, ProtocolPacketEvent event, boolean sync) {
    Vector3d delta = relativeMoveDelta(event);
    this.serverPosX += ClientMath.positionLong(delta.getX());
    this.serverPosY += ClientMath.positionLong(delta.getY());
    this.serverPosZ += ClientMath.positionLong(delta.getZ());
    double newPosX = (double) serverPosX / 4096d;
    double newPosY = (double) serverPosY / 4096d;
    double alternativeNewPosY = newPosY;
    double newPosZ = (double) serverPosZ / 4096d;

    // 3 is used to interpolate the entity position in new client ticks
    setPositionAndRotationEntityLiving(newPosX, newPosY, newPosZ, 3);
    setAlternativeYPosition(alternativeNewPosY);
    pushDebug("REL(Set lerp target) to " + formatDouble(newPosX, 3) + " " + formatDouble(newPosY, 3) + " " + formatDouble(newPosZ, 3));
  }

  /**
   * Used to set the position of an entity and moves its {@link BoundingBox}. On the client side this is also
   * applied for rotation changes.
   */
  public void setPositionAndRotationSpawnMob(double x, double y, double z, double alternativeY) {
    position.prevPosX = position.posX = x;
    position.prevPosY = position.posY = y;
    alternativePosition.prevPosY = alternativePosition.posY = alternativeY;
    position.prevPosZ = position.posZ = z;

    setPosition(position.posX, position.posY, position.posZ);
    setShiftedPositionY(alternativePosition.posY);
  }

  /**
   * Sets the position of the entity.
   */
  public void setPosition(double x, double y, double z) {
    updatePositionHistory();
    lastPosition.posX = position.posX;
    lastPosition.posY = position.posY;
    lastPosition.posZ = position.posZ;
    position.posX = x;
    position.posY = y;
    position.posZ = z;
    boundingBox = null;
  }

  private synchronized void updatePositionHistory() {
    if (!temporaryCopy) {
      try {
//        positionHistoryLock.lock();
//        if (positionHistory.size() > 25) {
//          positionHistory.remove(0);
//        }
        positionHistory.add(position.clone());
      } finally {
//        positionHistoryLock.unlock();
      }
    }
  }

  public double serverClientPositionOffset() {
    return distance(
      immediateServerPosition.getX(),
      immediateServerPosition.getY(),
      immediateServerPosition.getZ()
    );
  }

  public void setShiftedPositionY(double alternativeNewPosY) {
    alternativePosition.posY = alternativeNewPosY;
  }

  public double distance(double x, double y, double z) {
    double deltaX = Math.abs(x - position.posX);
    double deltaY = Math.abs(y - position.posY);
    double deltaZ = Math.abs(z - position.posZ);
    return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
  }

  public double distanceTo(Vector position) {
    return distance(position.getX(), position.getY(), position.getZ());
  }

  /**
   * Sets the position of the entity and the newPosRotationIncrements which is used to interpolate the entity position
   * in new ticks (Client side it also updates the rotation of the entity)
   *
   * @param newPosRotationIncrements the value which is used to interpolate the movement of the entity in new ticks
   */
  public void setPositionAndRotationEntityLiving(double x, double y, double z, int newPosRotationIncrements) {
    if (!typeData().isLivingEntity()) {
      setPosition(x, y, z);
      return;
    }
    position.newPosX = x;
    position.newPosY = y;
    position.newPosZ = z;
    updatePositionHistory();
    position.newPosRotationIncrements = newPosRotationIncrements;
  }

  public void setAlternativeYPosition(double alternativeY) {
    if (!typeData().isLivingEntity()) {
      setShiftedPositionY(alternativeY);
      return;
    }
    alternativePosition.newPosY = alternativeY;
  }

  public boolean moving(double distance) {
    EntityPositionContext positions = this.position;
    return Hypot.fast(positions.newPosX - positions.posX, positions.newPosZ - positions.posZ) >= distance;
  }

  public boolean isEntityAlive() {
    return !this.dead && (this.health > 0.0f || Float.isNaN(this.health));
  }

  public List<Entity> passengers() {
    return passengers;
  }

  public void clearPassengers() {
    passengers.clear();
  }

  public void addPassenger(Entity entity) {
    passengers.add(entity);
  }

  public void removePassenger(Entity passenger) {
    passengers.remove(passenger);
  }

  public void mountToEntity(Entity mountedOnEntity) {
    this.vehicle = mountedOnEntity;
  }

  public void unmountFromEntity() {
    vehicle = null;
  }

  public Entity vehicle() {
    return vehicle;
  }

  public boolean isInVehicle() {
    return vehicle != null;
  }

  public boolean tracingEnabled() {
    return enabledResponseTracing;
  }

  public boolean isTicked() {
    return ticked;
  }

  public void setTicked(boolean ticked) {
    this.ticked = ticked;
  }

  public boolean wasTracedLastCycle() {
    return wasTracedLastCycle;
  }

  public void setResponseTracingEnabled(boolean enabledResponseTracing) {
    this.wasTracedLastCycle = this.enabledResponseTracing;
    this.enabledResponseTracing = enabledResponseTracing;
  }

  public FeedbackObserver feedbackTracker() {
    return feedbackTracker;
  }

  public long pendingFeedbackPackets() {
    return feedbackTracker.pending();
  }

  public List<String> positionChanges() {
    return new ArrayList<>(positionChanges);
  }

  /**
   * Returns the type name of this entity.
   */
  public String entityName() {
    return typeData().name();
  }

  public int entityId() {
    return entityId;
  }

  /**
   * Resolves the current {@link BoundingBox} of the entity.
   *
   * @return the {@link BoundingBox}
   */
  public BoundingBox boundingBox() {
    if (boundingBox != null) {
      return boundingBox;
    }
    boundingBox = entityBoundingBoxFrom(position, this);
    return boundingBox;
  }

  public static BoundingBox entityBoundingBoxFrom(EntityPositionContext position, Entity entity) {
    double x = position.posX;
    double y = position.posY;
    double z = position.posZ;
    if (entity == null) {
      return new BoundingBox(x, y, z, x, y, z);
    }
    EntityTypeData typeData = entity.typeData();
    double halfWidth = typeData.size().width() / 2.0;
    double length = typeData.size().height();
    return new BoundingBox(
      x - halfWidth, y, z - halfWidth,
      x + halfWidth, y + length, z + halfWidth
    );
  }

  public static void setup() {
    /*
      Avoid possible class order deadlock
     */
    DESTROYED_ENTITY = new Destroyed();
  }

  /*
  Dead entities are used to identify recently removed entities.

  Some packets are synchronized and some are processed immediately so
  this type of entity ensures that the synchrosized packets are handled correctly.
   */
  public static Entity destroyedEntity() {
    return DESTROYED_ENTITY;
  }

  public static final class Destroyed extends Entity {
    public Destroyed() {
      super(0, new EntityTypeData("destroyed", HitboxSize.zero(), -1, false, 8), false);
    }

    @Override
    void onLivingUpdate() {
    }
  }

  @Override
  public String toString() {
    if (this == DESTROYED_ENTITY) {
      return "E[Destroyed]";
    }
    if (typeData == null) {
      return "E[" + entityId + "]";
    }
    return "E[" + entityId + "/" + typeData.name() + "]";
  }
}