package com.destroyer.rubikcubetimer;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

import java.util.Arrays;

public class ExternalPadAdapter {

    private static final int bufferSize = 320;
    private static final double RIGHT_TONE_FREQ = 880; // hz
    private static final double LEFT_TONE_FREQ = 220; // hz
    private static final int TONE_SAMPLE_RATE = 8000; // hz
    private static final int RECORDER_SAMPLE_RATE = 44100; // hz
    private static final int RECORDER_BUFFER_SIZE = AudioRecord.getMinBufferSize(RECORDER_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT); // bytes

    private boolean isRunning;
    private byte[] buffer = new byte[bufferSize];

    private Context context;
    private AudioTrack track;

    private AudioRecord recorder;

    public ExternalPadAdapter(Context context) {
        this.context = context;
//        new Thread(new Runnable() {
//            public void run() {
//                generateTone();
//            }
//        }).start();
//
//        track = new AudioTrack(AudioManager.STREAM_MUSIC,
//                TONE_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO,
//                AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
//                AudioTrack.MODE_STATIC);
//
//        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDER_SAMPLE_RATE,
//                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, RECORDER_BUFFER_SIZE);
    }

    private void startListener() {
        /** ======= Initialize AudioRecord and AudioTrack ======== **/
        track = findAudioTrack(track);
        if (track == null) {
            Log.wtf("EPA", "======== findAudioTrack : Returned Error! ========== ");
            return;
        }

        recorder = findAudioRecord();
        if (recorder == null) {
            Log.wtf("EPA", "======== findAudioRecord : Returned Error! =========== ");
            return;
        }

        if ((AudioRecord.STATE_INITIALIZED == recorder.getState()) &&
                (AudioTrack.STATE_INITIALIZED == track.getState()))
        {
            recorder.startRecording();
            Log.d("EPA", "========= Recorder Started... =========");

            byte[] generatedSnd = generateTone(track.getSampleRate());

            track.write(generatedSnd, 0, generatedSnd.length);
            track.setLoopPoints(0, generatedSnd.length / 4, -1);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                track.setVolume(0.50f);
            } else {
                track.setStereoVolume(0.50f, 0.50f);
            }
            track.play();
            Log.d("EPA", "========= Track Started... =========");
        }
        else
        {
            Log.wtf("EPA", "==== Initilazation failed for AudioRecord or AudioTrack =====");
            return;
        }

        while (isRunning)
        {
            recorder.read(buffer, 0, bufferSize);
        }
        Log.d("EPA", "Loopback exit");
    }

    public void start() {
        isRunning = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                startListener();
            }
        }).start();
    }

    public void stop() {
        isRunning = false;
        if (track != null && track.getState() == AudioTrack.STATE_INITIALIZED){
            track.stop();
            track.release();
        }
        if (recorder != null && recorder.getState() == AudioRecord.STATE_INITIALIZED){
            recorder.stop();
            recorder.release();
        }
    }

    public void logReadBuffer() {
        Log.wtf("MICROPHONE_BUFFER", Arrays.toString(buffer));
    }

    private void generateTone() {
        this.generateTone(TONE_SAMPLE_RATE);
    }

    private byte[] generateTone(int sampleRate){
        final double sample[] = new double[sampleRate];
        final byte generatedSnd[] = new byte[2 * sampleRate];

        for (int i = 0; i < sampleRate; ++i) {
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate /LEFT_TONE_FREQ));
            sample[++i] = Math.sin(2 * Math.PI * i / (sampleRate /RIGHT_TONE_FREQ));
        }

        int idx = 0;
        for (final double dVal : sample) {
            final short val = (short) ((dVal * 32767));
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }
        return generatedSnd;
    }

    private AudioTrack findAudioTrack(AudioTrack track) {
        for (int sampleRate : new int[] {8000, 11025, 22050, 44100}) {
            try {
                int bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);

                if (bufferSize != AudioTrack.ERROR_BAD_VALUE) {
                    track = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_IN_STEREO,
                            AudioFormat.ENCODING_PCM_16BIT, bufferSize*2, AudioTrack.MODE_STATIC);

                    if (track.getState() == AudioTrack.STATE_INITIALIZED) {
                        Log.d("FIND_TRACK", "Successful rate " + sampleRate + "Hz, bits: " + AudioFormat.ENCODING_PCM_16BIT +
                                ", channel: " + AudioFormat.CHANNEL_IN_STEREO);
                        return track;
                    } else Log.d("adfasfd", "adfasdf");
                } else Log.d("qwerqwerqwer", "qweqwerweqr");
            } catch (Exception e) {
                Log.e("FIND_TRACK", sampleRate + ":" + AudioFormat.ENCODING_PCM_16BIT + ":" + AudioFormat.CHANNEL_IN_STEREO + "\t" + "Exception, keep trying.",e);
            }
        }
        return null;
    }

    private AudioRecord findAudioRecord() {
        for (int sampleRate : new int[] {8000, 11025, 22050, 44100}) {
            for (short audioFormat : new short[] {AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_PCM_8BIT}) {
                for (short channelConfig : new short[] {AudioFormat.CHANNEL_IN_STEREO, AudioFormat.CHANNEL_IN_MONO}) {
                    try {
                        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                            // check if we can instantiate and have a success
                            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate,
                                    channelConfig, audioFormat, bufferSize);

                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                                Log.d("FIND_RECORDER", "Successful rate " + sampleRate + "Hz, bits: " + audioFormat +
                                        ", channel: " + channelConfig);
                                return recorder;
                            }
                        }
                    } catch (Exception e) {
                        Log.e("FIND_RECORDER", sampleRate + ":" + audioFormat + ":" + channelConfig + "\t" + "Exception, keep trying.",e);
                    }
                }
            }
        }
        return null;
    }
}
