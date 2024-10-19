package com.example.sda;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;

public class GriefingItemListener implements Listener {

    private final SDAPlugin plugin;
    private final List<Material> griefingMaterials = new ArrayList<>();
    private final Map<Material, Integer> itemDangerLevels = new HashMap<>();
    private final Map<String, Long> lastNotificationTime = new HashMap<>();
    public GriefingItemListener(SDAPlugin plugin) {
        this.plugin = plugin;
        loadGriefingItems();
    }

    // プラグインがアクティブかどうかを確認
    private boolean isPluginActive(Player player) {
        return plugin.isPluginEnabled() && !player.hasPermission("sda.bypass");
    }

    // 検知アイテムの読み込み
    private void loadGriefingItems() {
        FileConfiguration config = plugin.getConfig();
        List<Map<?, ?>> items = new ArrayList<>(config.getMapList("detectable-items"));

        // メインスレッドでコレクション操作を行う
        Bukkit.getScheduler().runTask(plugin, () -> {
            items.forEach(itemData -> {
                String itemName = (String) itemData.get("item");
                Integer dangerLevel = (Integer) itemData.get("danger-level");
                try {
                    Material material = Material.valueOf(itemName.toUpperCase());
                    griefingMaterials.add(material);
                    itemDangerLevels.put(material, dangerLevel != null ? dangerLevel : 1);
                } catch (IllegalArgumentException e) {
                    Bukkit.getLogger().warning(plugin.getMessage(String.format("invalid-item-name", itemName)));
                }
            });
        });
    }

    // ベッド爆破の検知
    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        Player player = event.getPlayer();
        if (!isPluginActive(player) || !plugin.isDetectBedUse()) return;
        
        World.Environment env = event.getBed().getWorld().getEnvironment();
        if (env == World.Environment.NETHER || env == World.Environment.THE_END) {
            Material bedMaterial = event.getBed().getType();
            if (isBedMaterial(bedMaterial)) {
                sendDiscordNotification(player, bedMaterial, plugin.getMessage("bed-explosion"), 3);
                plugin.logDebug(String.format(plugin.getMessage("bed-explosion-debug"), player.getName(), bedMaterial.name()));
            }
        }
    }   

    // ベッドの種類を確認
    private boolean isBedMaterial(Material material) {
        return material.name().endsWith("_BED");
    }

    // アイテム所持(クリック)検知
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (!isPluginActive(player)) return;
        
        ItemStack item = event.getCurrentItem();
        if (item != null) {
            Integer dangerLevel = itemDangerLevels.get(item.getType());
            if (dangerLevel != null) {
                sendDiscordNotification(player, item.getType(), plugin.getMessage("item-possession"), dangerLevel);
                if (plugin.isDebugMode()) {
                    plugin.logDebug(String.format(plugin.getMessage("item-possession-debug"), player.getName(), item.getType().name()));
                }
            }
        }
    }

    // アイテム設置検知
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!isPluginActive(player)) return;
        
        Material blockType = event.getBlock().getType();
        Integer dangerLevel = itemDangerLevels.get(blockType);
        if (dangerLevel != null) {
            sendDiscordNotification(player, blockType, plugin.getMessage("block-placement"), dangerLevel);
            if (plugin.isDebugMode()) {
                plugin.logDebug(String.format(plugin.getMessage("block-placement-debug"), player.getName(), blockType.name()));
            }
        }
    }

    // ブロックの着火を検知
    @EventHandler
    public void onPlayerUseFlintAndSteel(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isPluginActive(player) || !plugin.isblockignite()) return;
        
        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.FLINT_AND_STEEL) {
            Block block = event.getClickedBlock();
            if (block != null) {
                Material blockType = block.getType();
                Integer dangerLevel = itemDangerLevels.getOrDefault(blockType, 0);
                sendDiscordNotification(player, blockType, plugin.getMessage("block-ignition"), dangerLevel);
                if (plugin.isDebugMode()) {
                    plugin.logDebug(String.format(plugin.getMessage("block-ignition-debug"), player.getName(), blockType.name()));
                }
            }
        }
    }

    // 溶岩バケツの使用を検知
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isPluginActive(player) || !plugin.islavabucketuse()) return;
        
        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.LAVA_BUCKET) {
            Block block = event.getClickedBlock();
            if (block != null) {
                Material blockType = block.getType();
                Integer dangerLevel = itemDangerLevels.getOrDefault(blockType, 3);
                sendDiscordNotification(player, blockType, plugin.getMessage("lava-bucket-use"), dangerLevel);
                if (plugin.isDebugMode()) {
                    plugin.logDebug(String.format(plugin.getMessage("lava-bucket-use-debug"), player.getName(), blockType.name()));
                }
            }
        }
    }

    // コマンド実行イベントの検知
    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (!plugin.isEnableNotifyCommands()) return; // enable-notify-commandsが有効かどうかを確認

        Player player = event.getPlayer();
        String command = event.getMessage().split(" ")[0].substring(1); // コマンド名を取得

        if (plugin.getNotifyCommands().contains(command) && 
            !player.hasPermission("sda.use") && 
            !player.hasPermission("sda.bypass")) { //sdaのデフォルトはopだから大丈夫だしより確実
            sendDiscordNotification(player, null, plugin.getMessage("command-execution") + command, 5);
            if (plugin.isDebugMode()) {
            plugin.logDebug(String.format(plugin.getMessage("command-execution-debug"), player.getName(), command));
            }
        }
    }

    // Discord通知送信
    public void sendDiscordNotification(Player player, Material item, String action, int dangerLevel) {
        String discordChannelId = plugin.getDiscordChannelId();
        if (discordChannelId == null) {
            Bukkit.getLogger().warning(plugin.getMessage("discord-warning"));
            return;
        }

        String key = player.getUniqueId() + ":" + (item != null ? item.name() : "null") + ":" + action;
        long currentTime = System.currentTimeMillis();
        long cooldownMillis = plugin.getNotificationCooldown() * 1000L;

        if (lastNotificationTime.containsKey(key) && 
            currentTime - lastNotificationTime.get(key) < cooldownMillis) {
            return; // クールダウン中
        }

        lastNotificationTime.put(key, currentTime);

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("SDA Plugin: " + action)
            .setDescription(plugin.getMessage("dangerous-action"))
            .setColor(getEmbedColor(dangerLevel))
            .addField(plugin.getMessage("player-name"), player.getName(), false)
            .addField(plugin.getMessage("uuid"), player.getUniqueId().toString(), false)
            .addField(plugin.getMessage("coordinates"), String.format("X: %d Y: %d Z: %d", player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ()), false)
            .addField(plugin.getMessage("dimension"), getDimension(player.getWorld().getEnvironment()), false)
            .addField(plugin.getMessage("item"), item != null ? item.name() : "N/A", false)
            .setFooter("SDA Plugin", null);

        DiscordSRV discordSRV = (DiscordSRV) Bukkit.getPluginManager().getPlugin("DiscordSRV");
        if (discordSRV != null) {
            discordSRV.getMainGuild().getTextChannelById(discordChannelId)
                .sendMessageEmbeds(embed.build()).queue();
        } else {
            Bukkit.getLogger().warning(plugin.getMessage("discord-srv-not-found"));
        }
    }

    // 危険度に応じた色を返す
    private java.awt.Color getEmbedColor(int dangerLevel) {
        switch (dangerLevel) {
            case 5: return java.awt.Color.MAGENTA;
            case 4: return java.awt.Color.decode("#800000");
            case 3: return java.awt.Color.RED;
            case 2: return java.awt.Color.YELLOW;
            default: return java.awt.Color.WHITE;
        }
    }

    // ディメンション名を返す
    private String getDimension(World.Environment environment) {
        switch (environment) {
            case NORMAL: return plugin.getMessage("overworld");
            case NETHER: return plugin.getMessage("nether");
            case THE_END: return plugin.getMessage("end");
            default: return plugin.getMessage("unknown");
        }
    }
}
