package am.voicerecordview;

import android.animation.Animator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.cardview.widget.CardView;

import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

public class VoiceRecordView implements TextWatcher, View.OnTouchListener {

    public enum UserBehaviour {
        CANCELING,
        LOCKING,
        NONE
    }

    public enum RecordingBehaviour {
        CANCELED,
        LOCKED,
        LOCK_DONE,
        RELEASED
    }

    public interface RecordingListener {
        void onRecordingStarted();

        void onRecordingLocked();

        void onRecordingCompleted();

        void onRecordingCanceled();

    }


    private final String TAG = this.getClass().getName();

    private Context context;
    private TextInputEditText input_message;
    private CardView card_message, card_lock;
    private View layoutEffect1, layoutEffect2;
    private AppCompatTextView txt_slide, txt_time;
    private FrameLayout frameContainer, layout_controls;
    private LinearLayout layout_slideCancel, layout_dustin;
    private AppCompatImageButton btn_camera_pick, btn_voice, btn_stop, btn_send;
    private AppCompatImageView img_arrowSlide, img_mic, img_dustinCover, img_dustin, img_lock, img_lockArrow;

    private Animation animBlink, animJump, animJumpFast;
    private boolean isDeleting;
    private boolean stopTrackingAction;
    private Handler handler;

    private int audioTotalTime;
    private TimerTask timerTask;
    private Timer audioTimer;
    private SimpleDateFormat timeFormatter;

    private float lastX, lastY;
    private float firstX, firstY;

    private float directionOffset, cancelOffset, lockOffset;
    private float dp = 0;
    private boolean isLocked = false;
    private boolean showCameraIcon = true;

    private UserBehaviour userBehaviour = UserBehaviour.NONE;
    private RecordingListener recordingListener;
    private int screenWidth, screenHeight;
    private boolean isLayoutDirectionRightToLeft = false;


    public VoiceRecordView() {
    }

    //---------------------------------------- Getters&Setters -------------------------------------

    public RecordingListener getRecordingListener() {
        return recordingListener;
    }

    public void setRecordingListener(RecordingListener recordingListener) {
        this.recordingListener = recordingListener;
    }

    public TextInputEditText getMessageView() {
        return input_message;
    }

    public CardView getMessageCard() {
        return card_message;
    }

    public void setLayoutDirectionRightToLeft(boolean layoutDirectionRightToLeft) {
        isLayoutDirectionRightToLeft = layoutDirectionRightToLeft;
    }

    public void setSlideText(int textRes) {
        this.txt_slide.setText(textRes);
    }

    public void setVoiceRecordRes(int imgRes) {
        this.btn_voice.setImageResource(imgRes);
    }

    public void setStopRecordRes(int imgRes) {
        this.btn_stop.setImageResource(imgRes);
    }

    public void setSendButtonRes(int imgRes) {
        this.btn_send.setImageResource(imgRes);
    }

    public AppCompatImageButton getSendButton() {
        return btn_send;
    }

    public AppCompatImageButton getCameraButton() {
        return btn_camera_pick;
    }

    public boolean isShowCameraIcon() {
        return showCameraIcon;
    }

    public void showCameraIcon(boolean showCameraIcon) {
        this.showCameraIcon = showCameraIcon;
        btn_camera_pick.setVisibility(showCameraIcon ? View.VISIBLE : View.GONE);
    }

    //---------------------------------------- ViewInit -------------------------------------

    public void viewInit(ViewGroup view) {
        if (view == null) {
            showErrorLog("initView ViewGroup can't be NULL");
            return;
        }

        context = view.getContext();

        view.removeAllViews();
        view.addView(LayoutInflater.from(view.getContext()).inflate(R.layout.voice_record_view, null));

        frameContainer = view.findViewById(R.id.frameContainer);
        layout_controls = view.findViewById(R.id.layout_controls);
        layout_slideCancel = view.findViewById(R.id.layout_slideCancel);
        layout_dustin = view.findViewById(R.id.layout_dustin);
        txt_slide = view.findViewById(R.id.txt_slide);
        txt_time = view.findViewById(R.id.txt_time);
        card_message = view.findViewById(R.id.card_message);
        card_lock = view.findViewById(R.id.card_lock);
        img_arrowSlide = view.findViewById(R.id.img_arrowSlide);
        img_mic = view.findViewById(R.id.img_mic);
        img_lock = view.findViewById(R.id.img_lock);
        img_dustin = view.findViewById(R.id.img_dustin);
        img_dustinCover = view.findViewById(R.id.img_dustinCover);
        img_lockArrow = view.findViewById(R.id.img_lockArrow);
        input_message = view.findViewById(R.id.input_message);
        btn_camera_pick = view.findViewById(R.id.btn_camera_pick);
        btn_voice = view.findViewById(R.id.btn_voice);
        btn_stop = view.findViewById(R.id.btn_stop);
        btn_send = view.findViewById(R.id.btn_send);
        layoutEffect1 = view.findViewById(R.id.layoutEffect1);
        layoutEffect2 = view.findViewById(R.id.layoutEffect2);
        input_message.addTextChangedListener(this);

        handler = new Handler(Looper.getMainLooper());

        timeFormatter = new SimpleDateFormat("m:ss", Locale.getDefault());

        DisplayMetrics displayMetrics = view.getContext().getResources().getDisplayMetrics();
        screenHeight = displayMetrics.heightPixels;
        screenWidth = displayMetrics.widthPixels;

        dp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, view.getContext().getResources().getDisplayMetrics());

        animBlink = AnimationUtils.loadAnimation(view.getContext(),
                R.anim.blink);
        animJump = AnimationUtils.loadAnimation(view.getContext(),
                R.anim.jump);
        animJumpFast = AnimationUtils.loadAnimation(view.getContext(),
                R.anim.jump_fast);

        setupRecording();
    }


    public View setContainerView(int layoutResourceID) {
        View view = LayoutInflater.from(context).inflate(layoutResourceID, null);

        if (view == null) {
            showErrorLog("Unable to create the Container View from the layoutResourceID");
            return null;
        }

        frameContainer.removeAllViews();
        frameContainer.addView(view);
        return view;
    }


    private void setupRecording() {
        btn_send.animate().scaleX(0f).scaleY(0f).setDuration(100).setInterpolator(new LinearInterpolator()).start();

        btn_voice.setOnTouchListener(this);

        btn_stop.setOnClickListener(v -> {
            isLocked = false;
            stopRecording(RecordingBehaviour.LOCK_DONE);
        });

    }


    private void translateY(float y) {
        if (y < -lockOffset) {
            locked();
            btn_voice.setTranslationY(0);
            return;
        }

        if (card_lock.getVisibility() != View.VISIBLE) {
            card_lock.setVisibility(View.VISIBLE);
        }

        btn_voice.setTranslationY(y);
        card_lock.setTranslationY(y / 2);
        btn_voice.setTranslationX(0);
    }


    private void translateX(float x) {
        if (isLayoutDirectionRightToLeft ? x > cancelOffset : x < -cancelOffset) {
            canceled();
            btn_voice.setTranslationX(0);
            layout_slideCancel.setTranslationX(0);
            return;
        }

        btn_voice.setTranslationX(x);
        layout_slideCancel.setTranslationX(x);
        card_lock.setTranslationY(0);
        btn_voice.setTranslationY(0);

        if (Math.abs(x) < img_mic.getWidth() / 2) {
            if (card_lock.getVisibility() != View.VISIBLE) {
                card_lock.setVisibility(View.VISIBLE);
            }
        } else {
            if (card_lock.getVisibility() != View.GONE) {
                card_lock.setVisibility(View.GONE);
            }
        }
    }


    private void locked() {
        stopTrackingAction = true;
        stopRecording(RecordingBehaviour.LOCKED);
        isLocked = true;
    }


    private void canceled() {
        stopTrackingAction = true;
        stopRecording(RecordingBehaviour.CANCELED);
    }


    private void stopRecording(RecordingBehaviour recordingBehaviour) {
        stopTrackingAction = true;
        firstX = 0;
        firstY = 0;
        lastX = 0;
        lastY = 0;

        userBehaviour = UserBehaviour.NONE;

        btn_voice.animate().scaleX(1f).scaleY(1f).translationX(0).translationY(0).setDuration(100).setInterpolator(new LinearInterpolator()).start();
        layout_slideCancel.setTranslationX(0);
        layout_slideCancel.setVisibility(View.GONE);

        card_lock.setVisibility(View.GONE);
        card_lock.setTranslationY(0);
        img_lockArrow.clearAnimation();
        img_lock.clearAnimation();

        if (isLocked) {
            return;
        }

        switch (recordingBehaviour) {
            case LOCKED:
                btn_stop.setVisibility(View.VISIBLE);

                if (recordingListener != null)
                    recordingListener.onRecordingLocked();

                break;

            case CANCELED:
                txt_time.clearAnimation();
                txt_time.setVisibility(View.INVISIBLE);
                img_mic.setVisibility(View.INVISIBLE);
                btn_stop.setVisibility(View.GONE);
                layoutEffect2.setVisibility(View.GONE);
                layoutEffect1.setVisibility(View.GONE);

                timerTask.cancel();
                delete();

                if (recordingListener != null)
                    recordingListener.onRecordingCanceled();

                break;

            case RELEASED:
            case LOCK_DONE:
                txt_time.clearAnimation();
                txt_time.setVisibility(View.INVISIBLE);
                img_mic.setVisibility(View.INVISIBLE);
                input_message.setVisibility(View.VISIBLE);
                if (showCameraIcon) {
                    btn_camera_pick.setVisibility(View.VISIBLE);
                }
                btn_stop.setVisibility(View.GONE);
                input_message.requestFocus();
                layoutEffect2.setVisibility(View.GONE);
                layoutEffect1.setVisibility(View.GONE);

                timerTask.cancel();

                if (recordingListener != null)
                    recordingListener.onRecordingCompleted();
                break;
        }

    }


    private void startRecord() {
        if (recordingListener != null)
            recordingListener.onRecordingStarted();

        stopTrackingAction = false;
        input_message.setVisibility(View.INVISIBLE);
        btn_camera_pick.setVisibility(View.INVISIBLE);
        btn_voice.animate().scaleXBy(1f).scaleYBy(1f).setDuration(200).setInterpolator(new OvershootInterpolator()).start();
        txt_time.setVisibility(View.VISIBLE);
        card_lock.setVisibility(View.VISIBLE);
        layout_slideCancel.setVisibility(View.VISIBLE);
        img_mic.setVisibility(View.VISIBLE);
        layoutEffect2.setVisibility(View.VISIBLE);
        layoutEffect1.setVisibility(View.VISIBLE);

        txt_time.startAnimation(animBlink);
        img_lockArrow.clearAnimation();
        img_lock.clearAnimation();
        img_lockArrow.startAnimation(animJumpFast);
        img_lock.startAnimation(animJump);

        if (audioTimer == null) {
            audioTimer = new Timer();
            timeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        timerTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(() -> {
                    txt_time.setText(timeFormatter.format(new Date(audioTotalTime * 1000)));
                    audioTotalTime++;
                });
            }
        };

        audioTotalTime = 0;
        audioTimer.schedule(timerTask, 0, 1000);
    }


    private void delete() {
        img_mic.setVisibility(View.VISIBLE);
        img_mic.setRotation(0);
        isDeleting = true;
        btn_voice.setEnabled(false);

        handler.postDelayed(() -> {
            isDeleting = false;
            btn_voice.setEnabled(true);

            if (showCameraIcon) {
                btn_camera_pick.setVisibility(View.VISIBLE);
            }
        }, 1250);

        img_mic.animate().translationY(-dp * 150).rotation(180).scaleXBy(0.6f).scaleYBy(0.6f).setDuration(500).setInterpolator(new DecelerateInterpolator()).setListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
                float displacement = 0;

                if (isLayoutDirectionRightToLeft) {
                    displacement = dp * 40;
                } else {
                    displacement = -dp * 40;
                }

                img_dustin.setTranslationX(displacement);
                img_dustinCover.setTranslationX(displacement);

                img_dustinCover.animate().translationX(0).rotation(-120).setDuration(350).setInterpolator(new DecelerateInterpolator()).start();

                img_dustin.animate().translationX(0).setDuration(350).setInterpolator(new DecelerateInterpolator()).setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        img_dustin.setVisibility(View.VISIBLE);
                        img_dustinCover.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }
                }).start();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                img_mic.animate().translationY(0).scaleX(1).scaleY(1).setDuration(350).setInterpolator(new LinearInterpolator()).setListener(
                        new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animation) {
                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                img_mic.setVisibility(View.INVISIBLE);
                                img_mic.setRotation(0);

                                float displacement = 0;

                                if (isLayoutDirectionRightToLeft) {
                                    displacement = dp * 40;
                                } else {
                                    displacement = -dp * 40;
                                }

                                img_dustinCover.animate().rotation(0).setDuration(150).setStartDelay(50).start();
                                img_dustin.animate().translationX(displacement).setDuration(200).setStartDelay(250).setInterpolator(new DecelerateInterpolator()).start();
                                img_dustinCover.animate().translationX(displacement).setDuration(200).setStartDelay(250).setInterpolator(new DecelerateInterpolator()).setListener(new Animator.AnimatorListener() {
                                    @Override
                                    public void onAnimationStart(Animator animation) {
                                    }

                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        input_message.setVisibility(View.VISIBLE);
                                        input_message.requestFocus();
                                    }

                                    @Override
                                    public void onAnimationCancel(Animator animation) {
                                    }

                                    @Override
                                    public void onAnimationRepeat(Animator animation) {
                                    }
                                }).start();
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {
                            }

                            @Override
                            public void onAnimationRepeat(Animator animation) {
                            }
                        }
                ).start();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        }).start();
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (isDeleting)
            return true;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                cancelOffset = (float) (screenWidth / 2.8);
                lockOffset = (float) (screenWidth / 2.5);

                if (firstX == 0) {
                    firstX = event.getRawX();
                }

                if (firstY == 0) {
                    firstY = event.getRawY();
                }
                startRecord();
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    stopRecording(RecordingBehaviour.RELEASED);
                }
                break;

            case MotionEvent.ACTION_MOVE:

                if (stopTrackingAction) {
                    return true;
                }

                UserBehaviour direction = UserBehaviour.NONE;

                float motionX = Math.abs(firstX - event.getRawX());
                float motionY = Math.abs(firstY - event.getRawY());

                if (isLayoutDirectionRightToLeft ? (motionX > directionOffset && lastX > firstX && lastY > firstY) : (motionX > directionOffset && lastX < firstX && lastY < firstY)) {

                    if (isLayoutDirectionRightToLeft ? (motionX > motionY && lastX > firstX) : (motionX > motionY && lastX < firstX)) {
                        direction = UserBehaviour.CANCELING;

                    } else if (motionY > motionX && lastY < firstY) {
                        direction = UserBehaviour.LOCKING;
                    }

                } else if (isLayoutDirectionRightToLeft ? (motionX > motionY && motionX > directionOffset && lastX > firstX) : (motionX > motionY && motionX > directionOffset && lastX < firstX)) {
                    direction = UserBehaviour.CANCELING;
                } else if (motionY > motionX && motionY > directionOffset && lastY < firstY) {
                    direction = UserBehaviour.LOCKING;
                }

                if (direction == UserBehaviour.CANCELING) {
                    if (userBehaviour == UserBehaviour.NONE || event.getRawY() + btn_voice.getWidth() / 2 > firstY) {
                        userBehaviour = UserBehaviour.CANCELING;
                    }

                    if (userBehaviour == UserBehaviour.CANCELING) {
                        translateX(-(firstX - event.getRawX()));
                    }
                } else if (direction == UserBehaviour.LOCKING) {
                    if (userBehaviour == UserBehaviour.NONE || event.getRawX() + btn_voice.getWidth() / 2 > firstX) {
                        userBehaviour = UserBehaviour.LOCKING;
                    }

                    if (userBehaviour == UserBehaviour.LOCKING) {
                        translateY(-(firstY - event.getRawY()));
                    }
                }

                lastX = event.getRawX();
                lastY = event.getRawY();
                break;
        }

        v.onTouchEvent(event);
        return true;
    }


    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (s.toString().trim().isEmpty()) {
            if (btn_send.getVisibility() != View.GONE) {
                btn_send.setVisibility(View.GONE);
                btn_send.animate().scaleX(0f).scaleY(0f).setDuration(100).setInterpolator(new LinearInterpolator()).start();
            }

            if (showCameraIcon) {
                if (btn_camera_pick.getVisibility() != View.VISIBLE && !isLocked) {
                    btn_camera_pick.setVisibility(View.VISIBLE);
                    btn_camera_pick.animate().scaleX(1f).scaleY(1f).setDuration(100).setInterpolator(new LinearInterpolator()).start();
                }
            }

        } else {
            if (btn_send.getVisibility() != View.VISIBLE && !isLocked) {
                btn_send.setVisibility(View.VISIBLE);
                btn_send.animate().scaleX(1f).scaleY(1f).setDuration(100).setInterpolator(new LinearInterpolator()).start();
            }

            if (showCameraIcon) {
                if (btn_camera_pick.getVisibility() != View.GONE) {
                    btn_camera_pick.setVisibility(View.GONE);
                    btn_camera_pick.animate().scaleX(0f).scaleY(0f).setDuration(100).setInterpolator(new LinearInterpolator()).start();
                }
            }
        }
    }


    private void showErrorLog(String s) {
        Log.e(TAG, s);
    }

}