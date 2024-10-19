# SDAPlugin

このプラグインは、[**DiscordSRV**](https://github.com/DiscordSRV/DiscordSRV)のAPIを使用した非公式のプラグインです。  
基本的にDiscordSRVの設定を済ませ、confit.ymlにチャンネル名を設定すれば動作します。  

# 欠陥  
バージョンアップをするとconfigファイルが更新されません。方法もわからないので詳しい方居ましたら教えて下さい。((  
最初から一番新しいバージョンを導入することを推奨します。

### [各コマンド](src/main/resources/config.yml)  
`/sda on/off`  
SDAPluginの有効化/無効化  
`/sda reload`  
SDAPluginのConfigファイルの再読み込み  
  
### [パーミッション](src/main/resources/plugin.yml)  
`sda.use` -上記コマンドを使用するためのパーミッション  
`sda.bypass` -検知をバイパスするパーミッション  
  
### [Config.ymlファイルの見方](src/main/resources/config.yml)  
`plugin-enabled: true/false`    SDAPluginの有効化/無効化  
`detectable-items:`    検出するアイテムと危険度(レベル2～5まで)
`detect-bed-use: true/false`    ベッド爆破を検知の有効化/無効化  
`discord-channel-id: 'YOUR-DISCORD-CHANNEL-ID'   ` DiscordのチャンネルIDを`'YOUR-DISCORD-CHANNEL-ID'`に入力。  
`detect-ignite-block: true/false`    ブロック着火検知を有効化/無効化  
`detect-lavabucket-use: true/false`    溶岩バケツ使用検知を有効化/無効化  
`notification-cooldown: [seconds]`    通知のクールタイム（秒単位）  
`enable-notify-commands: true/false`    コマンド通知を有効化/無効化  
`notify-commands:`    通知を送信するコマンドのリスト  
`debug-mode: true/false`    デバッグモードの有効化/無効化  
**注意!**  
こちらのプラグインはBukkit/Spigot/papermcの環境のみ動作します！  
また、動作確認自体はpapermcのみで確認しております。  
  
現在は検知できるものは少ないですが、今後のアプデートでクリックの検知など  
様々なものを追加する予定です。  
  
こちらのプラグインは完全にβ版なので、configファイルに追加した検知したいアイテムは動作しない場合があります。ご了承ください。  
  
# 宣伝  
## 当プラグインの作成者が運営しているサーバーがあるので是非入ってください！  
## [【舞雑サーバー公式Discord】](https://discord.gg/DVEUK4gYaz)  
![survivalspawnpicture](https://i.gyazo.com/d2216fa5eaf169512ad4cb2f43ad841a.png)  
