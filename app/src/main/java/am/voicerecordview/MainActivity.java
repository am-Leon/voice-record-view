package am.voicerecordview;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity implements VoiceRecordView.RecordingListener {

    VoiceRecordView recordView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recordView = new VoiceRecordView();
        recordView.viewInit(findViewById(R.id.layout_main));
        // this is to provide the container layout to the audio record view..
        View containerView = recordView.setContainerView(R.layout.layout_chatting);
        recordView.setRecordingListener(this);


    }

    @Override
    public void onRecordingStarted() {

    }

    @Override
    public void onRecordingLocked() {

    }

    @Override
    public void onRecordingCompleted() {

    }

    @Override
    public void onRecordingCanceled() {

    }
}
