package logbook.internal;

import java.util.stream.Stream;

/**
 * 海域
 *
 */
public enum SeaArea {

    識別札1("識別札1", 1),
    識別札2("識別札2", 2),
    識別札3("識別札3", 3),
    識別札4("識別札4", 4),
    識別札5("識別札5", 5),
    識別札6("識別札6", 6),
    識別札7("識別札7", 7),
    識別札8("識別札8", 8),
    識別札9("識別札9", 9),
    識別札10("識別札10", 10),
    識別札11("識別札11", 11),
    識別札12("識別札12", 12);

    /** 名前 */
    private String name;

    /** 海域(イベント海域のお札) */
    private int area;

    SeaArea(String name, int area) {
        this.name = name;
        this.area = area;
    }

    /**
     * 名前を取得します。
     * @return 名前
     */
    public String getName() {
        return this.name;
    }

    /**
     * 名前を更新します
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 海域(イベント海域のお札)を取得します。
     * @return 海域(イベント海域のお札)
     */
    public int getArea() {
        return this.area;
    }

    @Override
    public String toString() {
        return this.name;
    }

    /**
     * イベント海域を取得します
     *
     * @param area お札
     * @return 海域
     */
    public static SeaArea fromArea(int area) {
        return Stream.of(SeaArea.values()).filter(s -> s.getArea() == area).findAny().orElse(null);
    }
}
