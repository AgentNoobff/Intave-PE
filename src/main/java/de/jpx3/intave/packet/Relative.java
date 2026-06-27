package de.jpx3.intave.packet;

import com.google.common.collect.Maps;
import de.jpx3.intave.klass.locate.Locate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Teleport relative-movement flags.
 *
 * <p>Each constant's {@link #index()} bit matches the vanilla teleport relative-flag mask, which is
 * exactly what PacketEvents exposes through {@code WrapperPlayServerPlayerPositionAndLook}'s
 * {@code getRelativeMask()}/{@code setRelativeMask(byte)} and {@code RelativeFlag}. The migration
 * therefore replaced the old ProtocolLib NMS-enum conversion with plain mask arithmetic.
 */
public enum Relative {
  X(0),
  Y(1),
  Z(2),
  Y_ROT(3),
  X_ROT(4),
  DELTA_X(5),
  DELTA_Y(6),
  DELTA_Z(7),
  ROTATE_DELTA(8);

  public static final Set<Relative> ALL_RELATIVE = new HashSet<>(Arrays.asList(values()));
  public static final Set<Relative> RELATIVE_POSITION = new HashSet<>(Arrays.asList(X, Y, Z));
  public static final Set<Relative> RELATIVE_ROTATION = new HashSet<>(Arrays.asList(Y_ROT, X_ROT));
  public static final Set<Relative> RELATIVE_MOTION = new HashSet<>(Arrays.asList(DELTA_X, DELTA_Y, DELTA_Z, ROTATE_DELTA));

  private final int slot;

  Relative(int slot) {
    this.slot = slot;
  }

  public int index() {
    return 1 << this.slot;
  }

  /** Decode a vanilla teleport relative-flag mask (e.g. from {@code getRelativeMask()}). */
  public static Set<Relative> fromMask(int mask) {
    Set<Relative> result = EnumSet.noneOf(Relative.class);
    for (Relative flag : values()) {
      if ((mask & flag.index()) != 0) {
        result.add(flag);
      }
    }
    return result;
  }

  /** Encode a set of flags into a vanilla teleport relative-flag mask. */
  public static int toMask(Set<Relative> flags) {
    int mask = 0;
    for (Relative flag : flags) {
      mask |= flag.index();
    }
    return mask;
  }

  public static int maskOfAllFlags() {
    return 0b11111;
  }

  public static int maskOfMovementChange() {
    return 0b00111;
  }

  public static int maskOfNoRotationChange() {
    return 0b11000;
  }

  // The following build native NMS teleport-flag sets via reflection (not ProtocolLib); they are used
  // by the internal NMS teleport path in module/mitigate.
  private static final Map<Integer, Set<?>> flagCache = Maps.newConcurrentMap();

  public static Set<?> nativeSetOfAllFlags() {
    return nativeFromIndex(maskOfAllFlags());
  }

  public static Set<?> nativeSetOfMovementChange() {
    return nativeFromIndex(maskOfMovementChange());
  }

  public static Set<?> nativeSetOfNoRotationChange() {
    return nativeFromIndex(maskOfNoRotationChange());
  }

  public static Set<?> fromSet(Set<Relative> flags) {
    return nativeFromIndex(toMask(flags));
  }

  public static Set<?> nativeFromIndex(int index) {
    return flagCache.computeIfAbsent(index, integer -> {
      try {
        Method resolverMethod = Locate.methodByKey(
          "PacketPlayOutPosition$EnumPlayerTeleportFlags",
          "unpack(I)Ljava/util/Set;"
        );
        return (Set<?>) resolverMethod.invoke(null, index);
      } catch (InvocationTargetException | IllegalAccessException exception) {
        throw new IllegalStateException("Something is wrong");
      }
    });
  }
}
