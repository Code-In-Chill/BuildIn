package org.broseidon.buildin.objects;

import com.sk89q.worldedit.world.DataException;
import org.broseidon.buildin.main.BuildIn;
import org.broseidon.buildin.main.BuildSchematic;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BuildManager {

    private Set<BuildTask> tasks;
    private BuildIn main;
    private File buildFile;
    private int maxId;
    private YamlConfiguration currentBuilds;
    private List<Integer> ids;
    private YamlConfiguration config;

    public BuildManager(BuildIn buildIn) {
        this.main = buildIn;
        buildFile = new File(main.getDataFolder() + File.separator + "currentBuilds.yml");
        config = YamlConfiguration.loadConfiguration(new File(main.getDataFolder() + File.separator + "config.yml"));

        if (!buildFile.exists())
            try {
                buildFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }


        tasks = new HashSet<>();
        ids = new ArrayList<>();
        currentBuilds = YamlConfiguration.loadConfiguration(buildFile);
        maxId = 0;
    }

    public void createNewTask(BuildSchematic schematicName, Block chestBlock, String pName) {
        BuildTask buildTask = new BuildTask(schematicName, chestBlock, pName, main);
        buildTask.setBuildTaskID(getMaxId() + 1);

        int blocksPerSecond = Math.max(1, config.getInt("Options.blocksPerSecond"));
        buildTask.runTaskTimer(main, 40, 20 / blocksPerSecond);
        tasks.add(buildTask);
        this.saveTask(buildTask);

    }

    public int getMaxId(){
        List<Integer> allIds = new ArrayList<>();
        if(currentBuilds.getKeys(false) != null || !currentBuilds.getKeys(false).isEmpty()) {
            for (String id : currentBuilds.getKeys(false)) {
                allIds.add(Integer.parseInt(id));
            }
        }

        if(allIds.isEmpty())
            return 0;

        return Collections.max(allIds);
    }



    public boolean hasTask(String pName) {
        if(currentBuilds.getKeys(false) != null || !currentBuilds.getKeys(false).isEmpty()) {
            for (String id : currentBuilds.getKeys(false)) {
                if (currentBuilds.get(id + ".player").equals(pName)) {
                    return true;
                }

            }
        }
        return false;
    }
    public void initTasks() throws IOException, DataException {
        //Deserealize tempFile, reinit all tasks
        if(currentBuilds.getKeys(false) != null || !currentBuilds.getKeys(false).isEmpty()) {
        for (String id : currentBuilds.getKeys(false)) {
            long systemTime = currentBuilds.getLong(id + ".startTime");
            //1st minute, 7min - 2 minutes
            if(((systemTime/1000) + (main.getConfig().getInt("Options.expiry-time") * 60)) - (System.currentTimeMillis()/1000) <= 0){
                currentBuilds.set(id, null);
                currentBuilds.save(buildFile);
                main.getLogger().info("Removed task " + id + " as it expired");
            }
            ids.add(Integer.parseInt(id));

         }
        }

        if(ids.isEmpty()) {
            maxId = 0;
            return;
        }


        if(Collections.max(ids) == null){
            for(int id: ids){
                maxId = id;
                break;
            }
            return;
        }

        maxId = Collections.max(ids);

    }

public BuildTask startTask(String pName) throws IOException, DataException{
        for(String id: currentBuilds.getKeys(false)){
            if(currentBuilds.get(id + ".player").equals(pName)) {

                BuildTask task = new BuildTask(new BuildSchematic(currentBuilds.getString(id + ".name"), main), getChestFromLocation((Location) currentBuilds.get(id + ".location")),
                        currentBuilds.getString(id + ".player"), main);
                task.setPlace(currentBuilds.getInt(id + ".place"));
                task.setBuildTaskID(Integer.parseInt(id));
                int blocksPerSecond = Math.max(1, main.getConfig().getInt("Options.blocksPerSecond"));
                task.runTaskTimer(main, 0, 20 / blocksPerSecond);
                tasks.add(task);
                return task;
            }
        }
        return null;
}

    public void removeTask(BuildTask task){
        tasks.remove(task);
        task.cancel();
        if(currentBuilds.getKeys(false) != null || !currentBuilds.getKeys(false).isEmpty()) {
            if(currentBuilds.getKeys(false).contains(Integer.toString(task.getBuildTaskID()))){
                currentBuilds.set(Integer.toString(task.getBuildTaskID()), null);
                try {
                    currentBuilds.save(buildFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Block getChestFromLocation(Location loc) {
        loc.add(-1, 0, -1);
        return loc.getBlock();
    }

    public Set<BuildTask> getAllTasks() {
        return tasks;
    }

    public void saveAllTasks(){
        for(BuildTask task: getAllTasks()){
            saveTask(task);
        }
    }


    public void saveTask(BuildTask task){
        currentBuilds.set(task.getBuildTaskID() + ".name", task.getSchematic().getName());
        currentBuilds.set(task.getBuildTaskID() + ".location", task.getLocation());
        currentBuilds.set(task.getBuildTaskID() + ".player", task.getOwnerName());
        currentBuilds.set(task.getBuildTaskID() + ".place", task.getPlace());
        currentBuilds.set(task.getBuildTaskID() + ".startTime", System.currentTimeMillis());


        try {
            currentBuilds.save(buildFile);
        } catch (IOException e) {
            Bukkit.getServer().getLogger().severe("Could not save build task!");
        }
    }
}
