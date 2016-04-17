package logbook.pushbullet.api;

import java.util.List;

public class PushResult {
    private List<SimplePushResult> pushes;

    public List<SimplePushResult> getPushes() {
        return pushes;
    }

    private class SimplePushResult {
        private String ident;
    }
}
