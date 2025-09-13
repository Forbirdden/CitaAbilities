package ru.forbirdden.citaabilities;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CitaAbilities extends JavaPlugin implements Listener {

    private final Map<String, Integer> citaValues = new HashMap<>();
    private final Map<UUID, Integer> lastCitaValues = new HashMap<>();
    private final Map<UUID, BukkitRunnable> playerTasks = new HashMap<>();
    private final Map<UUID, Inventory> openMenus = new HashMap<>();
    private static final int MENU_SIZE = 45;
    private static final int CENTER_SLOT = 22;
    private static final int DEFAULT_CITA = 20;
    private static final int MIN_CITA = 0;
    private static final int MAX_CITA = 20;
    
    private static final int BOOTS_SLOT = 20;
    private static final int SWORD_SLOT = 24;
    private static final int NETHERRACK_SLOT = 30;
    private static final int GRASS_SLOT = 40;
    private static final int ENDSTONE_SLOT = 32;
    private static final int APPLE_SLOT = 12;
    
    private File dataFile;
    private BukkitRunnable restorationTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Не удалось создать файл данных: " + e.getMessage());
            }
        }
        
        getLogger().info("Плагин CitaAbilities запущен!");
        
        getServer().getPluginManager().registerEvents(this, this);
        loadCitaData();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            startIndividualActionBarTask(player);
        }
        
        getLogger().info("ActionBar задачи запущены для " + Bukkit.getOnlinePlayers().size() + " игроков!");
        
        startRestorationTask();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                saveCitaData();
                if (getConfig().getBoolean("debug", false)) {
                    getLogger().info("Автосохранение данных выполнено");
                }
            }
        }.runTaskTimer(this, 6000L, 6000L);
    }

    private void startRestorationTask() {
        restorationTask = new BukkitRunnable() {
            @Override
            public void run() {
                int restoredCount = 0;
                
                for (Map.Entry<String, Integer> entry : citaValues.entrySet()) {
                    String playerName = entry.getKey();
                    int currentCita = entry.getValue();
                    
                    if (currentCita < MAX_CITA) {
                        int newValue = Math.min(MAX_CITA, currentCita + 1);
                        citaValues.put(playerName, newValue);
                        restoredCount++;
                        
                        if (getConfig().getBoolean("debug", false)) {
                            getLogger().info("Восстановлена 1 цита игроку " + playerName + ": " + newValue + "/20");
                        }
                    }
                }
                
                saveCitaData();
                
                if (getConfig().getBoolean("debug", false)) {
                    getLogger().info("Автоматическое восстановление циты выполнено. Восстановлено: " + restoredCount + " игрокам");
                }
            }
        };
        
        long delay = 30L * 60L * 20L;
        restorationTask.runTaskTimer(this, delay, delay);
        
        getLogger().info("Задача автоматического восстановления циты запущена (каждые 30 минут)");
    }

    public void openCitaMenu(Player player) {
        Inventory menu = Bukkit.createInventory(player, MENU_SIZE, Component.text("CitaAbilities"));
        
        ItemStack lantern = new ItemStack(Material.LANTERN);
        ItemMeta lanternMeta = lantern.getItemMeta();
        lanternMeta.displayName(Component.text("§6§lДа будет свет"));
        lanternMeta.lore(Arrays.asList(
            Component.text("§7Подсвети всех существ вокруг"),
            Component.text(" "),
            Component.text("§eСтоимость: §f4 ✦")
        ));
        lantern.setItemMeta(lanternMeta);
        menu.setItem(CENTER_SLOT, lantern);
        
        ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);
        ItemMeta bootsMeta = boots.getItemMeta();
        bootsMeta.displayName(Component.text("§b§lУскорение"));
        bootsMeta.lore(Arrays.asList(
            Component.text("§7Ноги сильнее ветра"),
            Component.text(" "),
            Component.text("§eСтоимость: §f2 ✦")
        ));
        boots.setItemMeta(bootsMeta);
        menu.setItem(BOOTS_SLOT, boots);
        
        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        swordMeta.displayName(Component.text("§9§lТвёрдые руки"));
        swordMeta.lore(Arrays.asList(
            Component.text("§7Очень сильные удары"),
            Component.text(" "),
            Component.text("§eСтоимость: §f3 ✦")
        ));
        sword.setItemMeta(swordMeta);
        menu.setItem(SWORD_SLOT, sword);
        
        ItemStack netherrack = new ItemStack(Material.NETHERRACK);
        ItemMeta netherrackMeta = netherrack.getItemMeta();
        netherrackMeta.displayName(Component.text("§4§lПламенный отель"));
        netherrackMeta.lore(Arrays.asList(
            Component.text("§7Телепортация в Незер"),
            Component.text(" "),
            Component.text("§eСтоимость: §f6 ✦")
        ));
        netherrack.setItemMeta(netherrackMeta);
        menu.setItem(NETHERRACK_SLOT, netherrack);
        
        ItemStack grass = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta grassMeta = grass.getItemMeta();
        grassMeta.displayName(Component.text("§a§lВозвращение домой"));
        grassMeta.lore(Arrays.asList(
            Component.text("§7Телепортация в Люменару"),
            Component.text(" "),
            Component.text("§eСтоимость: §f6 ✦")
        ));
        grass.setItemMeta(grassMeta);
        menu.setItem(GRASS_SLOT, grass);
        
        ItemStack endstone = new ItemStack(Material.END_STONE);
        ItemMeta endstoneMeta = endstone.getItemMeta();
        endstoneMeta.displayName(Component.text("§d§lВверх"));
        endstoneMeta.lore(Arrays.asList(
            Component.text("§7Телепортация в Энд"),
            Component.text(" "),
            Component.text("§eСтоимость: §f6 ✦")
        ));
        endstone.setItemMeta(endstoneMeta);
        menu.setItem(ENDSTONE_SLOT, endstone);

        ItemStack apple = new ItemStack(Material.APPLE);
        ItemMeta appleMeta = apple.getItemMeta();
        appleMeta.displayName(Component.text("§c§lВосстановление"));
        appleMeta.lore(Arrays.asList(
            Component.text("§7Моментальное излечение"),
            Component.text(" "),
            Component.text("§eСтоимость: §f5 ✦")
        ));
        apple.setItemMeta(appleMeta);
        menu.setItem(APPLE_SLOT, apple);
        
        player.openInventory(menu);
        openMenus.put(player.getUniqueId(), menu);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        
        if (clickedInventory != null && openMenus.containsKey(player.getUniqueId()) && 
            clickedInventory.equals(openMenus.get(player.getUniqueId()))) {
            
            event.setCancelled(true);
            
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
            
            if (event.getSlot() == CENTER_SLOT && clickedItem.getType() == Material.LANTERN) {
                handleLanternClick(player);
            }
            else if (event.getSlot() == BOOTS_SLOT && clickedItem.getType() == Material.DIAMOND_BOOTS) {
                handleBootsClick(player);
            }
            else if (event.getSlot() == SWORD_SLOT && clickedItem.getType() == Material.IRON_SWORD) {
                handleSwordClick(player);
            }
            else if (event.getSlot() == NETHERRACK_SLOT && clickedItem.getType() == Material.NETHERRACK) {
                handleNetherrackClick(player);
            }
            else if (event.getSlot() == GRASS_SLOT && clickedItem.getType() == Material.GRASS_BLOCK) {
                handleGrassClick(player);
            }
            else if (event.getSlot() == ENDSTONE_SLOT && clickedItem.getType() == Material.END_STONE) {
                handleEndstoneClick(player);
            }
            else if (event.getSlot() == APPLE_SLOT && clickedItem.getType() == Material.APPLE) {
                handleAppleClick(player);
            }
        }
    }

    private void handleLanternClick(Player player) {
        int currentCita = getCitaValue(player);
        
        if (currentCita < 4) {
            player.sendMessage(Component.text("§cНедостаточно циты! Нужно 4, у вас: " + currentCita));
            player.closeInventory();
            return;
        }
        
        setCitaValue(player, currentCita - 4);
        
        String glowingCommand = "execute as " + player.getName() + " at " + player.getName() + 
                              " run effect give @e[distance=..50] minecraft:glowing 15 255 true";
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), glowingCommand);

        String particleCommand = "execute as " + player.getName() + " at " + player.getName() + 
                               " run particle minecraft:end_rod ~ ~1 ~ 1 1 1 1 100 force";
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), particleCommand);
        
        player.closeInventory();
    }

    private void handleAppleClick(Player player) {
        int currentCita = getCitaValue(player);
        
        if (currentCita < 5) {
            player.sendMessage(Component.text("§cНедостаточно циты! Нужно 5, у вас: " + currentCita));
            player.closeInventory();
            return;
        }
        
        setCitaValue(player, currentCita - 5);
        
        String regenCommand = "execute as " + player.getName() + " at " + player.getName() + 
                              " run effect give @s minecraft:regeneration 5 5 true";
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), regenCommand);

        String saturCommand = "execute as " + player.getName() + " at " + player.getName() + 
                              " run effect give @s minecraft:saturation 5 5 true";
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), saturCommand);

        String particleCommand = "execute as " + player.getName() + " at " + player.getName() + 
                               " run particle minecraft:end_rod ~ ~1 ~ 1 1 1 1 100 force";
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), particleCommand);
        
        player.closeInventory();
    }

    private void handleBootsClick(Player player) {
        int currentCita = getCitaValue(player);
        
        if (currentCita < 2) {
            player.sendMessage(Component.text("§cНедостаточно циты! Нужно 2, у вас: " + currentCita));
            player.closeInventory();
            return;
        }
        
        setCitaValue(player, currentCita - 2);
        
        String speedCommand = "execute as " + player.getName() + " at " + player.getName() + 
                             " run effect give @s minecraft:speed 10 1 true";
        String jumpCommand = "execute as " + player.getName() + " at " + player.getName() + 
                            " run effect give @s minecraft:jump_boost 10 1 true";
        
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), speedCommand);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), jumpCommand);
        
        String particleCommand = "execute as " + player.getName() + " at " + player.getName() + 
                               " run particle minecraft:end_rod ~ ~1 ~ 1 1 1 1 100 force";
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), particleCommand);
        
        startElectricParticles(player);
        player.closeInventory();
    }

    private void handleSwordClick(Player player) {
        int currentCita = getCitaValue(player);
        
        if (currentCita < 3) {
            player.sendMessage(Component.text("§cНедостаточно циты! Нужно 3, у вас: " + currentCita));
            player.closeInventory();
            return;
        }
        
        setCitaValue(player, currentCita - 3);
        
        String strengthCommand = "execute as " + player.getName() + " at " + player.getName() + 
                               " run effect give @s minecraft:strength 10 1 true";
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), strengthCommand);
        
        String particleCommand = "execute as " + player.getName() + " at " + player.getName() + 
                               " run particle minecraft:end_rod ~ ~1 ~ 1 1 1 1 100 force";
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), particleCommand);
        
        player.closeInventory();
    }

    private void handleNetherrackClick(Player player) {
        int currentCita = getCitaValue(player);
        
        if (currentCita < 6) {
            player.sendMessage(Component.text("§cНедостаточно циты! Нужно 6, у вас: " + currentCita));
            player.closeInventory();
            return;
        }
        
        setCitaValue(player, currentCita - 6);
        
        String teleportCommand = "mv tp " + player.getName() + " world_nether";
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), teleportCommand);
        
        String particleCommand = "execute as " + player.getName() + " at " + player.getName() + 
                               " run particle minecraft:end_rod ~ ~1 ~ 1 1 1 1 100 force";
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), particleCommand);
        
        player.closeInventory();
    }

    private void handleGrassClick(Player player) {
        int currentCita = getCitaValue(player);
        
        if (currentCita < 6) {
            player.sendMessage(Component.text("§cНедостаточно циты! Нужно 6, у вас: " + currentCita));
            player.closeInventory();
            return;
        }
        
        setCitaValue(player, currentCita - 6);
        
        String teleportCommand = "mv tp " + player.getName() + " world";
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), teleportCommand);
        
        String particleCommand = "execute as " + player.getName() + " at " + player.getName() + 
                               " run particle minecraft:end_rod ~ ~1 ~ 1 1 1 1 100 force";
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), particleCommand);
        
        player.closeInventory();
    }

    private void handleEndstoneClick(Player player) {
        int currentCita = getCitaValue(player);
        
        if (currentCita < 6) {
            player.sendMessage(Component.text("§cНедостаточно циты! Нужно 6, у вас: " + currentCita));
            player.closeInventory();
            return;
        }
        
        setCitaValue(player, currentCita - 6);
        
        String teleportCommand = "mv tp " + player.getName() + " world_the_end";
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), teleportCommand);
        
        String particleCommand = "execute as " + player.getName() + " at " + player.getName() + 
                               " run particle minecraft:end_rod ~ ~1 ~ 1 1 1 1 100 force";
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), particleCommand);
        
        player.closeInventory();
    }

    private void startElectricParticles(Player player) {
        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 10 * 20;
            
            @Override
            public void run() {
                if (ticks >= duration || !player.isOnline()) {
                    this.cancel();
                    return;
                }
                
                String particleCommand = "execute as " + player.getName() + " at " + player.getName() + 
                                       " run particle minecraft:electric_spark ~ ~1 ~ 0.25 0.25 0.25 1 1 force";
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), particleCommand);
                
                ticks += 5;
            }
        }.runTaskTimer(this, 0L, 5L);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            openMenus.remove(player.getUniqueId());
        }
    }

    private void startIndividualActionBarTask(Player player) {
        UUID uuid = player.getUniqueId();
        String playerName = player.getName().toLowerCase();
        
        stopIndividualActionBarTask(player);
        
        if (!citaValues.containsKey(playerName)) {
            citaValues.put(playerName, DEFAULT_CITA);
        }
        
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    playerTasks.remove(uuid);
                    return;
                }
                
                try {
                    int currentCita = getCitaValue(player);
                    
                    Component message = Component.text()
                        .content("✦ " + currentCita + "/20 циты ✦")
                        .color(NamedTextColor.WHITE)
                        .build();
                    
                    player.sendActionBar(message);
                    
                    if (getConfig().getBoolean("debug", false)) {
                        getLogger().info("Отправлен ActionBar игроку " + player.getName() + ": " + currentCita);
                    }
                } catch (Exception e) {
                    getLogger().warning("Ошибка при отправке ActionBar игроку " + player.getName() + ": " + e.getMessage());
                }
            }
        };
        
        task.runTaskTimer(this, 0L, getConfig().getInt("update_interval", 40));
        playerTasks.put(uuid, task);
    }
    
    private void stopIndividualActionBarTask(Player player) {
        BukkitRunnable existingTask = playerTasks.remove(player.getUniqueId());
        if (existingTask != null && !existingTask.isCancelled()) {
            existingTask.cancel();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName().toLowerCase();
        
        if (!citaValues.containsKey(playerName)) {
            citaValues.put(playerName, DEFAULT_CITA);
        }
        
        startIndividualActionBarTask(player);
        
        if (getConfig().getBoolean("debug", false)) {
            getLogger().info("Запущена ActionBar задача для " + player.getName());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        stopIndividualActionBarTask(player);
        lastCitaValues.remove(uuid);
        
        if (getConfig().getBoolean("debug", false)) {
            getLogger().info("Остановлена ActionBar задача для " + player.getName());
        }
    }

    @Override
    public void onDisable() {
        if (restorationTask != null && !restorationTask.isCancelled()) {
            restorationTask.cancel();
        }
        
        for (BukkitRunnable task : playerTasks.values()) {
            if (!task.isCancelled()) {
                task.cancel();
            }
        }
        playerTasks.clear();
        lastCitaValues.clear();
        
        getLogger().info("Плагин CitaAbilities остановлен! Все задачи отменены.");
        saveCitaData();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                           @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("cita")) {
            return handleCitaCommand(sender, args);
        }
        return false;
    }

    private boolean handleCitaCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§6§lЦита Система");
            sender.sendMessage("§e/cita set [игрок] <значение> §7- Установить значение циты");
            sender.sendMessage("§e/cita add [игрок] <значение> §7- Добавить значение циты");
            sender.sendMessage("§e/cita remove [игрок] <значение> §7- Удалить значение циты");
            sender.sendMessage("§e/cita get [игрок] §7- Проверить значение циты");
            sender.sendMessage("§e/cita debug §7- Режим отладки");
            sender.sendMessage("§e/cita help §7- Показать справку");
            sender.sendMessage("§e/cita reload §7- Перезагрузить конфиг");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "menu":
                return handleMenuCommand(sender);
            case "set":
                return handleSetCommand(sender, args);
            case "add":
                return handleAddCommand(sender, args);
            case "remove":
                return handleRemoveCommand(sender, args);
            case "get":
                return handleCheckCommand(sender, args);
            case "debug":
                return handleDebugCommand(sender);
            case "help":
                return handleHelpCommand(sender);
            case "reload":
                return handleReloadCommand(sender);
            default:
                sender.sendMessage("§cНеизвестная подкоманда! Используйте §f/cita help");
                return true;
        }
    }

    private boolean handleMenuCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cЭта команда только для игроков!");
            return true;
        }
        
        Player player = (Player) sender;
        openCitaMenu(player);
        return true;
    }

    private boolean handleSetCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("citaabilities.set")) {
            sender.sendMessage("§cУ вас нет прав на использование этой команды!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cИспользование: §f/cita set [игрок] <значение>");
            return true;
        }

        Player targetPlayer;
        int valueIndex;

        if (args.length == 2) {
            if (sender instanceof Player) {
                targetPlayer = (Player) sender;
                valueIndex = 1;
            } else {
                sender.sendMessage("§cКонсоль должна указать игрока: §f/cita set <игрок> <значение>");
                return true;
            }
        } else {
            targetPlayer = Bukkit.getPlayer(args[1]);
            valueIndex = 2;
        }

        if (targetPlayer == null) {
            sender.sendMessage("§cИгрок не найден или не в сети!");
            return true;
        }

        try {
            int newValue = Integer.parseInt(args[valueIndex]);
            if (newValue < MIN_CITA || newValue > MAX_CITA) {
                sender.sendMessage("§cЗначение должно быть от " + MIN_CITA + " до " + MAX_CITA + "!");
                return true;
            }

            setCitaValue(targetPlayer, newValue);
            sender.sendMessage("§aЗначение циты для §f" + targetPlayer.getName() + 
                             " §aустановлено на: §e" + newValue + "/20");

            if (!sender.equals(targetPlayer)) {
                targetPlayer.sendMessage("§aВаше значение циты установлено на: §e" + newValue + "/20");
            }

        } catch (NumberFormatException e) {
            sender.sendMessage("§cНеверное число! Используйте целое число от " + MIN_CITA + " до " + MAX_CITA);
        }
        return true;
    }

    private boolean handleAddCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("citaabilities.set")) {
            sender.sendMessage("§cУ вас нет прав на использование этой команды!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cИспользование: §f/cita add [игрок] <значение>");
            return true;
        }

        Player targetPlayer;
        int valueIndex;

        if (args.length == 2) {
            if (sender instanceof Player) {
                targetPlayer = (Player) sender;
                valueIndex = 1;
            } else {
                sender.sendMessage("§cКонсоль должна указать игрока: §f/cita add <игрок> <значение>");
                return true;
            }
        } else {
            targetPlayer = Bukkit.getPlayer(args[1]);
            valueIndex = 2;
        }

        if (targetPlayer == null) {
            sender.sendMessage("§cИгрок не найден или не в сети!");
            return true;
        }

        try {
            int addValue = Integer.parseInt(args[valueIndex]);
            if (addValue < 0) {
                sender.sendMessage("§cЗначение должно быть положительным!");
                return true;
            }

            int currentCita = getCitaValue(targetPlayer);
            int newValue = Math.min(MAX_CITA, currentCita + addValue);
            
            setCitaValue(targetPlayer, newValue);
            
            sender.sendMessage("§aДобавлено §e" + addValue + " §aциты игроку §f" + targetPlayer.getName() + 
                             "§a. Новое значение: §e" + newValue + "/20");

            if (!sender.equals(targetPlayer)) {
                targetPlayer.sendMessage("§aВам добавлено §e" + addValue + " §aциты. Новое значение: §e" + newValue + "/20");
            }

        } catch (NumberFormatException e) {
            sender.sendMessage("§cНеверное число! Используйте целое положительное число");
        }
        return true;
    }

    private boolean handleRemoveCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("citaabilities.set")) {
            sender.sendMessage("§cУ вас нет прав на использование этой команды!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cИспользование: §f/cita remove [игрок] <значение>");
            return true;
        }

        Player targetPlayer;
        int valueIndex;

        if (args.length == 2) {
            if (sender instanceof Player) {
                targetPlayer = (Player) sender;
                valueIndex = 1;
            } else {
                sender.sendMessage("§cКонсоль должна указать игрока: §f/cita remove <игрок> <значение>");
                return true;
            }
        } else {
            targetPlayer = Bukkit.getPlayer(args[1]);
            valueIndex = 2;
        }

        if (targetPlayer == null) {
            sender.sendMessage("§cИгрок не найден или не в сети!");
            return true;
        }

        try {
            int removeValue = Integer.parseInt(args[valueIndex]);
            if (removeValue < 0) {
                sender.sendMessage("§cЗначение должно быть положительным!");
                return true;
            }

            int currentCita = getCitaValue(targetPlayer);
            int newValue = Math.max(MIN_CITA, currentCita - removeValue);
            
            setCitaValue(targetPlayer, newValue);
            
            sender.sendMessage("§aУдалено §e" + removeValue + " §aциты у игрока §f" + targetPlayer.getName() + 
                             "§a. Новое значение: §e" + newValue + "/20");

            if (!sender.equals(targetPlayer)) {
                targetPlayer.sendMessage("§aУ вас удалено §e" + removeValue + " §aциты. Новое значение: §e" + newValue + "/20");
            }

        } catch (NumberFormatException e) {
            sender.sendMessage("§cНеверное число! Используйте целое положительное число");
        }
        return true;
    }

    private boolean handleCheckCommand(CommandSender sender, String[] args) {
        Player targetPlayer;
        
        if (args.length == 1) {
            if (sender instanceof Player) {
                targetPlayer = (Player) sender;
            } else {
                sender.sendMessage("§cКонсоль должна указать игрока: §f/cita check <игрок>");
                return true;
            }
        } else {
            targetPlayer = Bukkit.getPlayer(args[1]);
        }

        if (targetPlayer == null) {
            String playerName = args[1].toLowerCase();
            if (citaValues.containsKey(playerName)) {
                int cita = getCitaValue(playerName);
                sender.sendMessage("§aЦита игрока §f" + args[1] + "§a: §e" + cita + "/20 §7(оффлайн)");
            } else {
                sender.sendMessage("§cИгрок не найден!");
            }
            return true;
        }

        int cita = getCitaValue(targetPlayer);
        sender.sendMessage("§aЦита игрока §f" + targetPlayer.getName() + "§a: §e" + cita + "/20");
        return true;
    }

    private boolean handleDebugCommand(CommandSender sender) {
        if (!sender.hasPermission("citaabilities.debug")) {
            sender.sendMessage("§cУ вас нет прав на использование этой команды!");
            return true;
        }

        boolean debugMode = !getConfig().getBoolean("debug", false);
        getConfig().set("debug", debugMode);
        saveConfig();

        sender.sendMessage("§aРежим отладки " + (debugMode ? "§aвключен" : "§cвыключен"));
        sender.sendMessage("§aОнлайн игроков: §e" + Bukkit.getOnlinePlayers().size());
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            int cita = getCitaValue(player);
            sender.sendMessage("§7- §f" + player.getName() + "§7: §e" + cita + "/20");
        }
        
        return true;
    }

    private boolean handleHelpCommand(CommandSender sender) {
        sender.sendMessage("§6§lПомощь по командам Циты");
        sender.sendMessage("§e/cita §7- Основное меню");
        sender.sendMessage("§e/cita set [игрок] <значение> §7- Установить значение (0-20)");
        sender.sendMessage("§e/cita add [игрок] <значение> §7- Добавить значение циты");
        sender.sendMessage("§e/cita remove [игрок] <значение> §7- Удалить значение циты");
        sender.sendMessage("§e/cita get [игрок] §7- Проверить значение");
        sender.sendMessage("§e/cita debug §7- Режим отладки");
        sender.sendMessage("§e/cita reload §7- Перезагрузить конфиг");
        return true;
    }

    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("citaabilities.reload")) {
            sender.sendMessage("§cУ вас нет прав на использование этой команды!");
            return true;
        }

        reloadConfig();
        loadCitaData();
        sender.sendMessage("§aКонфигурация и данные игроков перезагружены!");
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                              @NotNull String alias, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("cita")) {
            if (args.length == 1) {
                return java.util.Arrays.asList("set", "add", "remove", "get", "debug", "help", "reload", "menu");
            } else if ((args.length == 2 && (args[0].equalsIgnoreCase("set") || 
                              args[0].equalsIgnoreCase("add") || 
                              args[0].equalsIgnoreCase("remove") || 
                              args[0].equalsIgnoreCase("get")))) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .toList();
            }
        }
        return java.util.Collections.emptyList();
    }

    public int getCitaValue(Player player) {
        return citaValues.getOrDefault(player.getName().toLowerCase(), DEFAULT_CITA);
    }
    
    public int getCitaValue(String playerName) {
        return citaValues.getOrDefault(playerName.toLowerCase(), DEFAULT_CITA);
    }

    public void setCitaValue(Player player, int value) {
        citaValues.put(player.getName().toLowerCase(), Math.max(MIN_CITA, Math.min(MAX_CITA, value)));
    }
    
    public void setCitaValue(String playerName, int value) {
        citaValues.put(playerName.toLowerCase(), Math.max(MIN_CITA, Math.min(MAX_CITA, value)));
    }

    private void loadCitaData() {
        YamlConfiguration dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        
        if (dataConfig.contains("player_data")) {
            for (String playerName : dataConfig.getConfigurationSection("player_data").getKeys(false)) {
                int value = dataConfig.getInt("player_data." + playerName, DEFAULT_CITA);
                citaValues.put(playerName.toLowerCase(), value);
            }
        }
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            String playerName = player.getName().toLowerCase();
            if (!citaValues.containsKey(playerName)) {
                citaValues.put(playerName, DEFAULT_CITA);
            }
        }
        
        getLogger().info("Загружены данные для " + citaValues.size() + " игроков");
    }

    private void saveCitaData() {
        YamlConfiguration dataConfig = new YamlConfiguration();
        
        for (Map.Entry<String, Integer> entry : citaValues.entrySet()) {
            dataConfig.set("player_data." + entry.getKey(), entry.getValue());
        }
        
        try {
            dataConfig.save(dataFile);
            if (getConfig().getBoolean("debug", false)) {
                getLogger().info("Данные циты сохранены для " + citaValues.size() + " игроков");
            }
        } catch (IOException e) {
            getLogger().severe("Не удалось сохранить данные: " + e.getMessage());
        }
    }
}
