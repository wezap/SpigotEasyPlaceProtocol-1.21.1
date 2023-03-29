package net.dungeondev.accurateblockplacement;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.MovingObjectPositionBlock;
import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Attachable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Comparator;
import org.bukkit.block.data.type.Repeater;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AccurateBlockPlacement extends JavaPlugin implements Listener
{

    private Map<Player, PacketData> playerPacketDataHashMap = new HashMap<>();
    @Override public void onEnable() {
	Bukkit.getConsoleSender().sendMessage("AccurateBlockPlacement loaded!");
	ProtocolManager manager = ProtocolLibrary.getProtocolManager();
	manager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Client.USE_ITEM) {
	    @Override public void onPacketReceiving(final PacketEvent event) {
		onBlockBuildPacket(event);
	    }
	});
	getServer().getPluginManager().registerEvents(this, this);
    }


    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
	playerPacketDataHashMap.remove(event.getPlayer());
    }

    @EventHandler (priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBuildEvent(BlockPlaceEvent event) {
	Player player = event.getPlayer();
	PacketData packetData = playerPacketDataHashMap.get(player);
	if (packetData == null) {
	    return;
	}
	BlockPosition packetBlock = packetData.block();
	Block block = event.getBlock();
	if (packetBlock.getX() != block.getX() || packetBlock.getY() != block.getY() || packetBlock.getZ() != block.getZ()) {
	    playerPacketDataHashMap.remove(player);
	    return;
	}
	accurateBlockProtocol(event, packetData.protocolValue());
	playerPacketDataHashMap.remove(player);
    }

    private void accurateBlockProtocol(BlockPlaceEvent event, int protocolValue)
    {
	Block block = event.getBlock();
	BlockData blockData = block.getBlockData();
	if (blockData instanceof Bed) {
	    return;
	}
	if (blockData instanceof Directional) {
	    int facingIndex = protocolValue & 0xF;
	    Directional directional = (Directional) blockData;
	    if (facingIndex == 6) {
		directional.setFacing(directional.getFacing().getOppositeFace());
	    }
	    else if (facingIndex <= 5) {
		BlockFace face = null;
		Set<BlockFace> validFaces = directional.getFaces();
	    	switch (facingIndex) {
		    case 0:
			face = BlockFace.DOWN;
			break;
		    case 1:
			face = BlockFace.UP;
			break;
		    case 2:
			face = BlockFace.NORTH;
			break;
		    case 3:
			face = BlockFace.SOUTH;
			break;
		    case 4:
			face = BlockFace.WEST;
			break;
		    case 5:
			face = BlockFace.EAST;
			break;
		}
		if (validFaces.contains(face)) {
		    directional.setFacing(face);
		}
	    }
	}
	else if (blockData instanceof Orientable) {
	    Orientable orientable = (Orientable) blockData;
	    Axis[] validAxes = orientable.getAxes().toArray(new Axis[0]);
	    int axisIndex = protocolValue % 3;
	    if (axisIndex < validAxes.length) {
		orientable.setAxis(validAxes[axisIndex]);
	    }
	}
	protocolValue &= 0xFFFFFFF0;
	if (protocolValue >= 16) {
	    if (blockData instanceof Repeater) {
		Repeater repeater = (Repeater) blockData;
		int deley = protocolValue / 16;
		if (deley >= repeater.getMinimumDelay() && deley <= repeater.getMaximumDelay()) {
			repeater.setDelay(deley);
		}
	    }
	    else if (protocolValue == 16) {
		if (blockData instanceof Comparator) {
		    ((Comparator) blockData).setMode(Comparator.Mode.SUBTRACT);
		}
	    }
	}
	if (blockData.isSupported(block.getLocation())) {
	    block.setBlockData(blockData);
	}
    }

    private void onBlockBuildPacket(final PacketEvent event) {
        Player player = event.getPlayer();
        PacketContainer packet = event.getPacket();
	MovingObjectPositionBlock clickInformation = packet.getMovingBlockPositions().read(0);
	BlockPosition blockPosition = clickInformation.getBlockPosition();
	Vector posVector = clickInformation.getPosVector();

	double relativeX = posVector.getX() - blockPosition.getX();

	if (relativeX < 2)
	{
	    playerPacketDataHashMap.remove(player);
	    return;
	}
	int protocolValue = ((int) relativeX - 2) / 2;
	playerPacketDataHashMap.put(player, new PacketData(clickInformation.getBlockPosition(), protocolValue));
	int relativeInt = (int) relativeX;
	relativeX -= (relativeInt/2)*2; //Remove largest multiple of 2.
	posVector.setX(relativeX + blockPosition.getX());
	clickInformation.setPosVector(posVector);
	packet.getMovingBlockPositions().write(0, clickInformation);
    }

}
