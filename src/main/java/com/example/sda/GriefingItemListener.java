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

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GriefingItemListener implements Listener {

    private final SDAPlugin plugin;
    private final List<ItemStack> griefingItems = new ArrayList<>();
    private String discordChannelId = null; // 初期化、もしくはデフォルト値を設定

    public GriefingItemListener(SDAPlugin plugin) {
        this.plugin = plugin;
        loadGriefingItems();
    }

    private void loadGriefingItems() {
        FileConfiguration config = plugin.getConfig();
        List<String> items = config.getStringList("detectable-items");

        for (String itemName : items) {
            try {
                Material material = Material.valueOf(itemName.toUpperCase());
                griefingItems.add(new ItemStack(material));
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning(itemName + " は有効なアイテム名ではありません。");
            }
        }
    }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission("sda.bypass")) {
            return;
        }
        if (!plugin.isPluginEnabled()) return; // プラグインが無効化されている場合
        if (!plugin.isDetectBedUse()) return; // ベッド検知が無効化されている場合

        // ネザーやエンドでベッドを使用した場合
        if (event.getBed().getWorld().getEnvironment() == org.bukkit.World.Environment.NETHER ||
            event.getBed().getWorld().getEnvironment() == org.bukkit.World.Environment.THE_END) {
            Material bedMaterial = event.getBed().getType(); // ベッドの種類を取得

            if (isBedMaterial(bedMaterial)) {
                sendDiscordNotification(player, bedMaterial, "ベッド使用");
            }
        }
    }

    // 色付きベッドかどうかを判定するメソッド
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
    // Config.ymlの"どのアイテムを検知するか"の中にあるアイテムをインベントリ内でクリックした場合に検知します！
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!plugin.isPluginEnabled()) return; // プラグインが無効化されている場合
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            if (player.hasPermission("sda.bypass")) {
                return;
            }

            ItemStack item = event.getCurrentItem();
            if (item != null && griefingItems.stream().anyMatch(i -> i.getType() == item.getType())) {
                sendDiscordNotification(player, item.getType(), "アイテム所持");
            }
        }
    }
    // Config.ymlの"どのアイテムを検知するか"の中にある設置可能なブロックを検知します！
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!plugin.isPluginEnabled()) return; // プラグインが無効化されている場合
        Player player = event.getPlayer();
        Block block = event.getBlock();
        List<String> detectItems = plugin.getDetectItems();
        if (player.hasPermission("sda.bypass")) {
            return;
        }
        
        if (detectItems.contains(block.getType().name())) {
            sendDiscordNotification(player, block.getType(), "ブロック設置");
        }
    }
    // ブロック着火の検知を行うやつです
    @EventHandler
    public void onPlayerUseFlintAndSteel(PlayerInteractEvent event) {
        if (!plugin.isPluginEnabled()) return; // プラグインが無効化されている場合
        if (!plugin.isblockignite()) return; //ブロック着火が無効化されている場合
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.getType() == Material.FLINT_AND_STEEL) {
            Block block = event.getClickedBlock();
        if (block != null) {
            sendDiscordNotification(player, item.getType(), "ブロック着火");
        }
      }
    }
    //Discord検知用　埋め込み形式でDiscordのBOTから通知が来ます！
    public void sendDiscordNotification(Player player, Material item, String action) {
        String discordChannelId = plugin.getDiscordChannelId();
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("SDA Plugin: " + action)
            .setDescription("プレイヤーが危険な行為を行いました。")
            .setColor(Color.RED)
            .addField("プレイヤー名", player.getName(), false)
            .addField("UUID", player.getUniqueId().toString(), false)
            .addField("座標", "X: " + player.getLocation().getBlockX() + " Y: " + player.getLocation().getBlockY() + " Z: " + player.getLocation().getBlockZ(), false)
            .addField("ディメンション", getDimension(player.getWorld().getEnvironment()), false)
            .addField("アイテム", item.name(), false)
            .setFooter("SDA Plugin", null);

        Plugin plugin = Bukkit.getPluginManager().getPlugin("DiscordSRV");
        if (plugin != null && plugin instanceof DiscordSRV) {
            DiscordSRV discordSRV = (DiscordSRV) plugin;
            discordSRV.getMainGuild().getTextChannelById(discordChannelId)
                    .sendMessageEmbeds(embed.build()).queue();
        }
    }
    //　プレイヤーの居るディメンションを特定してsendDiscordNotificationのディメンションに反映させるやつです！
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
