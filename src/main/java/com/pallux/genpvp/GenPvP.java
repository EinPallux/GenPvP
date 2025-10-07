package com.pallux.genpvp;

import com.pallux.genpvp.commands.GenPvPCommand;
import com.pallux.genpvp.commands.AliasCommands;
import com.pallux.genpvp.listeners.*;
import com.pallux.genpvp.managers.*;
import com.pallux.genpvp.placeholders.GenPvPPlaceholders;
import com.pallux.genpvp.utils.ColorUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Level;

public class GenPvP extends JavaPlugin {

    private static GenPvP instance;

    // Managers
    private ConfigManager configManager;
    private DataManager dataManager;
    private GeneratorManager generatorManager;
    private LevelManager levelManager;
    private CubeManager cubeManager;
    private StatisticsManager statisticsManager;
    private ArmorManager armorManager;
    private WorldGuardManager worldGuardManager;
    private DefenseManager defenseManager;
    private DefenseDataManager defenseDataManager;
    private RaidManager raidManager;

    // Economy
    private Economy economy;

    // PlaceholderAPI support
    private boolean placeholderAPIEnabled = false;

    // Listeners
    private ArmorListener armorListener;

    // Tasks
    private BukkitTask armorEffectTask;

    @Override
    public void onLoad() {
        // WorldGuard flags MUST be registered in onLoad()
        try {
            Class.forName("com.sk89q.worldguard.WorldGuard");
            worldGuardManager = new WorldGuardManager(this);
            worldGuardManager.registerFlags();
        } catch (ClassNotFoundException e) {
            getLogger().warning("WorldGuard not found! Generator protection will not be available.");
        }
    }

    @Override
    public void onEnable() {
        instance = this;

        // ASCII Art Banner
        getLogger().info("  ____            ____       ____  ");
        getLogger().info(" / ___| ___ _ __ |  _ \\ __  _|  _ \\ ");
        getLogger().info("| |  _ / _ \\ '_ \\| |_) |\\ \\/ / |_) |");
        getLogger().info("| |_| |  __/ | | |  __/  >  <|  __/ ");
        getLogger().info(" \\____|\\___|_| |_|_|    /_/\\_\\_|    ");
        getLogger().info("");
        getLogger().info("GenPvP v" + getDescription().getVersion());
        getLogger().info("Author: Pallux");
        getLogger().info("");

        // Setup Vault Economy
        if (!setupEconomy()) {
            getLogger().severe("Vault or an economy plugin not found! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize managers
        initManagers();

        // Register commands
        registerCommands();

        // Register listeners
        registerListeners();

        // Setup PlaceholderAPI
        setupPlaceholderAPI();

        // Start generator task
        generatorManager.startGeneratorTask();

        // Start auto-save task
        dataManager.startAutoSaveTask();

        // Start armor effect task
        startArmorEffectTask();

        getLogger().info("GenPvP has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        // Save all data
        if (dataManager != null) {
            dataManager.saveAllData();
            getLogger().info("All player data saved!");
        }

        // Save defense blocks
        if (defenseDataManager != null) {
            defenseDataManager.saveDefenseBlocks();
            getLogger().info("All defense blocks saved!");
        }

        // Stop tasks
        if (generatorManager != null) {
            generatorManager.stopGeneratorTask();
        }

        if (armorEffectTask != null) {
            armorEffectTask.cancel();
        }

        getLogger().info("GenPvP has been disabled!");
    }

    private void initManagers() {
        getLogger().info("Initializing managers...");

        // Config manager must be first
        configManager = new ConfigManager(this);
        configManager.loadConfigs();

        // WorldGuard manager (must be before listeners)
        worldGuardManager = new WorldGuardManager(this);

        // Data manager
        dataManager = new DataManager(this);
        dataManager.loadAllData();

        // Defense data manager
        defenseDataManager = new DefenseDataManager(this);
        defenseDataManager.loadDefenseBlocks();

        // Game managers
        generatorManager = new GeneratorManager(this);
        levelManager = new LevelManager(this);
        cubeManager = new CubeManager(this);
        statisticsManager = new StatisticsManager(this);
        armorManager = new ArmorManager(this);
        defenseManager = new DefenseManager(this);
        raidManager = new RaidManager(this);

        getLogger().info("All managers initialized!");
    }

    private void registerCommands() {
        getLogger().info("Registering commands...");

        GenPvPCommand mainCommand = new GenPvPCommand(this);
        getCommand("gpvp").setExecutor(mainCommand);
        getCommand("gpvp").setTabCompleter(mainCommand);

        // Register alias commands
        AliasCommands aliasCommands = new AliasCommands(this);

        // Register each alias command individually
        if (getCommand("level") != null) {
            getCommand("level").setExecutor(aliasCommands);
        }
        if (getCommand("levelup") != null) {
            getCommand("levelup").setExecutor(aliasCommands);
        }
        if (getCommand("gens") != null) {
            getCommand("gens").setExecutor(aliasCommands);
        }
        if (getCommand("generators") != null) {
            getCommand("generators").setExecutor(aliasCommands);
        }
        if (getCommand("gems") != null) {
            getCommand("gems").setExecutor(aliasCommands);
        }
        if (getCommand("stats") != null) {
            getCommand("stats").setExecutor(aliasCommands);
        }
        if (getCommand("armor") != null) {
            getCommand("armor").setExecutor(aliasCommands);
        }
        if (getCommand("armory") != null) {
            getCommand("armory").setExecutor(aliasCommands);
        }
        if (getCommand("shop") != null) {
            getCommand("shop").setExecutor(aliasCommands);
        }

        getLogger().info("Commands registered!");
    }

    private void registerListeners() {
        getLogger().info("Registering listeners...");

        getServer().getPluginManager().registerEvents(new GeneratorListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new NuggetListener(this), this);
        getServer().getPluginManager().registerEvents(new CubeListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new DefenseListener(this), this);
        getServer().getPluginManager().registerEvents(new RaidListener(this), this);

        // Register armor listeners
        armorListener = new ArmorListener(this);
        getServer().getPluginManager().registerEvents(armorListener, this);
        getServer().getPluginManager().registerEvents(new ArmorEquipListener(this, armorListener), this);

        getLogger().info("Listeners registered!");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }

        economy = rsp.getProvider();
        getLogger().info("Vault economy hooked successfully!");
        return economy != null;
    }

    private void setupPlaceholderAPI() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                new GenPvPPlaceholders(this).register();
                placeholderAPIEnabled = true;
                getLogger().info("PlaceholderAPI hooked successfully!");
                getLogger().info("Registered placeholders with prefix: %gpvp_");
            } catch (Exception e) {
                getLogger().severe("Failed to register PlaceholderAPI expansion!");
                e.printStackTrace();
            }
        } else {
            getLogger().warning("PlaceholderAPI not found! Placeholders will not work.");
        }
    }

    private void startArmorEffectTask() {
        int interval = configManager.getArmorEffectUpdateInterval();

        armorEffectTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                armorListener.updateArmorEffects(player);
            }
        }, interval, interval);

        getLogger().info("Armor effect task started!");
    }

    public void reload() {
        getLogger().info("Reloading GenPvP...");

        // Save data before reload
        dataManager.saveAllData();
        defenseDataManager.saveDefenseBlocks();

        // Reload configs
        configManager.loadConfigs();

        // Reload managers
        generatorManager.reload();
        levelManager.reload();
        cubeManager.reload();
        armorManager.reload();
        defenseManager.reload();

        // Stop and restart armor effect task
        if (armorEffectTask != null) {
            armorEffectTask.cancel();
        }
        startArmorEffectTask();

        getLogger().info("GenPvP reloaded successfully!");
    }

    // Getters
    public static GenPvP getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public GeneratorManager getGeneratorManager() {
        return generatorManager;
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    public CubeManager getCubeManager() {
        return cubeManager;
    }

    public StatisticsManager getStatisticsManager() {
        return statisticsManager;
    }

    public ArmorManager getArmorManager() {
        return armorManager;
    }

    public WorldGuardManager getWorldGuardManager() {
        return worldGuardManager;
    }

    public DefenseManager getDefenseManager() {
        return defenseManager;
    }

    public DefenseDataManager getDefenseDataManager() {
        return defenseDataManager;
    }

    public RaidManager getRaidManager() {
        return raidManager;
    }

    public Economy getEconomy() {
        return economy;
    }

    public boolean isPlaceholderAPIEnabled() {
        return placeholderAPIEnabled;
    }
}