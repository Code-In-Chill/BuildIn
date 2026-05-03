package org.broseidon.buildin.events;

import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.world.DataException;
import org.broseidon.buildin.main.*;
import org.broseidon.buildin.objects.BuildManager;
import org.broseidon.buildin.objects.Lang;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.conversations.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BlockListener implements Listener {
    private SchematicManager manager;
    private BuildSchematic buildSchematic;
    private BuildIn main;
    private ItemStack stack;
    private boolean isDone;
    private Block block;
    private String name;
    private Location playerLocation;
    private FileConfiguration config;
    private List<ItemStack> requirements;
    private RegionManager rgManager;
    private BuildManager buildManager;
    private Block chestBlock;

    public BlockListener(BuildIn buildIn) {
        main = buildIn;
        manager = main.getSchematicManager();
        config = YamlConfiguration.loadConfiguration(new File(main.getDataFolder() + File.separator + "config.yml"));
        rgManager = main.getrManager();
        buildManager = buildIn.getBuildManager();
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) throws DataException, IOException, MaxChangedBlocksException, IllegalAccessException, NoSuchFieldException {
        ItemStack stack = e.getItemInHand();

        if (isPlacedBlockBuildChest(stack)) {
            ItemMeta meta = stack.getItemMeta();
            final Player p = e.getPlayer();


            BlockFace facing = PUtils.getCardinalDirection(p.getLocation().getYaw(), false);
            e.setCancelled(true);


            String[] split;
            if (!main.getConfig().getBoolean("Options.use-lore")) {
                split = meta.getDisplayName().split("\\s+");
            } else {
                if (meta.getLore() == null) {
                    Bukkit.getLogger().severe("Lore isn't found in scematic block");
                    return;
                }
                split = meta.getLore().get(0).split("\\s+");
            }


            name = ChatColor.stripColor(split[0]);
            buildSchematic = manager.getSchematic(name);

            if (buildSchematic == null) {
                p.sendMessage(ChatColor.RED + Lang.EXISTS.toString());
                return;
            }

            this.stack = stack;
            this.block = e.getBlockPlaced();

            if (manager.hasPermission(ChatColor.stripColor(split[0])) && !p.hasPermission("in." + ChatColor.stripColor(split[0]))) {
                p.sendMessage(ChatColor.RED + Lang.NO_PERM.toString());
                return;
            }


            if (p.isConversing()) {
                p.sendRawMessage(ChatColor.RED + Lang.PREVIEW_ERROR.toString());
                return;
            }

            if (buildManager.hasTask(e.getPlayer().getName())) {
                p.sendRawMessage(ChatColor.RED + "You already have a build in progress!");
                return;
            }


            Location placementLocation = e.getBlock().getLocation();
            chestBlock = placementLocation.getBlock();
//            buildSchematic.rotateAndSetBoardOrigin(placementLocation, facing);


            try {
                if (!rgManager.canPlayerPlace(p, buildSchematic, placementLocation)) {
                    p.sendMessage(ChatColor.RED + Lang.BAD_PLACE.toString());
                    return;
                }
            } catch (Exception ignored) {
                ignored.printStackTrace();
                return;
            }


           e.setCancelled(true);

            List<ItemStack> requirements = new ArrayList<>();

            if (config.getBoolean("Options.survival-mode")) {
                if (meta.getLore().isEmpty() || meta.getLore() == null) {
                    p.sendMessage(ChatColor.RED + "This schematic has no block requirements!");
                } else {
                    //Check player inventory for requirements
                    for (String item : meta.getLore()) {
                        if (!ChatColor.stripColor(item).contains("schematic")) {
                            String[] parts = (ChatColor.stripColor(item).split("\\s+"));
                            int i = Integer.parseInt(parts[0]);
                            requirements.add(new ItemStack(Material.getMaterial(parts[1].toUpperCase()), i));
                        }
                    }
                    this.requirements = requirements;
                }
            }

            try {
                if (!rgManager.canPlayerPlace(p, buildSchematic, placementLocation)) {
                    p.sendMessage(ChatColor.RED + Lang.BAD_PLACE.toString());
                    return;
                }
            } catch (Exception ignored) {
                ignored.printStackTrace();
                return;
            }
            p.updateInventory();

            buildSchematic.preview(p, placementLocation);

            playerLocation = placementLocation;
            initConvo(p).begin();
            e.setCancelled(true);
        }
    }


    public boolean isPlacedBlockBuildChest(ItemStack placedBlock) {

        if (doesItemHaveDisplayNameOrLore(placedBlock)) {
            ItemMeta meta = placedBlock.getItemMeta();
            if (meta.hasLore() && meta.getLore().get(0).contains("schematic") || meta.hasDisplayName() && meta.getDisplayName().charAt(0) == ChatColor.COLOR_CHAR && meta.getDisplayName().contains("schematic")) {
                return true;
            }
        }

        return false;
    }


    public boolean doesItemHaveDisplayNameOrLore(ItemStack block) {
        if (!block.hasItemMeta())
            return false;

        ItemMeta meta = block.getItemMeta();

        if (meta.hasDisplayName() || meta.hasLore())
            return true;

        return false;
    }


    public Conversation initConvo(final Player p) {
        isDone = false;
        ConversationFactory fac = new ConversationFactory(main).withFirstPrompt(new ValidatingPrompt() {
            @Override
            public String getPromptText(ConversationContext conversationContext) {
                return ChatColor.GREEN + Lang.PREVIEW.toString().replace("%n", Integer.toString(config.getInt("Options.preview-time")));
            }

            @Override
            protected boolean isInputValid(ConversationContext conversationContext, String s) {
                return true;
            }

            @Override
            protected Prompt acceptValidatedInput(ConversationContext conversationContext, String s) {
                if (!s.equals("yes") && !s.equals("rotate")) {
                    buildSchematic.unloadPreview(p, playerLocation);
                    isDone = true;
                    p.sendRawMessage(ChatColor.RED + Lang.CANCEL.toString());
                    return Prompt.END_OF_CONVERSATION;
                }

                isDone = true;
                iNPlaceEvent e = new iNPlaceEvent(name, p, playerLocation, block, buildSchematic);
                Bukkit.getServer().getPluginManager().callEvent(e);
                buildSchematic.unloadPreview(p, playerLocation);

                if (e.isCancelled()) {
                    p.sendRawMessage(ChatColor.RED + Lang.CANCEL.toString());
                    return Prompt.END_OF_CONVERSATION;
                }

                buildManager.createNewTask(buildSchematic, chestBlock, p.getName());


                p.sendRawMessage(ChatColor.YELLOW + Lang.SUCCESS_PLACE.toString());
                if (stack.getAmount() > 1) {
                    ItemMeta meta = stack.getItemMeta();
                    meta.setDisplayName(ChatColor.YELLOW + buildSchematic.getName() + " schematic");
                    stack.setItemMeta(meta);
                    stack.setAmount(stack.getAmount() - 1);
                    p.setItemInHand(stack);
                    p.updateInventory();
                } else {
                    p.getInventory().removeItem(stack);
                }



                return Prompt.END_OF_CONVERSATION;

            }
        }).withTimeout(config.getInt("Options.preview-time")).withModality(true).addConversationAbandonedListener(new ConversationAbandonedListener() {
            @Override
            public void conversationAbandoned(ConversationAbandonedEvent conversationAbandonedEvent) {
                if (!isDone) {
                    buildSchematic.unloadPreview(p, playerLocation);
                    p.sendRawMessage(ChatColor.RED + "Preview time out!");
                }
            }

        });
        return fac.buildConversation(p);
    }
}
