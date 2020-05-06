package am.voicerecordview;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class VoiceClass {

    private Context context;
    private VoiceCallback voiceCallback;

    private MediaPlayer player = null;
    private MediaRecorder recorder = null;
    private static File voiceFile = null;

    private final String LOG_TAG = getClass().getName();
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};


    public VoiceClass(Context context, VoiceCallback voiceCallback) {
        this.context = context;
        this.voiceCallback = voiceCallback;
    }


    //--------------------------------------------- Installation -----------------------------------


    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Log.e(LOG_TAG, "Permission Allowed");
            else
                Log.e(LOG_TAG, "Permission Denied");
        }
    }


    public void onStop() {
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }

        if (player != null) {
            player.release();
            player = null;
        }
    }


    //----------------------------------------------- Methods --------------------------------------

    private boolean isPermissionAccepted() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
            return false;
        } else
            return true;
    }


    public void startRecording() {
        if (isPermissionAccepted()) {
            voiceFile = getNewFile();
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setOutputFile(voiceFile.getPath());
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

            try {
                recorder.prepare();
            } catch (IOException e) {
                Log.e(LOG_TAG, "prepare() failed");
            }

            recorder.start();
        }
    }


    public void finishRecording() {
        if (isPermissionAccepted()) {
            try {
                recorder.stop();
                recorder.reset();
                recorder.release();
                recorder = null;

                if (voiceFile != null && voiceCallback != null)
                    voiceCallback.onFinishRecording(voiceFile);
            } catch (NullPointerException ignored) {
            }
        }
    }


    public void onCancel() {
        if (isPermissionAccepted()) {
            voiceFile.delete();
            player = null;
            Log.e(LOG_TAG, "onCancel");
        }
    }


    public void onLessThanSecond() {
        if (isPermissionAccepted()) {
            recorder.reset();
            recorder = null;
            voiceFile.delete();
            player = null;
        }
    }


    public void playLastVoice() {
        player = new MediaPlayer();
        try {
            player.setDataSource(voiceFile.getPath());
            player.prepare();
            player.start();

        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        } catch (NullPointerException e) {
            Log.e(LOG_TAG, e.getLocalizedMessage());
        }

        player.setOnCompletionListener(mp -> {
            player.release();
            player = null;
            Log.e(LOG_TAG, "player is Done");
        });
    }


    //------------------------------------------------ Utils ---------------------------------------

    private File getNewFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());

        return new File(context.getExternalCacheDir().getAbsolutePath()
                .concat("/" + timeStamp + ".mp3"));
    }


    public String getFileDuration(long milliseconds) {
        String finalTimerString = "";
        String secondsString;

        // Convert total duration into time
        int hours = (int) (milliseconds / (1000 * 60 * 60));
        int minutes = (int) (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
        int seconds = (int) ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);

        // Add hours if there
        if (hours > 0)
            finalTimerString = hours + ":";

        // Prepending 0 to seconds if it is one digit
        if (seconds < 10)
            secondsString = "0" + seconds;
        else
            secondsString = "" + seconds;

        finalTimerString = finalTimerString + minutes + ":" + secondsString;

        // return timer string
        return finalTimerString;
    }


    //---------------------------------------------- Interface -------------------------------------

    public interface VoiceCallback {
        void onFinishRecording(File voiceFile);
    }

}