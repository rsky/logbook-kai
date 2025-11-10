package logbook.internal;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import logbook.internal.gui.InternalFXMLLoader;
import logbook.internal.gui.Tools;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * アップデートチェック
 *
 */
public class CheckUpdate {

    /** GitHub リポジトリのパス */
    public static final String REPOSITORY_PATH = "rsky/logbook-kai";

    /** 更新確認先 Github tags API */
    private static final String TAGS = "https://api.github.com/repos/" + REPOSITORY_PATH + "/tags";

    /** 更新確認先 Github releases API */
    private static final String RELEASES = "https://api.github.com/repos/" + REPOSITORY_PATH + "/releases/tags/";

    /** ダウンロードサイトを開くを選択したときに開くURL */
    private static final String OPEN_URL = "https://github.com/" + REPOSITORY_PATH + "/releases";

    /** 検索するtagの名前 */
    /* 例えばv20.1.1 の 20.1.1, v23.8.1-rsky-20230809 の 23.8.1 にマッチ */
    static final Pattern TAG_REGEX = Pattern.compile("(\\d+\\.\\d+(?:\\.\\d+)?)(-\\w+-\\d{8})?$");
    static final int TAG_REGEX_GROUP_VERSION = 1;

    /** Prerelease を使う System Property */
    private static  final String USE_PRERELEASE = "logbook.use.prerelease";

    public static void run(Stage stage) {
        run(false, stage);
    }

    public static void run(boolean isStartUp) {
        run(isStartUp, null);
    }

    private static void run(boolean isStartUp, Stage stage) {
        VersionAndTag remoteVersion = remoteVersion();

        if (!Version.UNKNOWN.equals(remoteVersion.version) && Version.getCurrent().compareTo(remoteVersion.version) < 0) {
            Platform.runLater(() -> CheckUpdate.openInfo(Version.getCurrent(), remoteVersion.version, remoteVersion.tag, isStartUp, stage));
        } else if (!isStartUp) {
            Tools.Controls.alert(AlertType.INFORMATION, "更新の確認", "最新のバージョンです。", stage);
        }
    }

    /**
     * 最新のバージョンを取得します。
     * @return 最新のバージョン
     */
    private static VersionAndTag remoteVersion() {
        try {
            JsonArray tags;
            try (JsonReader r = Json.createReader(new ByteArrayInputStream(readURI(URI.create(TAGS))))) {
                tags = r.readArray();
            }
            // Githubのtagsから一番新しいreleasesを取ってくる
            // tagsを処理する
            return tags.stream()
                    // tagの名前
                    .map(val -> val.asJsonObject().getString("name"))
                    // tagの名前にバージョンを含む?実行中のバージョンより新しい?
                    .filter(tagname -> {
                        Matcher m = TAG_REGEX.matcher(tagname);
                        if (m.find()) {
                            Version remote = new Version(m.group(TAG_REGEX_GROUP_VERSION));
                            return (!Version.UNKNOWN.equals(remote) && Version.getCurrent().compareTo(remote) < 0);
                        }
                        return false;
                    })
                    // tagがreleasesにある?
                    .filter(name -> {
                        try {
                            JsonObject releases;
                            try (JsonReader r = Json.createReader(new ByteArrayInputStream(readURI(URI.create(RELEASES + name))))) {
                                releases = r.readObject();
                            }
                            // releasesにない場合は "message": "Not Found"
                            if (releases.getString("message", null) != null)
                                return false;
                            // draftではない
                            if (releases.getBoolean("draft", false))
                                return false;
                            // prereleaseではない
                            if (!Boolean.getBoolean(USE_PRERELEASE) && releases.getBoolean("prerelease", false))
                                return false;
                            // assetsが1つ以上ある
                            if (releases.getJsonArray("assets") == null || releases.getJsonArray("assets").isEmpty())
                                return false;
                            // 最新版が見つかった!
                            return true;
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .findFirst()
                    .map(VersionAndTag::new)
                    .orElse(VersionAndTag.UNKNOWN);
        } catch (Exception e) {
            LoggerHolder.get().warn("最新バージョンの取得に失敗しました", e);
        }
        return VersionAndTag.UNKNOWN;
    }

    private static byte[] readURI(URI uri) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        try {
            // タイムアウトを設定
            connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(10));
            connection.setReadTimeout((int) TimeUnit.SECONDS.toMillis(5));
            // 200 OKの場合にURIを読み取る
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try (InputStream in = connection.getInputStream()) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = in.read(buffer, 0, buffer.length)) > 0) {
                        out.write(buffer, 0, len);
                    }
                }
                return out.toByteArray();
            }
        } finally {
            connection.disconnect();
        }
        return new byte[0];
    }

    private static void openInfo(Version o, Version n, String tag, boolean isStartUp, Stage stage) {
        String message = "新しいバージョンがあります。ダウンロードサイトを開きますか？\n"
                + "現在のバージョン:" + o + "\n"
                + "新しいバージョン:" + n;
        if (isStartUp) {
            message += "\n※自動アップデートチェックは[その他]-[設定]から無効に出来ます";
        }

        ButtonType visit = new ButtonType("ダウンロードサイトを開く");
        ButtonType no = new ButtonType("後で");

        Alert alert = new Alert(AlertType.INFORMATION);
        alert.getDialogPane().getStylesheets().add("logbook/gui/application.css");
        InternalFXMLLoader.setGlobal(alert.getDialogPane());
        alert.setTitle("新しいバージョン");
        alert.setHeaderText("新しいバージョン");
        alert.setContentText(message);
        alert.initOwner(stage);
        alert.getButtonTypes().clear();
        alert.getButtonTypes().addAll(visit, no);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == visit) {
                openBrowser();
            }
        }
    }

    private static void openBrowser() {
        try {
            ThreadManager.getExecutorService()
                    .submit(() -> {
                        Desktop.getDesktop()
                                .browse(URI.create(OPEN_URL));
                        return null;
                    });
        } catch (Exception e) {
            LoggerHolder.get().warn("アップデートチェックで例外", e);
        }
    }

    private static class VersionAndTag {
        private static final VersionAndTag UNKNOWN = new VersionAndTag(Version.UNKNOWN, "");

        private final Version version;

        private final String tag;

        private VersionAndTag(Version version, String tag) {
            this.version = version;
            this.tag = tag;
        }

        VersionAndTag(String tag) {
            Matcher m = TAG_REGEX.matcher(tag);
            if (m.find()) {
                this.version = new Version(m.group(TAG_REGEX_GROUP_VERSION));
                this.tag = tag;
            } else {
                this.version = Version.UNKNOWN;
                this.tag = "";
            }
        }
    }
}
