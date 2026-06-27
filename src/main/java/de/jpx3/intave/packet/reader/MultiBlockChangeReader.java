package de.jpx3.intave.packet.reader;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange.EncodedBlock;

import java.util.ArrayList;
import java.util.List;

public final class MultiBlockChangeReader extends CompiledPacketReader implements BlockChanges {
  private List<Vector3i> blockPositions;
  private List<WrappedBlockState> blockDataList;

  @Override
  public void compile() {
    WrapperPlayServerMultiBlockChange wrapper = new WrapperPlayServerMultiBlockChange((PacketSendEvent) event());
    ClientVersion clientVersion = event().getClientVersion();
    EncodedBlock[] blocks = wrapper.getBlocks();
    blockPositions = new ArrayList<>(blocks.length);
    blockDataList = new ArrayList<>(blocks.length);
    for (EncodedBlock block : blocks) {
      // PacketEvents decodes section-relative coordinates into absolute world coordinates.
      blockPositions.add(new Vector3i(block.getX(), block.getY(), block.getZ()));
      blockDataList.add(block.getBlockState(clientVersion));
    }
  }

  @Override
  public void release() {
    super.release();
    blockPositions = null;
    blockDataList = null;
  }

  @Override
  public List<Vector3i> blockPositions() {
    return blockPositions;
  }

  @Override
  public List<WrappedBlockState> blockDataList() {
    return blockDataList;
  }
}
