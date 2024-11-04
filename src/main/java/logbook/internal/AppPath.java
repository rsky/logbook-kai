package logbook.internal;

import java.nio.file.Paths;

/**
 * アプリケーションのパス
 */
public final class AppPath {
    /**
     * 設定が保存されるディレクトリ
     */
    public static final String CONFIG_DIR;

    /**
     * データが保存されるディレクトリ
     */
    public static final String DATA_DIR;

    static {
        final String APP_NAME = "logbook-kai";
        final String HOME_DIR = System.getProperty("user.home");
        final String LOGBOOK_KAI_CONFIG_DIR = System.getProperty("logbook_kai.config_dir");
        final String LOGBOOK_KAI_DATA_DIR = System.getProperty("logbook_kai.data_dir");
        final String XDG_CONFIG_HOME = System.getenv("XDG_CONFIG_HOME");
        final String XDG_DATA_HOME = System.getenv("XDG_DATA_HOME");

        if (LOGBOOK_KAI_CONFIG_DIR != null) {
            CONFIG_DIR = LOGBOOK_KAI_CONFIG_DIR;
        } else if (XDG_CONFIG_HOME != null) {
            CONFIG_DIR = Paths.get(XDG_CONFIG_HOME, APP_NAME).toAbsolutePath().toString();
        } else {
            CONFIG_DIR = Paths.get(HOME_DIR, ".config", APP_NAME).toAbsolutePath().toString();
        }

        if (LOGBOOK_KAI_DATA_DIR != null) {
            DATA_DIR = LOGBOOK_KAI_DATA_DIR;
        } else if (XDG_DATA_HOME != null) {
            DATA_DIR = Paths.get(XDG_DATA_HOME, APP_NAME).toAbsolutePath().toString();
        } else {
            DATA_DIR = Paths.get(HOME_DIR, ".local/share", APP_NAME).toAbsolutePath().toString();
        }
    }
}
