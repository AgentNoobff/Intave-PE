package de.jpx3.intave.block.variant;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import de.jpx3.intave.adapter.MinecraftVersions;
import de.jpx3.intave.block.access.BlockAccess;
import de.jpx3.intave.block.access.VolatileBlockAccess;
import de.jpx3.intave.user.User;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class BlockVariantNativeAccess {
  private static final boolean MODERN_MATERIAL_PROCESSING = MinecraftVersions.VER1_13_0.atOrAbove();

  public static void setup() {
  }

  /**
   * This method performs a direct type lookup, which will be quite heavy if the underlying chunk has not been loaded yet.
   * To avoid this performance-bottleneck, use {@link VolatileBlockAccess#variantIndexAccess(User, World, double, double, double)} instead,
   * providing fast performance, a robust cache implementation and stable chunk fallback
   */
  @Deprecated
  public static int variantAccess(Block block) {
    return BlockAccess.global().variantIndexOf(block);
  }

  public static int variantAccess(WrappedBlockState blockData) {
    if (!MODERN_MATERIAL_PROCESSING) {
      // Pre-1.13 the variant is the block-state metadata, which is the low nibble of the legacy id.
      return blockData.getGlobalId() & 0xF;
    }
    org.bukkit.block.data.BlockData bukkitBlockData = SpigotConversionUtil.toBukkitBlockData(blockData);
    Material type = bukkitBlockData.getMaterial();
    Object handle;
    try {
      // CraftBlockData#getState() returns the native IBlockData handle the variant register keys on.
      handle = bukkitBlockData.getClass().getMethod("getState").invoke(bukkitBlockData);
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to resolve native block data for " + type, exception);
    }
    int index = BlockVariantRegister.variantIndexOf(type, handle);
    if (index < 0) {
      throw new IllegalStateException("Invalid block data update: " + type + "/" + handle);
    }
    return index;
  }

  public static Object nativeVariantAccess(Block bukkitBlock) {
    return BlockAccess.global().nativeVariantOf(bukkitBlock);
  }
}
