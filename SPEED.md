# About speed
We add a method at [ExoPlayer.java](https://github.com/yangwuan55/ExoPlayer/blob/playbackSpeed/library/src/main/java/com/google/android/exoplayer2/ExoPlayer.java)

```java

  /**
   * @param speed the speed factor: speed_of_playback / speed_of_real_clock
   */
  void setPlaybackSpeed(float speed);

```

just use the [SimpleExoPlayer](https://github.com/yangwuan55/ExoPlayer/blob/playbackSpeed/library/src/main/java/com/google/android/exoplayer2/SimpleExoPlayer.java).

Or you can run the [demo](https://github.com/yangwuan55/ExoPlayer/tree/playbackSpeed/demo_speed).

Hope useful to you.

#Thanks
[BigPeach](https://github.com/BigPeach)

[K Sun](https://github.com/jcodeing)