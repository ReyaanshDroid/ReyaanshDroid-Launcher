package com.reyaansh72.reyaanshdroidlauncher;

import android.content.ComponentName;
import android.content.Intent;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import androidx.annotation.Nullable;

import java.util.List;

public class MediaListenerService extends NotificationListenerService {

    public static final String ACTION_MEDIA_UPDATE = "com.reyaansh72.reyaanshdroidlauncher.MEDIA_UPDATE";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_ARTIST = "artist";
    public static final String EXTRA_PLAYING = "playing";

    private MediaSessionManager mediaSessionManager;
    private MediaController currentController;

    private final MediaController.Callback callback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(@Nullable PlaybackState state) {
            updateMediaInfo();
        }

        @Override
        public void onMetadataChanged(@Nullable MediaMetadata metadata) {
            updateMediaInfo();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mediaSessionManager = (MediaSessionManager) getSystemService(MEDIA_SESSION_SERVICE);
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        updateCurrentController();
    }

    private void updateCurrentController() {
        if (mediaSessionManager == null) return;
        List<MediaController> controllers = mediaSessionManager.getActiveSessions(new ComponentName(this, MediaListenerService.class));
        if (!controllers.isEmpty()) {
            if (currentController != null) {
                currentController.unregisterCallback(callback);
            }
            currentController = controllers.get(0);
            currentController.registerCallback(callback);
            updateMediaInfo();
        }
    }

    private void updateMediaInfo() {
        if (currentController == null) return;
        MediaMetadata metadata = currentController.getMetadata();
        PlaybackState state = currentController.getPlaybackState();

        if (metadata != null) {
            String title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
            String artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
            boolean isPlaying = state != null && state.getState() == PlaybackState.STATE_PLAYING;

            Intent intent = new Intent(ACTION_MEDIA_UPDATE);
            intent.putExtra(EXTRA_TITLE, title);
            intent.putExtra(EXTRA_ARTIST, artist);
            intent.putExtra(EXTRA_PLAYING, isPlaying);
            sendBroadcast(intent);
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        updateCurrentController();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        updateCurrentController();
    }
}
