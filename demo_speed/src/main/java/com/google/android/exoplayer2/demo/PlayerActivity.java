/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2.demo;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer.DecoderInitializationException;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil.DecoderQueryException;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelections;
import com.google.android.exoplayer2.ui.DebugTextViewHelper;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.util.Util;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

/**
 * An activity that plays media using {@link SimpleExoPlayer}.
 */
public class PlayerActivity extends Activity implements OnKeyListener, OnTouchListener,
    OnClickListener, ExoPlayer.EventListener, SimpleExoPlayer.VideoListener,
    MappingTrackSelector.EventListener, IPlayerUI, PlaybackControlView.VisibilityListener {

  public static final String DRM_SCHEME_UUID_EXTRA = "drm_scheme_uuid";
  public static final String DRM_LICENSE_URL = "drm_license_url";
  public static final String PREFER_EXTENSION_DECODERS = "prefer_extension_decoders";

  public static final String ACTION_VIEW = "com.google.android.exoplayer.demo.action.VIEW";
  public static final String EXTENSION_EXTRA = "extension";

  public static final String ACTION_VIEW_LIST =
      "com.google.android.exoplayer.demo.action.VIEW_LIST";
  public static final String URI_LIST_EXTRA = "uri_list";
  public static final String EXTENSION_LIST_EXTRA = "extension_list";

  private static final CookieManager DEFAULT_COOKIE_MANAGER;

  static {
    DEFAULT_COOKIE_MANAGER = new CookieManager();
    DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
  }

  private IPlayer player = new PlayerImp(this);

  private View rootView;
  private LinearLayout debugRootView;
  private TextView debugTextView;
  private Button retryButton;

  private DebugTextViewHelper debugViewHelper;
  private Spinner spinnerSpeeds;
  private SimpleExoPlayerView simpleExoPlayerView;

  // Activity lifecycle

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    player.onCreate();
    if (CookieHandler.getDefault() != DEFAULT_COOKIE_MANAGER) {
      CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER);
    }

    setContentView(R.layout.player_activity);
    rootView = findViewById(R.id.root);
    spinnerSpeeds = ((Spinner) findViewById(R.id.spinner_speeds));
    rootView.setOnTouchListener(this);
    rootView.setOnKeyListener(this);
    final String[] speeds = getResources().getStringArray(R.array.speed_values);
    spinnerSpeeds.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        player.setSpeed(Float.valueOf(speeds[position]));
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {

      }
    });
    debugRootView = (LinearLayout) findViewById(R.id.controls_root);
    debugTextView = (TextView) findViewById(R.id.debug_text_view);
    retryButton = (Button) findViewById(R.id.retry_button);
    retryButton.setOnClickListener(this);
    player.setSpeed(1.0f);
    simpleExoPlayerView = ((SimpleExoPlayerView) findViewById(R.id.player_view));
    simpleExoPlayerView.setControllerVisibilityListener(this);
    simpleExoPlayerView.requestFocus();
  }

  @Override
  public void onNewIntent(Intent intent) {
    releasePlayer();
    player.resetPosition();
    setIntent(intent);
  }

  @Override
  public void onStart() {
    super.onStart();
    initAfter23();
  }

  private void initAfter23() {
    if (Util.SDK_INT > 23) {
      initPlayer();
    }
  }

  private void initPlayer() {
    Uri uri = Uri.parse("asset:///cf752b1c12ce452b3040cab2f90bc265_h264818000nero_aac32-1.mp4");
    player.initPlayer(uri);
    //trackSelectionHelper = player.createTrackSelectionHelper();
    debugViewHelper = new DebugTextViewHelper(player.getExoPlayer(), debugTextView);
    debugViewHelper.start();
  }

  @Override
  public void onResume() {
    super.onResume();
    initPre23();
  }

  private void initPre23() {
    if ((Util.SDK_INT <= 23 || !player.hasPlayer())) {
      initPlayer();
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    releasePre23();
  }

  private void releasePre23() {
    if (Util.SDK_INT <= 23) {
      releasePlayer();
    }
  }

  @Override
  public void onStop() {
    super.onStop();
    releaseAfter23();
  }

  private void releaseAfter23() {
    if (Util.SDK_INT > 23) {
      releasePlayer();
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions,
      int[] grantResults) {
    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      initPlayer();
    } else {
      showToast(R.string.storage_permission_denied);
      finish();
    }
  }

  // OnTouchListener methods

  @Override
  public boolean onTouch(View view, MotionEvent motionEvent) {
    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
      toggleControlsVisibility();
    } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
      view.performClick();
    }
    return true;
  }

  // OnKeyListener methods

  @Override
  public boolean onKey(View v, int keyCode, KeyEvent event) {
    return keyCode != KeyEvent.KEYCODE_BACK && keyCode != KeyEvent.KEYCODE_ESCAPE
        && keyCode != KeyEvent.KEYCODE_MENU;
  }

  // OnClickListener methods

  @Override
  public void onClick(View view) {
    if (view == retryButton) {
      initPlayer();
    } else if (view.getParent() == debugRootView) {
      //trackSelectionHelper.showSelectionDialog(this, ((Button) view).getText(),
      //        player.getTrackInfo(), (int) view.getTag());
    }
  }


  @Override
  public void onLoadingChanged(boolean isLoading) {
    // Do nothing.
  }

  @Override
  public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
    if (playbackState == ExoPlayer.STATE_ENDED) {
      showControls();
    }
    updateButtonVisibilities();
  }

  @Override
  public void onTimelineChanged(Timeline timeline, Object manifest) {

  }

  @Override
  public void onPlayerError(ExoPlaybackException e) {
    String errorString = null;
    if (e.type == ExoPlaybackException.TYPE_RENDERER) {
      Exception cause = e.getRendererException();
      if (cause instanceof DecoderInitializationException) {
        // Special case for decoder initialization failures.
        DecoderInitializationException decoderInitializationException =
            (DecoderInitializationException) cause;
        if (decoderInitializationException.decoderName == null) {
          if (decoderInitializationException.getCause() instanceof DecoderQueryException) {
            errorString = getString(R.string.error_querying_decoders);
          } else if (decoderInitializationException.secureDecoderRequired) {
            errorString = getString(R.string.error_no_secure_decoder,
                decoderInitializationException.mimeType);
          } else {
            errorString = getString(R.string.error_no_decoder,
                decoderInitializationException.mimeType);
          }
        } else {
          errorString = getString(R.string.error_instantiating_decoder,
              decoderInitializationException.decoderName);
        }
      }
    }
    if (errorString != null) {
      showToast(errorString);
    }
    player.onError();
    updateButtonVisibilities();
    showControls();
  }

  @Override
  public void onPositionDiscontinuity() {

  }

  // SimpleExoPlayer.VideoListener implementation

  @Override
  public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
      float pixelWidthAspectRatio) {
  }

  @Override
  public void onRenderedFirstFrame() {

  }

  @Override
  public void onVideoTracksDisabled() {

  }


  // User controls

  public void updateButtonVisibilities() {
    debugRootView.removeAllViews();

    retryButton.setVisibility(player.isMediaNeddSource() ? View.VISIBLE : View.GONE);
    debugRootView.addView(retryButton);

    if (!player.hasPlayer()) {
      return;
    }
  }

  private void toggleControlsVisibility()  {

  }

  private void showControls() {
    debugRootView.setVisibility(View.VISIBLE);
  }

  public void showToast(int messageId) {
    showToast(getString(messageId));
  }

  public void showToast(String message) {
    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
  }

  @Override
  public void onCreatePlayer() {
    simpleExoPlayerView.setPlayer(player.getExoPlayer());
  }

  private void releasePlayer() {
    if (player.hasPlayer()) {
      player.realReleasePlayer();
      debugViewHelper.stop();
      debugViewHelper = null;
    }
  }

  @Override
  public Activity getContext() {
    return this;
  }

  @Override
  public void onVisibilityChange(int visibility) {
    debugRootView.setVisibility(visibility);
  }

  @Override
  public void onTrackSelectionsChanged(TrackSelections trackSelections) {

  }
}
