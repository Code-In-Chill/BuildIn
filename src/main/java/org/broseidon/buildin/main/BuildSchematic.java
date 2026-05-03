package org.broseidon.buildin.main;

import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.DataException;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import org.broseidon.buildin.objects.IgnoredMaterial;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class BuildSchematic {

    private String sName;
    private BuildIn main;

    private File schematicFile;
    public int sizeX, sizeY, sizeZ;
    private Clipboard clipboard;

    private CuboidRegion cc;

    private BlockState[][][] blockArray;

    public BuildSchematic(String schematicsName, BuildIn instance) {
        this.sName = schematicsName;
        main = instance;
        schematicFile = new File(main.getDataFolder() + File.separator + "schematics" + File.separator + schematicsName + ".schematic");

        ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);

        try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
            clipboard = reader.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        cc = clipboard.getRegion().getBoundingBox();

        blockArray = loadBlocks();
    }


    public void preview(Player p, Location placementLocation) throws IOException, DataException, MaxChangedBlocksException, NoSuchFieldException, IllegalAccessException {
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    p.sendBlockChange(placementLocation.clone().add(x, y, z), BukkitAdapter.adapt(blockArray[x][y][z]));
                }
            }
        }

    }


    public void unloadPreview(Player p, Location placementLocation) {
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    Location temp = placementLocation.clone().add(x, y, z);
                    Block b = temp.getBlock();
                    p.sendBlockChange(temp, b.getBlockData());
                }
            }
        }

    }

    public String getName() {
        return sName;
    }


    public List<String> getBlockRequirements() {
        List<String> matList = new ArrayList<>();
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    //Ok I have a list of materials
                    Material material = BukkitAdapter.adapt(blockArray[x][y][z]).getMaterial();
                    System.out.println(material);
                    if (!IgnoredMaterial.isIgnoredMaterial(material))
                        matList.add(material.toString());
                }
            }
        }

        sortMaterials(matList);
        for (String s : matList) {
            System.out.println(s);
        }

        return matList;
    }


    public CuboidRegion getRegion() {
        return cc;
    }


    private void sortMaterials(List<String> materialList) {
        int index = 0;
        boolean flag = true; //Determines when the sort is finished
        String holderName;
        while (flag) {
            flag = false;
            for (index = 0; index < materialList.size() - 1; index++) {
                //If the first material name is greater than the second go on
                if (materialList.get(index).compareToIgnoreCase(materialList.get(index + 1)) > 0) {
                    holderName = materialList.get(index);
                    materialList.set(index, materialList.get(index + 1).toUpperCase());       //Swap them, this will make them go into ascending order
                    materialList.set(index + 1, holderName.toUpperCase());
                    flag = true;
                }
            }
        }
    }

    public BlockState[][][] loadBlocks() {
        //If north blockface is null check which blockface isnt null and based on that rotate clipboard so that it isnt null
        //Reload sizes
        sizeX = clipboard.getRegion().getWidth();
        sizeY = clipboard.getRegion().getHeight();
        sizeZ = clipboard.getRegion().getLength();
        BlockState[][][] blocks = new BlockState[sizeX][sizeY][sizeZ];
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    blocks[x][y][z] = clipboard.getFullBlock(BlockVector3.at(x, y, z)).toImmutableState();
                }
            }
        }
        return blocks;
    }


//    public void rotateAndSetBoardOrigin(Location placementLocation, BlockFace facing) {
//        Block placementBlock = placementLocation.getBlock();
//        String direction = YamlConfiguration.loadConfiguration(new File(main.getDataFolder() + File.separator + "schematics.yml")).getString("Schematics." + sName);
//        int rotateValue = PUtils.getRotateValue(PUtils.parseBlockFace(direction), facing);
//
//        AffineTransform transform = new AffineTransform();
//        clipboard.setTransform(clipboard.getTransform().combine(transform.rotateY(90)));
//        cc.rotate2D(rotateValue);
//        BlockVector offset = cc.getOffset().toBlockVector();
//        cc.setOrigin(new BlockVector(placementBlock.getX() + offset.getBlockX(), placementBlock.getY() + offset.getBlockY(), placementBlock.getZ()
//                + offset.getBlockZ()));
//
//
//        blockArray = loadBlocks();
//    }

}
