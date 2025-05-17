package com.example.mecore;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;

public class CallActivity extends AppCompatActivity {
    private static final String TAG = "CallActivity";
    private static final String AGORA_APP_ID = "27ccf8c2bc4e47f28801043b1419217d";
    private static final int PERMISSION_REQ_CODE = 1001;

    private RtcEngine agoraEngine;
    private boolean isInCall = false;
    private boolean isMuted = false;
    private boolean isSpeakerOn = false;
    private String channelName;

    private TextView callStatusText;
    private ImageButton muteButton;
    private ImageButton speakerButton;
    private ImageButton endCallButton;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        channelName = getIntent().getStringExtra("channelName");
        String recipientName = getIntent().getStringExtra("recipientName");

        callStatusText = findViewById(R.id.callStatusText);
        muteButton = findViewById(R.id.muteButton);
        speakerButton = findViewById(R.id.speakerButton);
        endCallButton = findViewById(R.id.endCallButton);

        if (recipientName != null) {
            callStatusText.setText("Calling " + recipientName + "...");
        }

        if (!checkPermissions()) {
            requestPermissions();
        } else {
            setupVoiceCalling();
            startVoiceCall();
        }

        muteButton.setOnClickListener(v -> toggleMute());
        speakerButton.setOnClickListener(v -> toggleSpeaker());
        endCallButton.setOnClickListener(v -> endCall());
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, PERMISSION_REQ_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupVoiceCalling();
                startVoiceCall();
            } else {
                Toast.makeText(this, "Microphone permission is required for voice calling", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void setupVoiceCalling() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getApplicationContext();
            config.mAppId = AGORA_APP_ID;
            config.mEventHandler = new IRtcEngineEventHandler() {
                @Override
                public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                    Log.d(TAG, "onJoinChannelSuccess: Joined channel " + channel + " with uid " + uid);
                    isInCall = true;
                    runOnUiThread(() -> {
                        callStatusText.setText("Connected");
                        Toast.makeText(CallActivity.this, "Call started", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onUserJoined(int uid, int elapsed) {
                    Log.d(TAG, "onUserJoined: User " + uid + " joined the call");
                    runOnUiThread(() -> Toast.makeText(CallActivity.this, "User joined the call", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onUserOffline(int uid, int reason) {
                    Log.d(TAG, "onUserOffline: User " + uid + " left the call, reason: " + reason);
                    runOnUiThread(() -> {
                        Toast.makeText(CallActivity.this, "User left the call", Toast.LENGTH_SHORT).show();
                        endCall();
                    });
                }

                @Override
                public void onLeaveChannel(RtcStats stats) {
                    Log.d(TAG, "onLeaveChannel: Left channel");
                    isInCall = false;
                    runOnUiThread(() -> {
                        Toast.makeText(CallActivity.this, "Call ended", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }

                @Override
                public void onError(int err) {
                    Log.e(TAG, "onError: Agora error " + err);
                    runOnUiThread(() -> {
                        Toast.makeText(CallActivity.this, "Call error: " + err, Toast.LENGTH_LONG).show();
                        finish();
                    });
                }
            };
            agoraEngine = RtcEngine.create(config);
            agoraEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION);
            agoraEngine.enableAudio();
        } catch (Exception e) {
            Log.e(TAG, "setupVoiceCalling: Failed to initialize Agora: " + e.getMessage(), e);
            Toast.makeText(this, "Failed to initialize voice calling: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void startVoiceCall() {
        if (agoraEngine == null) {
            Toast.makeText(this, "Voice calling not initialized", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String token = null; // No token since App Certificate is disabled
        int uid = 0; // Let Agora assign a UID automatically

        agoraEngine.joinChannel(token, channelName, "", uid);
        Log.d(TAG, "startVoiceCall: Joining channel " + channelName);
    }

    private void toggleMute() {
        if (agoraEngine != null) {
            isMuted = !isMuted;
            agoraEngine.muteLocalAudioStream(isMuted);
            try {
                muteButton.setImageResource(isMuted ? R.drawable.ic_mute_on : R.drawable.ic_mute_off);
            } catch (Exception e) {
                Log.e(TAG, "Failed to set mute button drawable: " + e.getMessage(), e);
                // Fallback to a default drawable or disable the button
                muteButton.setImageResource(android.R.drawable.ic_menu_info_details);
            }
            Toast.makeText(this, isMuted ? "Microphone muted" : "Microphone unmuted", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleSpeaker() {
        if (agoraEngine != null) {
            isSpeakerOn = !isSpeakerOn;
            agoraEngine.setEnableSpeakerphone(isSpeakerOn);
            try {
                speakerButton.setImageResource(isSpeakerOn ? R.drawable.ic_speaker_on : R.drawable.ic_speaker_off);
            } catch (Exception e) {
                Log.e(TAG, "Failed to set speaker button drawable: " + e.getMessage(), e);
                // Fallback to a default drawable or disable the button
                speakerButton.setImageResource(android.R.drawable.ic_menu_info_details);
            }
            Toast.makeText(this, isSpeakerOn ? "Speaker on" : "Speaker off", Toast.LENGTH_SHORT).show();
        }
    }

    private void endCall() {
        if (agoraEngine != null && isInCall) {
            agoraEngine.leaveChannel();
            Log.d(TAG, "endCall: Left channel");
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (agoraEngine != null) {
            agoraEngine.leaveChannel();
            RtcEngine.destroy();
            agoraEngine = null;
        }
    }
}