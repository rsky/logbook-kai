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
        final String APP_NAME = "logbook-kai"; //$NON-NLS-1$
        final String HOME_DIR = System.getProperty("user.home"); //$NON-NLS-1$
        final String XDG_CONFIG_HOME = System.getenv("XDG_CONFIG_HOME"); //$NON-NLS-1$
        final String XDG_DATA_HOME = System.getenv("XDG_DATA_HOME"); //$NON-NLS-1$

        if (XDG_CONFIG_HOME != null) {
            CONFIG_DIR = Paths.get(XDG_CONFIG_HOME, APP_NAME).toAbsolutePath().toString();
        } else {
            CONFIG_DIR = Paths.get(HOME_DIR, ".config", APP_NAME).toAbsolutePath().toString(); //$NON-NLS-1$
        }

        if (XDG_DATA_HOME != null) {
            DATA_DIR = Paths.get(XDG_DATA_HOME, APP_NAME).toAbsolutePath().toString();
        } else {
            DATA_DIR = Paths.get(HOME_DIR, ".local/share", APP_NAME).toAbsolutePath().toString(); //$NON-NLS-1$
        }
    }
}
