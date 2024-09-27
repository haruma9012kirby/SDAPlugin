package com.example.sda;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.configuration.ConfigurationSection;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class GriefingItemListener implements Listener {

    private final SDAPlugin plugin;
    private final List<Material> griefingMaterials = new ArrayList<>();
    private String discordChannelId = null; // 初期化、もしくはデフォルト値を設定
    private final Map<Material, Integer> itemDangerLevels = new HashMap<>();

    public GriefingItemListener(SDAPlugin plugin) {
        this.plugin = plugin;
        loadGriefingItems();
    }
    // バイパス、プラグイン無効時の共通メソッド
    private boolean isPluginActive(Player player) {
        return plugin.isPluginEnabled() && !player.hasPermission("sda.bypass");
    }

    private void loadGriefingItems() {
        FileConfiguration config = plugin.getConfig();
        List<Map<?, ?>> items = config.getMapList("detectable-items");

        for (Map<?, ?> itemData : items) {
            String itemName = (String) itemData.get("item");
            Integer dangerLevel = (Integer) itemData.get("danger-level");

            try {
                Material material = Material.valueOf(itemName.toUpperCase());
                griefingMaterials.add(material);
                if (dangerLevel == null) {
                }

            // 危険度を保持するマップに追加
                itemDangerLevels.put(material, dangerLevel);
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning(itemName + " は有効なアイテム名ではありません。");
            }
        }
    }
    //ベッド爆破の検知
    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
    Player player = event.getPlayer();
    //共通,ベッド検知
    if (!isPluginActive(player) || !plugin.isDetectBedUse()) {
        return;
    }
        // ネザーやエンドでベッドを使用した場合
        if (event.getBed().getWorld().getEnvironment() == org.bukkit.World.Environment.NETHER ||
            event.getBed().getWorld().getEnvironment() == org.bukkit.World.Environment.THE_END) {
            Material bedMaterial = event.getBed().getType(); // ベッドの種類を取得
            if (isBedMaterial(bedMaterial)) {
            sendDiscordNotification(player, bedMaterial, "ベッド爆破", 3);
        }
    }
}   
    //ベッドの種類
    private boolean isBedMaterial(Material material) {
        return material == Material.WHITE_BED || 
        material == Material.ORANGE_BED || 
        material == Material.MAGENTA_BED || 
        material == Material.LIGHT_BLUE_BED || 
        material == Material.YELLOW_BED || 
        material == Material.LIME_BED || 
        material == Material.PINK_BED || 
        material == Material.GRAY_BED || 
        material == Material.LIGHT_GRAY_BED || 
        material == Material.CYAN_BED || 
        material == Material.PURPLE_BED || 
        material == Material.BLUE_BED || 
        material == Material.BROWN_BED || 
        material == Material.GREEN_BED || 
        material == Material.RED_BED || 
        material == Material.BLACK_BED;
    }

    // アイテム所持(クリック)検知
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        //パーミッション・プラグインの有効or無効
        if (!isPluginActive(player)) {
            return;
        }
        ItemStack item = event.getCurrentItem();
        if (item != null) {
            Material itemType = item.getType();
            Integer dangerLevel = itemDangerLevels.get(itemType);// 危険度を取得
            if (dangerLevel != null) {
                sendDiscordNotification(player, itemType, "アイテム所持", dangerLevel);
            }
        }
    }

    // アイテム設置検知(ただし直接設置でないと検知不可)
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
    Player player = event.getPlayer();
    Block block = event.getBlock();
    Material blockType = block.getType();
    //パーミッション・プラグインの有効or無効
    if (!isPluginActive(player)) {
        return;
    }
    // 対象のブロックが検知アイテムリストにあるかどうか確認
    Integer dangerLevel = itemDangerLevels.get(blockType); // 危険度を取得
    if (dangerLevel != null) {
        sendDiscordNotification(player, blockType, "ブロック設置", dangerLevel);
    }
}

    // ブロックの着火を検知
    @EventHandler
    public void onPlayerUseFlintAndSteel(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    ItemStack item = event.getItem();
    //パーミッション・プラグインの有効or無効
    if (!isPluginActive(player) || !plugin.isblockignite()) {
        return;
    }
    if (item != null && item.getType() == Material.FLINT_AND_STEEL) {
        Block block = event.getClickedBlock();

        if (block != null) {
            Material blockType = block.getType();
            // 対象のブロックが検知アイテムリストにあるかどうか確認
            Integer dangerLevel = itemDangerLevels.getOrDefault(blockType, 0); // デフォルトを0に設定
            sendDiscordNotification(player, blockType, "ブロック着火", dangerLevel);
        }
    }
}
    //

    //Discord検知用 埋め込み形式でDiscordのBOTから通知が来ます！
    public void sendDiscordNotification(Player player, Material item, String action, int dangerLevel) {
    //チャンネルID取得
    String discordChannelId = plugin.getDiscordChannelId();
    //チャンネルIDが設定されていなかった時の処理
        if (discordChannelId == null) {
    Bukkit.getLogger().warning("Discord チャンネルIDが設定されていません！");
        return;
    }
    EmbedBuilder embed = new EmbedBuilder();
    embed.setTitle("SDA Plugin: " + action)
    .setDescription("プレイヤーが危険な行為を行いました。")
    .setColor(getEmbedColor(dangerLevel))
    .addField("プレイヤー名", player.getName(), false)
    .addField("UUID", player.getUniqueId().toString(), false)
    .addField("座標", "X: " + player.getLocation().getBlockX() + " Y: " + player.getLocation().getBlockY() + " Z: " + player.getLocation().getBlockZ(), false)
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

    // アイテムの危険度に応じた色を返すメソッド
    private Color getEmbedColor(int dangerLevel) {
    switch (dangerLevel) {
        case 5:
            return Color.MAGENTA; // 最高危険度
        case 4:
            return Color.RED; // 高危険度
        case 3:
            return Color.ORANGE; // 中危険度
        case 2:
            return Color.YELLOW; // 低危険度
        default:
            return Color.WHITE; 
        }
    }

    // プレイヤーの居るディメンションを特定してsendDiscordNotificationのディメンションに反映させるやつです！
    private String getDimension(org.bukkit.World.Environment environment) {
        switch (environment) {
        case NORMAL:
            return "オーバーワールド";
        case NETHER:
            return "ネザー";
        case THE_END:
            return "エンド";
        default:
            return "不明";
        }
    }
}