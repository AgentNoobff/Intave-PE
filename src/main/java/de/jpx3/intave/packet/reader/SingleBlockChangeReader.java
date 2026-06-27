package de.jpx3.intave.packet.reader;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;

import java.util.Collections;
import java.util.List;

public final class SingleBlockChangeReader extends AbstractPacketReader implements BlockChanges {
  private WrapperPlayServerBlockChange wrapper() {
    return new WrapperPlayServerBlockChange((PacketSendEvent) event());
  }

  @Override
  public List<Vector3i> blockPositions() {
    return Collections.singletonList(wrapper().getBlockPosition());
  }

  @Override
  public List<WrappedBlockState> blockDataList() {
    return Collections.singletonList(wrapper().getBlockState());
  }
}
