package logbook.internal;

import java.nio.file.Paths;

/**
 * アプリケーションのパス
 */
public final class AppPath {
    /**
     * ユーザーのホームディレクトリ
     */
    public static final String HOME_DIR = System.getProperty("user.home"); //$NON-NLS-1$

    /**
     * 設定が保存されるディレクトリ
     * XDG Base Directory XDG_CONFIG_HOMEのデフォルト値 ~/.config 以下に保存します
     */
    public static final String CONFIG_DIR = Paths.get(HOME_DIR,".config/logbook-kai").toAbsolutePath().toString(); //$NON-NLS-1$

    /**
     * データが保存されるディレクトリ
     * XDG Base Directory XDG_DATA_HOMEのデフォルト値 ~/.local/share 以下に保存します
     */
    public static final String DATA_DIR = Paths.get(HOME_DIR,".local/share/logbook-kai").toAbsolutePath().toString(); //$NON-NLS-1$
}
