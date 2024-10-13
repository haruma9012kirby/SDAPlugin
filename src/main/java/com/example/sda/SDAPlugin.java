package com.example.sda;

import github.scarsz.discordsrv.DiscordSRV;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class SDAPlugin extends JavaPlugin implements TabCompleter {

    private boolean pluginEnabled;
    private List<String> detectItems;
    private boolean detectBedUse;
    private String discordChannelId;
    private boolean blockignite;
    public GriefingItemListener griefingItemListener;
    private boolean lavaBucketUse;
    private int notificationCooldown;
    private List<String> notifyCommands; // 新しい設定項目

    @Override
    public void onEnable() {
        saveDefaultConfig(); // config.ymlを作成
        loadConfigValues();
        getLogger().info("SDA Plugin Enabled");
        Bukkit.getPluginManager().registerEvents(new GriefingItemListener(this), this);
        
        // コマンドのTabCompleterを登録
        getCommand("sda").setTabCompleter(this);
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
        discordChannelId = getConfig().getString("discord-channel-id", "YOUR-DISCORD-CHANNEL-ID"); // デフォルトのチャンネルID
        blockignite = config.getBoolean("detect-ignite-block", true);
        lavaBucketUse = config.getBoolean("detect-lavabucket-use", true);
        notificationCooldown = config.getInt("notification-cooldown", 1);
        notifyCommands = config.getStringList("notify-commands"); // 新しい設定項目の読み込み
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
        return discordChannelId != null ? discordChannelId : "YOUR-DISCORD-CHANNEL-ID"; // nullチェックでデフォルト値を返す
    }

    public boolean isblockignite() {
        return blockignite;
    }

    public boolean islavabucketuse() {
        return lavaBucketUse;
    }

    public int getNotificationCooldown() {
        return notificationCooldown;
    }

    public List<String> getNotifyCommands() {
        return notifyCommands;
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
                    case "test":
                        if (sender instanceof Player) {
                            Player player = (Player) sender;
                            sendSimpleDiscordMessage("テストメッセージ", player.getName() + " がテストメッセージを送信しました！");
                            sender.sendMessage("§e[SDA] テストメッセージを Discord に送信しました！");
                        } else {
                            sender.sendMessage("§cこのコマンドはプレイヤーのみが実行できます。");
                        }
                        return true;
                }
            }
        }
        // コマンドが不正な場合、ヘルプを表示
        sendHelpMessage(sender);
        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "==========[ SDAPlugin ]==========");
        sender.sendMessage(ChatColor.YELLOW + "/sda on");
        sender.sendMessage(ChatColor.WHITE + "プラグインを有効化します");
        sender.sendMessage(ChatColor.YELLOW + "/sda off");
        sender.sendMessage(ChatColor.WHITE + "プラグインを無効化します");
        sender.sendMessage(ChatColor.YELLOW + "/sda reload");
        sender.sendMessage(ChatColor.WHITE + "設定ファイルを再読み込みします");
        sender.sendMessage(ChatColor.YELLOW + "/sda test");
        sender.sendMessage(ChatColor.WHITE + "テストメッセージをDiscordに送信します");
        sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "================================");
    }

    // テストメッセージ送信用
    public void sendSimpleDiscordMessage(String title, String message) {
        // Discord チャンネル ID の取得
        String discordChannelId = getConfig().getString("discord-channel-id");
        if (discordChannelId == null || discordChannelId.equals("YOUR-DISCORD-CHANNEL-ID")) {
            getLogger().warning("Discord チャンネル ID が設定されていません！");
            return;
        }

        // DiscordSRV インスタンスの取得
        DiscordSRV discordSRV = (DiscordSRV) Bukkit.getPluginManager().getPlugin("DiscordSRV");
        if (discordSRV != null) {
            // Discord チャンネルへメッセージを送信
            discordSRV.getMainGuild().getTextChannelById(discordChannelId)
                    .sendMessage("**" + title + "**\n" + message)
                    .queue();
        } else {
            getLogger().warning("DiscordSRV プラグインが見つかりません！");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("sda")) {
            if (args.length == 1) {
                List<String> completions = new ArrayList<>(Arrays.asList("on", "off", "reload", "test"));
                return completions;
            }
        }
        return null;
    }
}
