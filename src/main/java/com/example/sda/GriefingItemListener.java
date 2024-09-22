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
    private final List<ItemStack> griefingItems = new ArrayList<>();
    private String discordChannelId = null; // 初期化、もしくはデフォルト値を設定
    private final Map<Material, Integer> itemDangerLevels = new HashMap<>();

    public GriefingItemListener(SDAPlugin plugin) {
        this.plugin = plugin;
        loadGriefingItems();
    }

    public void loadGriefingItems() {
        FileConfiguration config = plugin.getConfig();
        List<Map<?, ?>> items = config.getMapList("detectable-items");

        for (Map<?, ?> itemData : items) {
            String itemName = (String) itemData.get("item");
            Integer dangerLevel = (Integer) itemData.get("danger-level");

            try {
                Material material = Material.valueOf(itemName.toUpperCase());
                griefingItems.add(new ItemStack(material));
                if (dangerLevel == null) {
                }

            // 危険度を保持するマップに追加
                itemDangerLevels.put(material, dangerLevel);
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
                int dangerLevel = 3;
                sendDiscordNotification(player, bedMaterial, "ベッド使用", dangerLevel);
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
            if (item != null) {
                Material itemType = item.getType();

            // 対象のアイテムが検知アイテムリストにあるかどうか確認
                if (itemDangerLevels.containsKey(itemType)) {
                int dangerLevel = itemDangerLevels.get(itemType); // 危険度を取得
                sendDiscordNotification(player, itemType, "アイテム所持", dangerLevel);
            }
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
        Material blockType = block.getType();

        // 対象のブロックが検知アイテムリストにあるかどうか確認
        if (itemDangerLevels.containsKey(blockType)) {
            int dangerLevel = itemDangerLevels.get(blockType); // 危険度を取得
            sendDiscordNotification(player, blockType, "ブロック設置", dangerLevel);
        }
    }
    // ブロックの着火を検知します！
    @EventHandler
    public void onPlayerUseFlintAndSteel(PlayerInteractEvent event) {
        if (!plugin.isPluginEnabled()) return; // プラグインが無効化されている場合
        if (!plugin.isblockignite()) return; //ブロック着火が無効化されている場合
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (player.hasPermission("sda.bypass")) {
            return;
        }
        if (item != null && item.getType() == Material.FLINT_AND_STEEL) {
            Block block = event.getClickedBlock();

            if (block != null) {
                Material blockType = block.getType();
                int dangerLevel = itemDangerLevels.getOrDefault(blockType, 1); // デフォルト値を1に設定

            // 対象のブロックが検知アイテムリストにあるかどうか確認
                if (itemDangerLevels.containsKey(blockType)) {
                    sendDiscordNotification(player, blockType, "ブロック着火", dangerLevel);
                }
            }
        }
    }
    //Discord検知用　埋め込み形式でDiscordのBOTから通知が来ます！
    public void sendDiscordNotification(Player player, Material item, String action, int dangerLevel) {
        String discordChannelId = plugin.getDiscordChannelId();
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

        Plugin plugin = Bukkit.getPluginManager().getPlugin("DiscordSRV");
        if (plugin != null && plugin instanceof DiscordSRV) {
            DiscordSRV discordSRV = (DiscordSRV) plugin;
            discordSRV.getMainGuild().getTextChannelById(discordChannelId)
            .sendMessageEmbeds(embed.build()).queue();
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