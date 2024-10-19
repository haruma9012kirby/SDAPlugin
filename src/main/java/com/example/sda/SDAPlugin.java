package com.example.sda;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import github.scarsz.discordsrv.DiscordSRV;
import net.md_5.bungee.api.ChatColor;

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
    private Map<String, String> messages = new ConcurrentHashMap<>(); // ConcurrentHashMapを使用
    private boolean enableNotifyCommands;
    private boolean debugMode;
    private String languageIdentifier;
    @Override
    public void onEnable() {
        messages = new HashMap<>(); // messagesマップを初期化
        // SDAPluginのアスキーアートをコンソールに表示
        logAsciiArt();
        createDefaultLanguageFile(); // デフォルトの言語設定ファイルをコピーするメソッドを呼び出し
        createLanguageConfigFiles(); // 言語設定ファイルをコピーするメソッドを呼び出し
        loadLanguageConfig(); // 言語設定を読み込む
        saveDefaultConfig(); // デフォルトの設定ファイルをコピーする
        loadConfigValues(); // 設定値を読み込む
        Bukkit.getPluginManager().registerEvents(new GriefingItemListener(this), this); // イベントリスナーを登録
        
        getCommand("sda").setTabCompleter(this);
    }

    @Override
    public void onDisable() {
        logInfo("plugin-disabled");
    }

    // config.ymlの値を読み込むメソッド
    public void loadConfigValues() {
        FileConfiguration config = getConfig();
        pluginEnabled = config.getBoolean("plugin-enabled", true); // プラグインの有効化
        detectItems = config.getStringList("detectable-items"); // 検知するアイテムの設定
        detectBedUse = config.getBoolean("detect-bed-use", true); // ベッドの使用検知の有効化
        discordChannelId = getConfig().getString("discord-channel-id", "YOUR-DISCORD-CHANNEL-ID"); // デフォルトのチャンネルID
        blockignite = config.getBoolean("detect-ignite-block", true); // ブロックの着火検知の有効化
        lavaBucketUse = config.getBoolean("detect-lavabucket-use", true); // 溶岩バケツの使用検知の有効化
        notificationCooldown = config.getInt("notification-cooldown", 1); // 通知のクールタイム
        enableNotifyCommands = config.getBoolean("enable-notify-commands", true); // コマンド検知の有効化
        notifyCommands = config.getStringList("notify-commands"); // 通知を送信するコマンドの設定
        debugMode = config.getBoolean("debug-mode", false); // デバッグモードの有効化
        languageIdentifier = config.getString("language-identifier", "ja"); // 言語識別子の設定
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
    
    public boolean isEnableNotifyCommands() {
        return enableNotifyCommands;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    // 言語設定を読み込むメソッド
    private void loadLanguageConfig() {
        FileConfiguration langConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "language.yml"));
        String configLanguage = langConfig.getString("language", "ja"); // ローカル変数の名前を変更
        ConfigurationSection langSection = langConfig.getConfigurationSection("messages." + configLanguage);
        if (langSection != null) {
            for (String key : langSection.getKeys(false)) {
                messages.put(key, langSection.getString(key));
            }
        }
        // 言語に応じた設定を読み込む
        if (langSection != null && configLanguage.equals("en")) {
            replaceConfigFile("config-en.yml");
            logInfo(getMessage("config-en"));
        } else if (langSection != null) {
            replaceConfigFile("config-jp.yml");
            logInfo(getMessage("config-jp"));
        }
    }

    public String getMessage(String key) {
        return messages.getOrDefault(key, "Message not found");
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
                        sender.sendMessage(ChatColor.GREEN + String.format("[SDA] %s", getMessage("plugin-enabled")));
                        logDebug(getMessage("plugin-enabled-debug"));
                        return true;
                    case "off":
                        pluginEnabled = false;
                        getConfig().set("plugin-enabled", false);
                        saveConfig();
                        sender.sendMessage(ChatColor.RED + String.format("[SDA] %s", getMessage("plugin-disabled")));
                        logDebug(getMessage("plugin-disabled-debug"));
                        return true;
                    case "reload":
                        reloadConfig();
                        loadConfigValues();
                        
                        // 言語設定を再読み込み
                        loadLanguageConfig();
                        
                        // 言語設定に基づいてconfigを置き換える
                        replaceConfigBasedOnLanguage();
                        
                        sender.sendMessage(ChatColor.GREEN + String.format("[SDA] %s", getMessage("config-reloaded")));
                        if (getDiscordChannelId().equals("YOUR-DISCORD-CHANNEL-ID")) {
                            sender.sendMessage(ChatColor.RED + String.format("[SDA] %s", getMessage("discord-warning")));
                            logInfo(getMessage("discord-warning"));
                        }
                        logDebug(getMessage("config-reloaded-debug"));
                        return true;
                    case "test":
                        if (sender instanceof Player) {
                            Player player = (Player) sender;
                            sendSimpleDiscordMessage(getMessage("title-test-message"), player.getName() + " " + getMessage("test-message"));
                            if (getDiscordChannelId().equals("YOUR-DISCORD-CHANNEL-ID")) {
                                sender.sendMessage(ChatColor.RED + String.format("[SDA] %s", getMessage("discord-warning")));
                                logInfo(getMessage("discord-warning"));
                            } else {
                                sender.sendMessage(ChatColor.YELLOW + String.format("[SDA] %s", getMessage("test-message")));
                            }
                            logDebug(getMessage("test-message-sent-debug"));
                        } else {
                            sender.sendMessage(ChatColor.RED + String.format("[SDA] %s", getMessage("player-only")));
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
        sender.sendMessage(ChatColor.WHITE + getMessage("help-on"));
        sender.sendMessage(ChatColor.YELLOW + "/sda off");
        sender.sendMessage(ChatColor.WHITE + getMessage("help-off"));
        sender.sendMessage(ChatColor.YELLOW + "/sda reload");
        sender.sendMessage(ChatColor.WHITE + getMessage("help-reload"));
        sender.sendMessage(ChatColor.YELLOW + "/sda test");
        sender.sendMessage(ChatColor.WHITE + getMessage("help-test"));
        sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "================================");
    }

    // テストメッセージ送信用
    public void sendSimpleDiscordMessage(String title, String message) {
        // Discord チャンネル ID の取得
        String localDiscordChannelId = getConfig().getString("discord-channel-id");
        if (localDiscordChannelId == null || localDiscordChannelId.equals("YOUR-DISCORD-CHANNEL-ID")) {
            logInfo(getMessage("discord-warning"));
            return;
        }

        // DiscordSRV インスタンスの取得
        DiscordSRV discordSRV = (DiscordSRV) Bukkit.getPluginManager().getPlugin("DiscordSRV");
        if (discordSRV != null) {
            // Discord チャンネルへメッセージを送信
            discordSRV.getMainGuild().getTextChannelById(localDiscordChannelId)
                    .sendMessage("**" + title + "**\n" + message)
                    .queue();
            logDebug(getMessage("discord-message-sent-debug"));
        } else {
            logInfo(getMessage("discord-srv-not-found"));
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

    // 言語に応じてconfig.ymlを置き換えるメソッド
    private void replaceConfigFile(String configFileName) {
        File languageDir = new File(getDataFolder(), "language");
        File sourceFile = new File(languageDir, configFileName);
        File targetFile = new File(getDataFolder(), "config.yml");

        logDebug(String.format("Replacing config.yml with %s", configFileName));

        try {
            Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logInfo(String.format("Config file replaced with: %s", configFileName));
        } catch (IOException e) {
            logInfo(String.format("%s: %s", getMessage("failed-to-replace-config-file"), e.getMessage()));
        }
    }

    // デフォルトのlanguage.ymlを生成するメソッド
    private void createDefaultLanguageFile() {
        File langFile = new File(getDataFolder(), "language.yml");
        if (!langFile.exists()) {
            try {
                getDataFolder().mkdirs();
                Files.copy(getResource("language.yml"), langFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                logInfo("Default language.yml created.");
            } catch (IOException e) {
                logInfo(String.format("%s: %s", getMessage("failed-to-create-default-language-file"), e.getMessage()));
            }
        }
    }

    // 言語設定ファイルをコピーするメソッド
    private void createLanguageConfigFiles() {
        File languageDir = new File(getDataFolder(), "language");
        languageDir.mkdirs();

        copyDefaultConfig("config-jp.yml", languageDir);
        copyDefaultConfig("config-en.yml", languageDir);
    }

    // デフォルトの設定ファイルをコピーするメソッド
    private void copyDefaultConfig(String fileName, File targetDir) {
        File targetFile = new File(targetDir, fileName);
        if (!targetFile.exists()) {
            try {
                Files.copy(getResource(fileName), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                logInfo(String.format("Default %s copied to %s", fileName, targetDir.getAbsolutePath()));
            } catch (IOException e) {
                logInfo(String.format("Failed to copy default %s: %s", fileName, e.getMessage()));
            }
        }
    }

    private void replaceConfigBasedOnLanguage() {
        File langFile = new File(getDataFolder(), "language.yml");
        FileConfiguration langConfig = YamlConfiguration.loadConfiguration(langFile);
    
        String currentLanguage = langConfig.getString("language", "ja");  // 現在の言語設定
        String previousLanguage = langConfig.getString("previous-language", currentLanguage);  // 前回の言語設定
        boolean configReplaced = langConfig.getBoolean("config-replaced", false); // configファイルが置き換え済みかどうか
    
        // 言語が変更された場合、もしくはconfigが置き換えられていない場合のみ置き換える
        if (!currentLanguage.equals(previousLanguage) || !configReplaced) {
            if (currentLanguage.equals("en")) {
                languageIdentifier = "en";
            } else {
                languageIdentifier = "ja";
            }
    
            // 言語設定の変更を保存
            langConfig.set("previous-language", currentLanguage);
            langConfig.set("config-replaced", true); // configが置き換えられたことを記録
            try {
                langConfig.save(langFile);
            } catch (IOException e) {
                logInfo(String.format("Failed to save language configuration: %s", e.getMessage()));
            }
        } else {
            logInfo("Language has not changed and config was already replaced, skipping config replacement.");
        }
    }
    private void logAsciiArt() {
        getLogger().info("┌──────────────────────────────────────────────────────────────────────┐");
        getLogger().info("│            ____  ____    _    ____  _             _                  │");
        getLogger().info("│           / ___||  _ \\  / \\  |  _ \\| |_   _  __ _(_)_ __             │");
        getLogger().info("│           \\___ \\| | | |/ _ \\ | |_) | | | | |/ _` | | '_ \\            │");
        getLogger().info("│            ___) | |_| / ___ \\|  __/| | |_| | (_| | | | | |           │");
        getLogger().info("│           |____/|____/_/   \\_\\_|   |_|\\__,_|\\__, |_|_| |_|           │");
        getLogger().info("│                                             |___/                    │");
        getLogger().info("│               created by Soryu-haruma(Asl,haruma9012kirby)           │");   
        getLogger().info("│       Repository: https://github.com/haruma9012kirby/SDAPlugin       │");
        getLogger().info("│          Discord: https://discord.gg/DVEUK4gYaz                      │");
        getLogger().info("│  Running version: v1.15-beta(tested:MC: 1.21.1)                      │");
        getLogger().info("└──────────────────────────────────────────────────────────────────────┘");
    }

    // デバッグログ出力メソッド
    public void logDebug(String message) {
        if (debugMode) {
            getLogger().info(String.format("[SDA-DEBUG] %s", message));
        }
    }
    // 一般的なログ出力メソッド
    public void logInfo(String message) {
        getLogger().info(String.format("[SDA] %s", message));
    }
        
}
