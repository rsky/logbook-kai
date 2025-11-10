package logbook;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import logbook.internal.LoggerHolder;
import logbook.plugin.PluginServices;

/**
 * 国際化対応
 *
 */
public class Messages {

    private static final String BUNDLE_NAME = "logbook.messages"; //$NON-NLS-1$

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(
            BUNDLE_NAME,
            Locale.getDefault(),
            PluginServices.getClassLoader());

    private Messages() {
    }

    /**
     * 指定されたキーの文字列を取得します
     *
     * @param key 目的の文字列のキー
     * @return 指定されたキーの文字列
     */
    public static String getString(String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (Exception e) {
            LoggerHolder.get().warn("リソース・バンドルから値の取得に失敗しました", e);
            return '!' + key + '!';
        }
    }

    /**
     * 指定されたキーの文字列を使ってMessageFormatを作成し、それを使用して指定された引数をフォーマットします
     * これは次の記述と同等です
     * <blockquote>
     *     <code>MessageFormat.format(Messages.getString(key), args)</code>
     * </blockquote>
     *
     * @param key 目的の文字列のキー
     * @param args フォーマットするオブジェクト
     * @return 指定されたキーの文字列
     */
    public static String getString(String key, Object... args) {
        return MessageFormat.format(getString(key), args);
    }
}
