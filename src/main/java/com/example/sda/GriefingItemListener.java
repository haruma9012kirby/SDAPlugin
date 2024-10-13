package com.example.sda;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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
        config.getMapList("detectable-items").forEach(itemData -> {
            String itemName = (String) itemData.get("item");
            Integer dangerLevel = (Integer) itemData.get("danger-level");
            try {
                Material material = Material.valueOf(itemName.toUpperCase());
                griefingMaterials.add(material);
                itemDangerLevels.put(material, dangerLevel != null ? dangerLevel : 1);
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning(itemName + " は有効なアイテム名ではありません。");
            }
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
                sendDiscordNotification(player, bedMaterial, "ベッド爆破", 3);
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
                sendDiscordNotification(player, item.getType(), "アイテム所持", dangerLevel);
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
            sendDiscordNotification(player, blockType, "ブロック設置", dangerLevel);
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
                sendDiscordNotification(player, blockType, "ブロック着火", dangerLevel);
            }
        }
    }

    // 溶岩バケツの使用を検知
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isPluginActive(player) || !plugin.islavabucketuse()) return;
        
        if (event.getItem() != null && event.getItem().getType() == Material.LAVA_BUCKET) {
            Block block = event.getClickedBlock();
            if (block != null) {
                Material blockType = block.getType();
                Integer dangerLevel = itemDangerLevels.getOrDefault(blockType, 3);
                sendDiscordNotification(player, blockType, "溶岩バケツ使用", dangerLevel);
            }
        }
    }

    // コマンド実行イベントの検知
    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().split(" ")[0].substring(1); // コマンド名を取得

        if (plugin.getNotifyCommands().contains(command) && 
            !player.hasPermission("sda.use") && 
            !player.hasPermission("sda.bypass")) {
            sendDiscordNotification(player, null, "コマンド実行: " + command, 5);
        }
    }

    // Discord通知送信
    public void sendDiscordNotification(Player player, Material item, String action, int dangerLevel) {
        String discordChannelId = plugin.getDiscordChannelId();
        if (discordChannelId == null) {
            Bukkit.getLogger().warning("Discord チャンネルIDが設定されていません！");
            return;
        }

        String key = player.getUniqueId() + ":" + item.name() + ":" + action;
        long currentTime = System.currentTimeMillis();
        long cooldownMillis = plugin.getNotificationCooldown() * 1000L;

        if (lastNotificationTime.containsKey(key) && 
            currentTime - lastNotificationTime.get(key) < cooldownMillis) {
            return; // クールダウン中
        }

        lastNotificationTime.put(key, currentTime);

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("SDA Plugin: " + action)
            .setDescription("プレイヤーが危険な行為を行いました。")
            .setColor(getEmbedColor(dangerLevel))
            .addField("プレイヤー名", player.getName(), false)
            .addField("UUID", player.getUniqueId().toString(), false)
            .addField("座標", String.format("X: %d Y: %d Z: %d", player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ()), false)
            .addField("ディメンション", getDimension(player.getWorld().getEnvironment()), false)
            .addField("アイテム", item.name(), false)
            .setFooter("SDA Plugin", null);

        DiscordSRV discordSRV = (DiscordSRV) Bukkit.getPluginManager().getPlugin("DiscordSRV");
        if (discordSRV != null) {
            discordSRV.getMainGuild().getTextChannelById(discordChannelId)
                .sendMessageEmbeds(embed.build()).queue();
        } else {
            Bukkit.getLogger().warning("DiscordSRVプラグインが見つかりません！");
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
            case NORMAL: return "オーバーワールド";
            case NETHER: return "ネザー";
            case THE_END: return "エンド";
            default: return "不明";
        }
    }
}
