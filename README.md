# AbemaTV(アベマTV) コメントビューア(非公式)


![メインウィンドウ](https://user-images.githubusercontent.com/22833098/35740349-db3beda0-0877-11e8-9b92-a388658803a1.png)
![過去発言閲覧ウィンドウ](https://user-images.githubusercontent.com/22833098/35740354-e2f306d2-0877-11e8-8377-c210cb8150ac.png)


## Description

sbt + Scala + JavaFX(ScalaFX) + ScalaFXMLで何か作ってみたかった  
手元にAbemaTVのコメント取得+投稿の雑なNodeJSスクリプトが残ってたのでScalaに移植  

なお、コメント投稿やAbemaTV様のブラウザ版での動作から独自(非公式)に挙動を模倣している為  
AbemaTV様の変更等により使用できなくなる恐れや、公開を停止する恐れがあります。  

さらにさらに、動作確認はmacOS Sierra(10.12.6)でしかやってません  
一応[WiX Toolset](http://wixtoolset.org/ "WiX Toolset")を入れたWindowsでビルドすればMSIも生成出来て動作すると思います
  
現行の機能は以下の通り  

- チャンネル切り替え
- チャンネルの番組名表示
- チャンネルの番組表サムネ表示
- チャンネルの概要文表示
- コメント閲覧
- コメント投稿
- ユーザーの過去発言を閲覧


## Requirements

- sbt(v1.1.0)
- JDK9
- [Java VP8 Decoder(v0.2)](http://sourceforge.net/projects/javavp8decoder/ "Java VP8 Decoder")

  
## How to Build

[Java VP8 Decoder](http://sourceforge.net/projects/javavp8decoder/ "Java VP8 Decoder") から`WebPViewer-0.2.jar`を取得してlibディレクトリ直下に置いてください

```bash
$ sbt jdkPackager:packageBin
```

`target/universal/jdkpackager/bundles` 配下にdmgやらが生成される


## 免責

当ソフトウェアの利用において、ユーザーまたは第三者が被った損害について、当方は一切の責任を負わないこととします。
 
