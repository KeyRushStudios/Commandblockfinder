package de.example.cbfinder;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class CommandBlockFinder extends JavaPlugin implements CommandExecutor {

    private static final int DEFAULT_RADIUS = 32;
    private static final int MAX_RADIUS = 128;
    private static final int MAX_RESULTS = 100;

    @Override
    public void onEnable() {
        getCommand("findcb").setExecutor(this);
        getLogger().info("CommandBlockFinder aktiviert.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl kann nur von einem Spieler genutzt werden.");
            return true;
        }

        int radius = DEFAULT_RADIUS;
        if (args.length > 0) {
            try {
                radius = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage("Ungültiger Radius. Benutzung: /findcb [radius]");
                return true;
            }
        }
        radius = Math.max(1, Math.min(radius, MAX_RADIUS));

        List<Location> found = scan(player.getLocation(), radius);

        if (found.isEmpty()) {
            player.sendMessage("Keine Command Blöcke im Umkreis von " + radius + " Blöcken gefunden.");
            return true;
        }

        player.sendMessage("Gefundene Command Blöcke (" + found.size() + "):");
        int shown = 0;
        for (Location loc : found) {
            if (shown++ >= MAX_RESULTS) {
                player.sendMessage("... und weitere. Radius verkleinern für eine vollständige Liste.");
                break;
            }
            Block block = loc.getBlock();
            String type = block.getType().name();
            player.sendMessage(" - " + type + " bei X: " + loc.getBlockX()
                    + ", Y: " + loc.getBlockY() + ", Z: " + loc.getBlockZ());
        }
        return true;
    }

    /**
     * Durchsucht alle geladenen Chunks im Quadrat um die Position.
     * Nur geladene Chunks werden gescannt, um Lag durch Chunk-Laden zu vermeiden.
     */
    private List<Location> scan(Location center, int radius) {
        List<Location> result = new ArrayList<>();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        int minX = cx - radius, maxX = cx + radius;
        int minZ = cz - radius, maxZ = cz + radius;
        int minY = Math.max(center.getWorld().getMinHeight(), cy - radius);
        int maxY = Math.min(center.getWorld().getMaxHeight() - 1, cy + radius);

        for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
            for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                if (!center.getWorld().isChunkLoaded(chunkX, chunkZ)) {
                    continue;
                }
                Chunk chunk = center.getWorld().getChunkAt(chunkX, chunkZ);
                int startX = Math.max(minX, chunkX << 4);
                int endX = Math.min(maxX, (chunkX << 4) + 15);
                int startZ = Math.max(minZ, chunkZ << 4);
                int endZ = Math.min(maxZ, (chunkZ << 4) + 15);

                for (int x = startX; x <= endX; x++) {
                    for (int z = startZ; z <= endZ; z++) {
                        for (int y = minY; y <= maxY; y++) {
                            Block block = chunk.getWorld().getBlockAt(x, y, z);
                            if (isCommandBlock(block.getType())) {
                                result.add(block.getLocation());
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    private boolean isCommandBlock(Material material) {
        return material == Material.COMMAND_BLOCK
                || material == Material.CHAIN_COMMAND_BLOCK
                || material == Material.REPEATING_COMMAND_BLOCK;
    }
}
