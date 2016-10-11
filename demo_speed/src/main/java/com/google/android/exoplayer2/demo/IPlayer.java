package com.google.android.exoplayer2.demo;

import android.net.Uri;
import android.view.View;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;

import java.util.UUID;

/**
 * Created by ymr on 16/8/12.
 */

public interface IPlayer {
    void setSpeed(float speed);

    void initPlayer(Uri uri);

    boolean hasPlayer();

    void realReleasePlayer();

    void onCreate();

    boolean isMediaNeddSource();

    int getRendererType(int rendererIndx);

    long getCurrentPosition();

    SimpleExoPlayer getExoPlayer();

    void onError();

    void resetPosition();
}
