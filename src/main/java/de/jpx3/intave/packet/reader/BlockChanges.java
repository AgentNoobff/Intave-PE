package de.jpx3.intave.packet.reader;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;

import java.util.List;

public interface BlockChanges extends PacketReader {
  List<Vector3i> blockPositions();
  List<WrappedBlockState> blockDataList();
}
