package logbook.pushbullet;

import logbook.pushbullet.api.PushParameter;
import logbook.pushbullet.api.PushbulletService;
import logbook.pushbullet.api.ServiceFactory;
import logbook.pushbullet.bean.Channel;
import logbook.pushbullet.bean.ChannelCollection;
import logbook.pushbullet.bean.Device;
import logbook.pushbullet.bean.DeviceCollection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rx.schedulers.JavaFxScheduler;
import rx.schedulers.Schedulers;

public class Pusher {
    private String accessToken;

    public Pusher(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * 設定で選択した端末およびチャンネルにプッシュする
     *
     * @param title   String
     * @param message String
     */
    public void pushToSelectedTargets(String title, String message) {
        PushbulletService service = ServiceFactory.create(accessToken);

        DeviceCollection.get()
                .stream()
                .filter(Device::isActive)
                .filter(Device::isSelected)
                .forEach(device -> service.push(PushParameter.noteToDevice(device, title, message))
                        .subscribeOn(Schedulers.io())
                        .observeOn(JavaFxScheduler.getInstance())
                        .subscribe(result -> LoggerHolder.LOG.debug(result.getPushes()), LoggerHolder.LOG::error));

        ChannelCollection.get()
                .stream()
                .filter(Channel::isActive)
                .filter(Channel::isSelected)
                .forEach(channel -> service.push(PushParameter.noteToChannel(channel, title, message))
                        .subscribeOn(Schedulers.io())
                        .observeOn(JavaFxScheduler.getInstance())
                        .subscribe(result -> LoggerHolder.LOG.debug(result.getPushes()), LoggerHolder.LOG::error));
    }

    private static class LoggerHolder {
        /**
         * ロガー
         */
        private static final Logger LOG = LogManager.getLogger(Pusher.class);
    }
}
