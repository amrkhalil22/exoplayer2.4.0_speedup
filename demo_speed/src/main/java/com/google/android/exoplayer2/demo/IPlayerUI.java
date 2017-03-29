package com.google.android.exoplayer2.demo;

import android.app.Activity;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.SimpleExoPlayer;

/**
 * Created by ymr on 16/8/12.
 */
public interface IPlayerUI extends ExoPlayer.EventListener, SimpleExoPlayer.VideoListener {
    Activity getContext();

    void showToast(int errorStringId);

    void updateButtonVisibilities();

    void showToast(String myString);

    void onCreatePlayer();
}
