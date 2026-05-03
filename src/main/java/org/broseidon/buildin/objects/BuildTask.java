package org.broseidon.buildin.objects;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockState;
import org.broseidon.buildin.main.BuildIn;
import org.broseidon.buildin.main.BuildSchematic;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class BuildTask extends BukkitRunnable {
    private BuildSchematic schematic;
    private int place, sizeX, sizeY, sizeZ;
    private BlockState[][][] blockArray;
    private UUID playerUUID;
    private FileConfiguration config;

    private BuildIn main;
    private int buildTaskID;

    private BuildChest buildChest;
    private Location placementLocation;
    private List<Block> originalBlocks;
    private HashMap<Block, Material> originalBlockMaterials;

    int size;
    HashMap<Block, BlockState> blocks;

    public BuildTask(BuildSchematic schematic, Block chestBlock, String pName, BuildIn main) {
        this.schematic = schematic;
        sizeX = schematic.sizeX;
        sizeY = schematic.sizeY;
        sizeZ = schematic.sizeZ;

        Chest chest = null;

        if (chestBlock.getType() != Material.CHEST)
            chestBlock.getLocation().getBlock().setType(Material.CHEST);


        chest = (Chest) chestBlock.getState();
        placementLocation = chest.getLocation().clone().add(1, 0, 1);

        blockArray = schematic.loadBlocks();

        Player player = Bukkit.getServer().getPlayer(pName);
        if (player != null) {
            this.playerUUID = player.getUniqueId();
        }
        this.main = main;
        buildTaskID = 0;
        config = main.getConfig();

        buildChest = new BuildChest(chest, getBuildTaskID());

        buildChest.setName(ChatColor.GREEN + schematic.getName());
        buildChest.update();


        blocks = new HashMap<>();
        originalBlocks = new ArrayList<>();
        originalBlockMaterials = new HashMap<>();

        //Map real-world block equivalents to base blocks
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    Block worldBlock = placementLocation.clone().add(x, y, z).getBlock();
                    blocks.put(worldBlock, blockArray[x][y][z]);
                    originalBlocks.add(worldBlock);
                    originalBlockMaterials.put(worldBlock, worldBlock.getType());
                }
            }
        }
        //Sort based on Y level for bottom-to-top placement
        originalBlocks.sort(Comparator.comparingDouble(o -> o.getLocation().getY()));
        place = 0;
        size = blocks.size();
    }

    @Override
    public void run() {
        if (place < size) {

            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null || !player.isOnline()) {
                main.getBuildManager().saveTask(this);
                this.cancel();
                return;
            }

            //For each BaseBlock get the vector of the player and place the corresponding block
            Block block = originalBlocks.get(place);
            BlockState base = blocks.get(block);

            if (base == null) {
                place++;
                return;
            }

            //Disabled by default to make plugin backwards compatible
            if (config.getBoolean("Options.sound"))
                player.playSound(placementLocation, Sound.BLOCK_GLASS_STEP, 1, 0);


            if (config.getBoolean("Options.survival-mode")) {
                Material material = BukkitAdapter.adapt(base).getMaterial();
                ItemStack stack = new ItemStack(material, 1);
                boolean isIgnoredMaterial = IgnoredMaterial.isIgnoredMaterial(stack.getType());

                if (!buildChest.containsRequirement(stack) && !isIgnoredMaterial) {
                    String newName = ChatColor.GREEN + schematic.getName() + ChatColor.RED + " Requires: " + stack.getType().toString();
                    if (!buildChest.getName().equals(newName)) {
                        buildChest.setName(newName);
                        buildChest.update();
                    }
                } else {
                    buildChest.setName(ChatColor.GREEN + schematic.getName());
                    if (!isIgnoredMaterial) {
                        buildChest.removeItemStack(stack);
                    }
                    buildChest.update();
                    place++;
                    block.setBlockData(BukkitAdapter.adapt(base), false);
                }


            } else {
                place++;
                block.setBlockData(BukkitAdapter.adapt(base), false);
            }
        } else {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null) {
                player.sendMessage(ChatColor.GREEN + Lang.COMPLETE.toString());
            }
            main.getBuildManager().removeTask(this);
            this.cancel();
        }
    }

    @Override
    public void cancel() {
        super.cancel();
    }

    public boolean isPlayerTaskOwner(String playerName){
        Player player = Bukkit.getPlayer(playerUUID);
        return player != null && playerName.equals(player.getName());
    }

    public void clearBuild(){
        for(Block b: originalBlocks){
            Material mat = originalBlockMaterials.get(b);
            b.setType(mat);
        }
    }


    public List<ItemStack> getCurrentBlocksInBuild(){
        //create a for loop ending at place and get all of the blocks in the region
        //use original blocks list and return all items on the floor.
        List<ItemStack> currentItemStacks = new ArrayList<>();
        for(Block block: originalBlocks){
            ItemStack stack = new ItemStack(block.getType(), 1);
            currentItemStacks.add(stack);
        }
        return currentItemStacks;
    }


    public void setPlace(int place) {
        this.place = place;
    }

    public int getPlace() {
        return place;
    }

    public BuildSchematic getSchematic() {
        return schematic;
    }

    public Location getLocation() {
        return placementLocation;
    }

    public String getOwnerName() {
        Player player = Bukkit.getPlayer(playerUUID);
        return player != null ? player.getName() : "Unknown";
    }

    public void setBuildTaskID(int buildTaskID){ this.buildTaskID = buildTaskID; }

    public int getBuildTaskID(){ return buildTaskID; }

    public BuildChest getBuildChest(){return buildChest; }

    public void runTaskTimer(BuildIn main, int delay, int period) {
        this.runTaskTimer((Plugin) main, (long) delay, (long) period);
    }
}
