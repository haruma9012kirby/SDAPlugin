package com.example.sda;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class SDAPlugin extends JavaPlugin {

    private boolean pluginEnabled;
    private List<String> detectItems;
    private boolean detectBedUse;
    private String discordChannelId;
    private boolean blockignite;
    public GriefingItemListener griefingItemListener;

    @Override
    public void onEnable() {
        saveDefaultConfig(); // config.ymlを作成
        loadConfigValues();
        getLogger().info("SDA Plugin Enabled");
        Bukkit.getPluginManager().registerEvents(new GriefingItemListener(this), this);

    }

    @Override
    public void onDisable() {
        getLogger().info("SDA Plugin Disabled");
    }

    // config.ymlの値を読み込むメソッド
    public void loadConfigValues() {
        FileConfiguration config = getConfig();
        pluginEnabled = config.getBoolean("plugin-enabled", true);
        detectItems = config.getStringList("detectable-items");
        detectBedUse = config.getBoolean("detect-bed-use", true);
        discordChannelId = getConfig().getString("discord-channel-id", "123456789012345678"); // デフォルトのチャンネルID
        blockignite = config.getBoolean("detect-ignite-block", true);
    }

    public boolean isPluginEnabled() {
        return pluginEnabled;
    }

    public List<String> getDetectItems() {
        return detectItems;
    }

    public boolean isDetectBedUse() {
        return detectBedUse;
    }

    public String getDiscordChannelId() {
        return discordChannelId != null ? discordChannelId : "123456789012345678"; // nullチェックでデフォルト値を返す
    }

    public boolean isblockignite() {
        return blockignite;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("sda") && sender.hasPermission("sda.use")) {
        if (args.length == 1) {
            switch (args[0].toLowerCase()) {
                case "on":
                    pluginEnabled = true;
                    getConfig().set("plugin-enabled", true);
                    saveConfig();
                    sender.sendMessage("§a[SDA]SDAPluginが有効化されました");
                    return true;
                case "off":
                    pluginEnabled = false;
                    getConfig().set("plugin-enabled", false);
                    saveConfig();
                    sender.sendMessage("§c[SDA]SDAPluginが無効化されました");
                    return true;
                case "reload":
                    reloadConfig();
                    loadConfigValues();
                    sender.sendMessage("§a[SDA]configファイルがリロードされました!");
                    if (getDiscordChannelId().equals("YOUR-DISCORD-CHANNEL-ID")) {
                        sender.sendMessage("§c[SDA] 警告: config.ymlでDiscordチャンネルIDが設定されていません！");
                        getLogger().warning("[SDA]DiscordチャンネルIDが設定されていません！");
                    }
                    return true;
                }
            }
        }
    sender.sendMessage("§c無効なコマンドです。");
    return false;
    }
}