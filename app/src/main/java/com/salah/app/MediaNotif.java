package com.salah.app;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;

/**
 * إشعار التشغيل بشكل مشغّل الصوت بتاع أندرويد (MediaStyle + MediaSession):
 * بيظهر فوق في كارت التشغيل مش في قسم Silent، بخلفية خضرا وأيقونة الهلال،
 * وأزرار بأيقونات، وبيتحكم من شاشة القفل والسماعة البلوتوث.
 */
final class MediaNotif {

    /** اللي الخدمة بتعمله لما المستخدم يضغط من الإشعار أو السماعة. */
    interface Controls {
        void toggle();

        void next();

        void stop();
    }

    private static Bitmap art;

    private final MediaSession session;

    MediaNotif(Context c, String tag, Controls controls) {
        session = new MediaSession(c, tag);
        session.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                controls.toggle();
            }

            @Override
            public void onPause() {
                controls.toggle();
            }

            @Override
            public void onSkipToNext() {
                controls.next();
            }

            @Override
            public void onStop() {
                controls.stop();
            }
        });
        session.setActive(true);
    }

    void release() {
        session.setActive(false);
        session.release();
    }

    /**
     * يبني الإشعار. next = null لو مفيش "التالي".
     * الأزرار: تشغيل/إيقاف، التالي (لو موجود)، إنهاء.
     */
    Notification build(Context c, String channel, String title, String text, boolean playing,
                       PendingIntent open, PendingIntent toggle, PendingIntent next,
                       PendingIntent stop) {
        Bitmap artwork = artwork(c);
        session.setMetadata(new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, text)
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artwork)
                .build());
        long actions = PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_PLAY
                | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_STOP;
        if (next != null) actions |= PlaybackState.ACTION_SKIP_TO_NEXT;
        session.setPlaybackState(new PlaybackState.Builder()
                .setActions(actions)
                .setState(playing ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED,
                        PlaybackState.PLAYBACK_POSITION_UNKNOWN, playing ? 1f : 0f)
                .build());

        Notification.Builder b = new Notification.Builder(c, channel)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(Notifications.accent(c))
                .setColorized(true)
                .setLargeIcon(artwork)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(open)
                .setDeleteIntent(stop)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setOngoing(playing);

        b.addAction(action(c, playing ? R.drawable.ic_pause : R.drawable.ic_play,
                playing ? "إيقاف مؤقت" : "تشغيل", toggle));
        if (next != null) b.addAction(action(c, R.drawable.ic_next, "التالي", next));
        b.addAction(action(c, R.drawable.ic_stop, "إنهاء", stop));

        int[] compact = next != null ? new int[]{0, 1, 2} : new int[]{0, 1};
        b.setStyle(new Notification.MediaStyle()
                .setMediaSession(session.getSessionToken())
                .setShowActionsInCompactView(compact));
        return b.build();
    }

    private static Notification.Action action(Context c, int icon, String label,
                                              PendingIntent pi) {
        return new Notification.Action.Builder(Icon.createWithResource(c, icon), label, pi)
                .build();
    }

    /** صورة الكارت: مربع أخضر مدوّر وفي نصه هلال التطبيق أبيض. */
    static synchronized Bitmap artwork(Context c) {
        if (art != null) return art;
        int size = 256;
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Notifications.accent(c));
        canvas.drawRoundRect(new RectF(0, 0, size, size), 48, 48, p);
        Drawable moon = c.getDrawable(R.drawable.ic_notification);
        if (moon != null) {
            int pad = 56;
            moon.setBounds(pad, pad, size - pad, size - pad);
            moon.draw(canvas);
        }
        art = bmp;
        return art;
    }
}
