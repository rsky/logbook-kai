航海日誌 (logbook-kai)
--
[![GitHub release (latest by date)](https://img.shields.io/github/v/release/rsky/logbook-kai)](https://github.com/Sdk0815/logbook-kai/releases/latest)
[![GitHub](https://img.shields.io/github/license/rsky/logbook-kai)](LICENSE)
[![GitHub All Releases](https://img.shields.io/github/downloads/rsky/logbook-kai/total)](https://github.com/Sdk0815/logbook-kai/releases)
[![GitHub Release Date](https://img.shields.io/github/release-date/rsky/logbook-kai)](https://github.com/rsky/logbook-kai/releases)

## ****重要なお知らせ****
これは[sanaehirotaka さん](https://github.com/sanaehirotaka/logbook-kai/)、[Sdk0815 さん](https://github.com/Sdk0815/logbook-kai/)が開発されていた航海日誌(logbook-kai)を2021年夏より[rsky](https://github.com/rsky/logbook-kai/)が個人用にメンテナンスしているものです。

Java 8版、Java 21版を提供しています。Java 17以降で動かす方法は[こちら](how-to-run-with-java21)を参照してください。

艦これAPIの仕様変更等にはできるだけ早く追従できるように努めていますが、新機能の追加は基本的に行わない予定です。

独自の仕様としては、passive mode APIを追加しています。プロキシサーバー等、別のソフトウェアからHTTP POSTでデータを受け取り、GUIのみ航海日誌(logbook-kai)を利用するための機能です。詳細は[こちら](how-to-passive-mode.md)をご覧ください。

## ****（以前の）重要なお知らせ****
航海日誌(logbook-kai)は[sanaehirotaka さん](https://github.com/sanaehirotaka/logbook-kai/)が開発されたものを引き継ぐ形で、2020年から[Sdk0815](https://github.com/Sdk0815/logbook-kai/)によってメンテナンスを継続してきましたが、諸般の事情により今後のメンテナンスを停止することといたしました。現状の最新版である `v21.7.1` が最終盤となります。公式Twitterアカウントも後日閉鎖する予定です。

今後もし開発を引き継いでいただける方がおられましたら引き継ぎのサポートはさせていただこうと思いますので私まで（issueをあげていただくか[Sdk0815＠Twitter](https://twitter.com/Sdk0815)まで）お知らせいただければと思います。ソースコードは当初より公開しておりますので特にお知らせなくforkしていただいてももちろん問題ありません。

引き継いで1年とちょっとでの開発終了となり心苦しい気持ちではありますが、ご理解いただけるとありがたく思います。以上、よろしくお願いいたします。

## ****（もっと以前の）重要なお知らせ****

v20.9.2 以降のバージョンはオリジナルの [sanaehirotaka さんのリポジトリ](https://github.com/sanaehirotaka/logbook-kai/)ではなく
こちらの[Sdk0815 の fork](https://github.com/Sdk0815/logbook-kai/)にて開発を行います。
最新バージョンも[こちら](https://github.com/Sdk0815/logbook-kai/releases)からダウンロードしてください。
今後は[Issue（問題報告・要望）](https://github.com/Sdk0815/logbook-kai/issues)や[Pull Request（変更要求）](https://github.com/Sdk0815/logbook-kai/pulls)などもこちらのリポジトリにオープンしていただきますようお願いします。

Twitter の公式アカウントを作成しました→  [@logbook_kai](https://twitter.com/logbook_kai) （Twitter の命名規則により、間の記号はハイフン `-` ではなくアンダースコア `_` なのでご注意ください。）
今後リリースのお知らせに加え、新機能の実装のやり方等でアンケートを取ったりする予定なので、よろしければ是非フォローしてみてください。サポート依頼を含む質問には返答いたしませんのであらかじめご了承ください。

### 概要

**航海日誌 (logbook-kai)** は、「艦隊これくしょん ～艦これ～」をより遊びやすくするための外部ツールです。

画面がコンパクトなのが特徴です。

![メイン画面](images/overview.png)

![メイン画面(ワイド)](images/overview-wide.png)

### 航海日誌 について

航海日誌 では[Jetty](http://www.eclipse.org/jetty/) で通信内容をキャプチャして内容を解析／表示します。
プロキシ設定を行うことで別のツールと連携することも可能です。

**「艦隊これくしょん ～艦これ～」サーバーに対する通信内容の改変、追加の通信等は一切行っていません。**

MIT ライセンスの下で公開する、自由ソフトウェアです。

### 主な機能

* 遠征・入渠の通知機能 : 1分前になると自動的に通知します。
* 海戦・ドロップ報告書 : 戦闘の状況、ドロップ艦娘などの情報の収集を行えます。
* 所有装備一覧 : 誰がどの装備を持っているかを簡単に確認することが出来ます。
* 所有艦娘一覧 : 艦娘の各種パラメータ(コンディション、制空値、火力値等)の閲覧を行うことが出来ます。
* お風呂に入りたい艦娘 : 修理が必要な艦娘の時間と必要資材を一覧で見ることが出来ます。


### 動作環境
![Java](https://img.shields.io/badge/-Java-007396.svg?logo=java)
![Windows](https://img.shields.io/badge/-Windows-0078D6.svg?logo=windows)
![Debian](https://img.shields.io/badge/-Debian-A81D33.svg?logo=debian)
![Redhat](https://img.shields.io/badge/-Redhat-EE0000.svg?logo=red-hat)
![macOS](https://img.shields.io/badge/-macOS-333333.svg?logo=apple)

Java 8u40以降のJava8、またはJava21がインストールされたWindows,LinuxまたはmacOSが必要です。

**次のJavaVMで動作確認されています。**
- **[Liberica JDK version 8](https://bell-sw.com/pages/downloads/#jdk-8-lts)**
   - 新規に導入する場合、こちらを推奨します。JavaFXを利用しているため必ず**StandardではなくFullをダウンロード**してください。
- **[Liberica JDK version 21](https://bell-sw.com/pages/downloads/#jdk-21-lts)**
  - Java 21版ではこちらを推奨します。同じくFullをダウンロードしてください。
- [Oracle JDK 8](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
   - ダウンロードにOTNアカウントが必要です。

### [ダウンロード](https://github.com/Sdk0815/logbook-kai/releases)

**ご注意ください**

**初期の状態では艦娘の画像が表示出来ません。必ず**[FAQ](faq.md)**をお読みください。**

### [ブラウザの設定(必須)](how-to-preference.md)

### [FAQ](faq.md)

#### プラグイン
* [Pushbullet Plugin](https://github.com/rsky/logbook-kai-plugins)
  * 遠征・入渠の通知をiPhone/Android端末へプッシュ通知することが可能になります。

### スクリーンショット

* メイン画面

![メイン画面](images/overview.png)

* 所有装備一覧

![所有装備一覧そのいち](images/items1.png)
![所有装備一覧そのに](images/items2.png)

* 戦闘ログ

![戦闘ログそのいち](images/battlelog1.png)
![戦闘ログそのに](images/battlelog2.png)

### 開発者向け

#### [ビルド方法](how-to-build.md)

#### [プラグイン開発](how-to-develop.md)

### ライセンス

* [The MIT License (MIT)](LICENSE)

MIT ライセンスの下で公開する、自由ソフトウェアです。

### 使用ライブラリとライセンス

以下のライブラリを使用しています。

#### [JSON Processing(JSR 353)](https://jsonp.java.net/)

* COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL - Version 1.1)
* GNU General Public License (GPL - Version 2, June 1991) with the Classpath Exception
* **ライセンス全文 :** [https://jsonp.java.net/license.html](https://jsonp.java.net/license.html)

#### [Jetty](http://www.eclipse.org/jetty/)

* Apache License 2.0
* Eclipse Public License 1.0
* **ライセンス全文 :** [http://www.eclipse.org/jetty/licenses.php](http://www.eclipse.org/jetty/licenses.php)

#### [commons-logging](https://commons.apache.org/proper/commons-logging/)

* Apache License 2.0
* **ライセンス全文 :** [http://www.apache.org/licenses/](http://www.apache.org/licenses/)

#### [Apache Log4j 2](http://logging.apache.org/log4j/2.x/)

* Apache License 2.0
* **ライセンス全文 :** [http://logging.apache.org/log4j/2.x/license.html](http://logging.apache.org/log4j/2.x/license.html)

#### [ControlsFX](http://fxexperience.com/controlsfx/)

* The BSD 3-Clause License
* **ライセンス全文 :** [https://bitbucket.org/controlsfx/controlsfx/src/default/license.txt?fileviewer=file-view-default](https://bitbucket.org/controlsfx/controlsfx/src/default/license.txt?fileviewer=file-view-default)
