package am.voicerecordview;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;

public class MainActivity extends AppCompatActivity implements VoiceRecordView.RecordingListener, VoiceClass.VoiceCallback {

    private VoiceClass voiceClass;
    private VoiceRecordView recordView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        voiceClass = new VoiceClass(this, this);

        recordView = new VoiceRecordView();
        recordView.viewInit(findViewById(R.id.layout_main));
        // this is to provide the container layout to the audio record view..
        View containerView = recordView.setContainerView(R.layout.layout_chatting);
        recordView.setRecordingListener(this);
        recordView.showCameraIcon(false);
//        recordView.setMaxVoiceDuration(20);

        recordView.getCameraButton().setOnClickListener(v -> Toast.makeText(MainActivity.this, "Camera Icon Clicked", Toast.LENGTH_SHORT).show());

        recordView.getSendButton().setOnClickListener(v -> {
            String msg = recordView.getMessageView().getText().toString().trim();
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

    }

    @Override
    public void onRecordingStarted() {
        voiceClass.startRecording();
    }

    @Override
    public void onLessThanSecond() {
        voiceClass.onLessThanSecond();
    }

    @Override
    public void onRecordingLocked() {
    }

    @Override
    public void onRecordingCompleted() {
        voiceClass.finishRecording();

//        Snackbar.make(findViewById(android.R.id.content), "Play Last Voice ?", BaseTransientBottomBar.LENGTH_INDEFINITE)
//                .setAction("Play", v -> voiceClass.playLastVoice()).show();
    }

    @Override
    public void onRecordingCanceled() {
        voiceClass.onCancel();
    }

    @Override
    public void onFinishRecording(File voiceFile) {
        Log.e("voiceFile ", voiceFile.getPath());
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        voiceClass.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @Override
    protected void onStop() {
        super.onStop();
        voiceClass.onStop();
    }

}
