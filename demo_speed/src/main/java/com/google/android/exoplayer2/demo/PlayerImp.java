package com.google.android.exoplayer2.demo;

import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

public class PlayerImp implements IPlayer {
    private final IPlayerUI playerUI;
    private SimpleExoPlayer player;
    private EventLogger eventLogger;
    private String userAgent;
    private DefaultDataSourceFactory mediaDataSourceFactory;
    private MappingTrackSelector trackSelector;
    private boolean playerNeedsSource;
    private long playerPosition;
    private Handler mainHandler = new Handler();

    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    private float speed = 1.0f;
    private TrackSelection.Factory videoTrackSelectionFactory;
    private Uri uri;

    public PlayerImp(IPlayerUI playerUI) {
        this.playerUI = playerUI;
    }

    @Override
    public void setSpeed(float speed) {
        this.speed = speed;
        if (player != null) {
            flagThePosition();
            player.setPlaybackSpeed(speed);
        }
    }

    @Override
    public void initPlayer(Uri uri) {
        this.uri = uri;
        if (!hasPlayer()) {
            boolean preferExtensionDecoders = false;
            eventLogger = new EventLogger();
            videoTrackSelectionFactory = new AdaptiveVideoTrackSelection.Factory(BANDWIDTH_METER);
            trackSelector = new DefaultTrackSelector(mainHandler, videoTrackSelectionFactory);
            trackSelector.addListener(playerUI);
            trackSelector.addListener(eventLogger);
            newPlayer(preferExtensionDecoders);
            playerNeedsSource = true;
        }
        if (playerNeedsSource) {
            MediaSource mediaSource = buildMediaSource(uri,"");
            player.prepare(mediaSource, false);
            playerNeedsSource = false;
            playerUI.updateButtonVisibilities();
        }
    }

    @Override
    public boolean hasPlayer() {
        return player != null;
    }

    @Override
    public void realReleasePlayer() {
        //shouldRestorePosition = playerTimeline != null && playerTimeline.isFinal();
        player.release();
        player = null;
        eventLogger = null;
        trackSelector = null;
    }

    private void newPlayer(boolean preferExtensionDecoders) {
        player = ExoPlayerFactory.newSimpleInstance(playerUI.getContext(), trackSelector, new DefaultLoadControl(),
                null, preferExtensionDecoders);
        player.addListener(playerUI);
        player.addListener(eventLogger);
        player.setId3Output(eventLogger);
        player.setVideoListener(playerUI);

        player.seekTo(playerPosition);
        player.setPlayWhenReady(true);
        player.setPlaybackSpeed(speed);
        playerUI.onCreatePlayer();
    }


    @Override
    public void onCreate() {
        userAgent = Util.getUserAgent(playerUI.getContext(), "ExoPlayerDemo");
        mediaDataSourceFactory = new DefaultDataSourceFactory(playerUI.getContext(), userAgent, BANDWIDTH_METER);
    }

    private MediaSource buildMediaSource(Uri uri, String overrideExtension) {
        int type = Util.inferContentType(!TextUtils.isEmpty(overrideExtension) ? "." + overrideExtension
                : uri.getLastPathSegment());
        switch (type) {
            case C.TYPE_SS:
                return new SsMediaSource(uri, new DefaultDataSourceFactory(playerUI.getContext(), userAgent),
                        new DefaultSsChunkSource.Factory(mediaDataSourceFactory), mainHandler, eventLogger);
            case C.TYPE_DASH:
                return new DashMediaSource(uri, new DefaultDataSourceFactory(playerUI.getContext(), userAgent),
                        new DefaultDashChunkSource.Factory(mediaDataSourceFactory), mainHandler, eventLogger);
            case C.TYPE_HLS:
                return new HlsMediaSource(uri, mediaDataSourceFactory, mainHandler, eventLogger);
            case C.TYPE_OTHER:
                return new ExtractorMediaSource(uri, mediaDataSourceFactory, new DefaultExtractorsFactory(),
                        mainHandler, eventLogger);
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }

    @Override
    public boolean isMediaNeddSource() {
        return playerNeedsSource;
    }

    @Override
    public int getRendererType(int rendererIndx) {
        return player.getRendererType(rendererIndx);
    }

    @Override
    public long getCurrentPosition() {
        return player.getCurrentPosition();
    }

    @Override
    public SimpleExoPlayer getExoPlayer() {
        return player;
    }

    @Override
    public void onError() {
        playerNeedsSource = true;
    }

    @Override
    public void resetPosition() {
        playerPosition = 0;
    }

    private void flagThePosition() {
      playerPosition = getCurrentPosition();
    }
}