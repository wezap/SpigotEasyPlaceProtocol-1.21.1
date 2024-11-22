package dev.tokasen.easyplaceprotocol;

import com.comphenix.protocol.wrappers.BlockPosition;

public record PacketData(BlockPosition block, int protocolValue) {
}
