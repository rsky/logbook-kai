## Java 21での実行方法

Java 21版は現在試験的なビルドです。

以下の環境で軽くビルドと動作の確認をしています。

- Build
  - macOS 15 (aarch64)
    - Liberica JDK 21.0.5+11-full
  - macOS 15 (x86_64)
    - Liberica JDK 21.0.5+11-full
- Runtime
  - macOS 15 (aarch64)
    - Liberica JDK 21.0.5+11-full
  - macOS 15 (x86_64)
    - Liberica JDK 21.0.5+11-full
  - Windows 10 (x86_64)
    - Liberica JRE 21.0.5+11-full

Java 21版を動かすにはLiberica JRE FullやZulu FX等のJavaFXを含むランタイムがおすすめです。

Java 8版との違いは `-Djavafx.allowjs=true` オプションを付けて起動する点で、Windows用起動スクリプトlaunch.batにはこのオプションが含まれています。

```console
java -Djavafx.allowjs=true -jar logbook-kai.jar
```

JavaFXを含まないランタイム上で動かすには、JavaFX SDKをダウンロード、展開し、`--module-path` オプションで指定する必要があります。

以下は、JavaFX SDKを `./javafx-sdk-21.0.5` に展開した場合の例です。

```console
java --module-path ./javafx-sdk-21.0.5/lib --add-modules javafx.controls,javafx.fxml,javafx.media,javafx.swing,javafx.web -Djavafx.allowjs=true -jar logbook-kai.jar
```

## Java 21での開発方法

Java 17からJREからJavaScript実行エンジンNashornが削除されています。OpenJDKプロジェクトのNashornを利用するため、ビルドするには
ソースコードを一部改変してimport元を変更する必要があります。

```diff
diff --git a/src/main/java/logbook/internal/gui/BattleLogScriptController.java b/src/main/java/logbook/internal/gui/BattleLogScriptController.java
--- a/src/main/java/logbook/internal/gui/BattleLogScriptController.java
+++ b/src/main/java/logbook/internal/gui/BattleLogScriptController.java
@@ -26,8 +26,8 @@ import javafx.scene.control.TextField;
 import javafx.scene.layout.Priority;
 import javafx.scene.layout.VBox;
 
-import jdk.nashorn.api.scripting.JSObject; // Java 8
-//import org.openjdk.nashorn.api.scripting.JSObject; // Java 17+
+//import jdk.nashorn.api.scripting.JSObject; // Java 8
+import org.openjdk.nashorn.api.scripting.JSObject; // Java 17+
 
 import com.fasterxml.jackson.databind.ObjectMapper;
 
```

Java 21版をビルドするには、Mavenで `pom-java21.xml` を指定します。

```console
mvn package -f pom-java21.xml
```

開発時はJDK 21ディストリビューションにOpenJFXが含まれているかどうかによらず `mvn javafx:run` で実行できます。

```console
mvn javafx:run -Djavafx.allowjs=true -f pom-java21.xml
```
