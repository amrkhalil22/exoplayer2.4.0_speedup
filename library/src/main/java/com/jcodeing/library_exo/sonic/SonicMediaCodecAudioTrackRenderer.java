//MIT License
//
//Copyright (c) 2016 Jcodeing <jcodeing@gmail.com>
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to deal
//in the Software without restriction, including without limitation the rights
//to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in all
//copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//SOFTWARE.

package com.jcodeing.library_exo.sonic;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;

import java.nio.ByteBuffer;

public final class SonicMediaCodecAudioTrackRenderer extends MediaCodecAudioRenderer {

    private Sonic sonic;
    private float speed;
    private byte[] inBuffer;
    private byte[] outBuffer;
    private ByteBuffer bufferSonicOut;

    private int bufferIndex;

    public SonicMediaCodecAudioTrackRenderer(MediaCodecSelector mediaCodecSelector, DrmSessionManager<FrameworkMediaCrypto> drmSessionManager, boolean playClearSamplesWithoutKeys, Handler eventHandler, AudioRendererEventListener eventListener, AudioCapabilities audioCapabilities) {
        super(mediaCodecSelector, drmSessionManager, playClearSamplesWithoutKeys, eventHandler, eventListener, audioCapabilities);
        bufferIndex = -1;
        speed = 1.0f;
    }

    // ------------------------------K------------------------------@Override

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected final void onOutputFormatChanged(MediaCodec codec, MediaFormat outputFormat) throws ExoPlaybackException {
        super.onOutputFormatChanged(codec, outputFormat);
        int sampleRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channelCount = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

        int bufferSize = channelCount * 4096;//1024*4 4M 22.05 kHz/4096
        inBuffer = new byte[bufferSize];
        outBuffer = new byte[bufferSize];

        sonic = new Sonic(sampleRate, channelCount);
        bufferSonicOut = ByteBuffer.wrap(outBuffer, 0, 0);
        setSpeed(speed);
    }

    @Override
    protected boolean processOutputBuffer(long positionUs, long elapsedRealtimeUs, MediaCodec codec, ByteBuffer buffer, int bufferIndex, int bufferFlags, long bufferPresentationTimeUs, boolean shouldSkip) throws ExoPlaybackException {
        if (bufferIndex == this.bufferIndex) {//bufferIndex: 0 ~ 14 / 0 ~ 3
            return super.processOutputBuffer(positionUs, elapsedRealtimeUs, codec, bufferSonicOut, bufferIndex, bufferFlags, bufferPresentationTimeUs, shouldSkip);
        } else {
            int sizeSonic;
            this.bufferIndex = bufferIndex;
            sizeSonic = buffer.remaining();
            buffer.get(inBuffer, 0, sizeSonic);
            sonic.writeBytesToStream(inBuffer, sizeSonic);
            sizeSonic = sonic.readBytesFromStream(outBuffer, outBuffer.length);
            bufferSonicOut.position(0);
            bufferSonicOut.limit(sizeSonic);
            return super.processOutputBuffer(positionUs, elapsedRealtimeUs, codec, bufferSonicOut, bufferIndex, bufferFlags, bufferPresentationTimeUs, shouldSkip);
        }
    }

    // ------------------------------K------------------------------@Speed

    public final void setSonicSpeed(float speed) {
        synchronized (this) {
            try {
                this.speed = speed;
                if (sonic != null) {
                    sonic.setSpeed(speed);
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }

    public float getSonicSpeed() {
        return sonic.getSpeed();
    }

    public final void setSonicPitch(float pitch) {
        synchronized (this) {
            try {
                if (sonic != null) {
                    sonic.setPitch(pitch);
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }


    public final void setSonicRate(float rate) {
        synchronized (this) {
            try {
                if (sonic != null) {
                    sonic.setRate(rate);
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }

    @Override
    public void setPlaybackSpeed(float speed) {
        setSpeed(speed);
        super.setPlaybackSpeed(speed);
    }

    private void setSpeed(float speed) {
        this.speed = speed;
        setSonicSpeed(speed);
        setSonicPitch(1);
        setSonicRate(1);
    }

}

