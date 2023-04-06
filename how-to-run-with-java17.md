## Java 17での実行方法

Java 17版は現在試験的なビルドです。

以下の環境で軽くビルドと動作の確認をしています。

- Build
  - macOS 13 (Apple Silicon)
    - Liberica JDK 17.0.6+10-full
  - macOS 13 (Intel)
    - Amazon Corretto 17.0.6
  - Windows 11 (x64)
    - Zulu FX JDK 17.0.6
- Runtime
  - macOS 13 (Apple Silicon)
    - Liberica JDK 17.0.6+10-full
  - macOS 13 (Intel)
    - Amazon Corretto 17.0.6 + Gluon JavaFX SDK 17.0.6
    - Liberica JRE 17.0.6+10-full
  - Windows 11 (x64)
    - Zulu FX JDK 17.0.6

Java 17版を動かすにはLiberica JRE FullやZulu FX等のJavaFXを含むラインタイムがおすすめです。

```console
java -Djavafx.allowjs=true -jar logbook-kai.jar
```

Amazon Corretto等のJavaFXを含まないランタイム上で動かすには、JavaFX SDKをダウンロード、展開し、`--module-path` オプションで指定する必要があります。

以下は、JavaFX SDKを `./javafx-sdk-17.0.6` に展開した場合の例です。

```console
java --module-path ./javafx-sdk-17.0.6/lib --add-modules javafx.controls,javafx.fxml,javafx.media,javafx.swing,javafx.web -Djavafx.allowjs=true -jar logbook-kai.jar
```

開発時はJDK 17ディストリビューションによらず `mvn javafx:run` で実行できます。

```console
mvn javafx:run -Djavafx.allowjs=true -f pom-java17.xml
```
