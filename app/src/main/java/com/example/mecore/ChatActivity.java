package com.example.mecore;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";
    private String chatId;
    private List<Message> messageList;
    private ChatAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration messageListener;
    private String recipientFcmToken;

    private EditText messageInput;
    private RecyclerView recyclerView;
    private ImageButton callButton;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        recyclerView = findViewById(R.id.chat_recycler_view);
        messageList = new ArrayList<>();
        adapter = new ChatAdapter(messageList, getIntent().getStringExtra("otherUsername"), this::showMessageOptions);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setReverseLayout(false);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() == null) {
            auth.signInAnonymously()
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            initializeChat();
                        } else {
                            Toast.makeText(this, "Failed to sign in anonymously", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    });
        } else {
            initializeChat();
        }
    }

    @SuppressLint("SetTextI18n")
    private void initializeChat() {
        String recipientId = getIntent().getStringExtra("recipientId");
        String otherUsername = getIntent().getStringExtra("otherUsername");
        String currentUsername = getIntent().getStringExtra("currentUsername");

        if (recipientId == null) {
            finish();
            return;
        }

        Log.d(TAG, "Fetching FCM token for recipient: " + recipientId);
        db.collection("users").document(recipientId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        recipientFcmToken = documentSnapshot.getString("fcmToken");
                        if (recipientFcmToken == null) {
                            Log.w(TAG, "Recipient FCM token is null for user: " + recipientId);
                        } else {
                            Log.d(TAG, "Recipient FCM token fetched: " + recipientFcmToken);
                        }
                    } else {
                        Log.w(TAG, "Recipient document not found for user: " + recipientId);
                    }
                    setupUI();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch recipient FCM token: " + e.getMessage());
                    setupUI();
                });
    }

    @SuppressLint("SetTextI18n")
    private void setupUI() {
        String currentUserId = Objects.requireNonNull(auth.getCurrentUser()).getUid();
        chatId = createChatId(currentUserId, getIntent().getStringExtra("recipientId"));

        messageInput = findViewById(R.id.messageEditText);
        Button sendMessageBtn = findViewById(R.id.sendButton);
        ImageButton goBackButton = findViewById(R.id.goBackButton);
        TextView otherUsernameTextView = findViewById(R.id.chatTitle);
        callButton = findViewById(R.id.callButton);

        String otherUsername = getIntent().getStringExtra("otherUsername");
        String recipientId = getIntent().getStringExtra("recipientId");
        String source = getIntent().getStringExtra("source");

        adapter = new ChatAdapter(messageList, otherUsername, this::showMessageOptions);
        recyclerView.setAdapter(adapter);

        if (otherUsernameTextView == null) {
            Log.e(TAG, "chatTitle TextView not found in layout. Check activity_chat.xml for ID 'chatTitle'.");
        } else if (otherUsername != null) {
            otherUsernameTextView.setText("Chat with " + otherUsername);
        } else {
            otherUsernameTextView.setText("Chat");
        }

        if (goBackButton == null) {
            Log.e(TAG, "goBackButton ImageButton not found in layout. Check activity_chat.xml for ID 'goBackButton'.");
        } else {
            goBackButton.setOnClickListener(v -> {
                Intent intent;
                Log.d(TAG, "goBackButton clicked. Source: " + source);
                if ("ChatListActivity".equals(source)) {
                    Log.d(TAG, "Navigating to ChatListActivity");
                    intent = new Intent(ChatActivity.this, ChatListActivity.class);
                } else if ("SearchUserProfileActivity".equals(source) || "UserProfileActivity".equals(source)) {
                    Log.d(TAG, "Navigating to SearchUserProfileActivity");
                    intent = new Intent(ChatActivity.this, SearchUserProfileActivity.class);
                    intent.putExtra("userId", recipientId);
                    intent.putExtra("username", otherUsername);
                } else {
                    Log.w(TAG, "Unknown source: " + source + ". Defaulting to ChatListActivity.");
                    intent = new Intent(ChatActivity.this, ChatListActivity.class);
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        if (sendMessageBtn == null) {
            Log.e(TAG, "sendButton Button not found in layout. Check activity_chat.xml for ID 'sendButton'.");
        } else {
            sendMessageBtn.setOnClickListener(v -> {
                String messageText = messageInput.getText().toString().trim();
                if (!messageText.isEmpty()) {
                    sendMessageBtn.setEnabled(false);
                    sendMessage(messageText);
                    new android.os.Handler().postDelayed(() -> sendMessageBtn.setEnabled(true), 1000);
                }
            });
        }

        if (callButton != null) {
            callButton.setOnClickListener(v -> {
                Intent intent = new Intent(ChatActivity.this, CallActivity.class);
                intent.putExtra("channelName", chatId);
                intent.putExtra("recipientName", otherUsername);
                startActivity(intent);
            });
        } else {
            Log.e(TAG, "callButton ImageButton not found in layout. Check activity_chat.xml for ID 'callButton'.");
        }

        setupChatRecyclerView();
    }

    private String createChatId(String user1, String user2) {
        List<String> userIds = Arrays.asList(user1, user2);
        userIds.sort(String::compareTo);
        return userIds.get(0) + "_" + userIds.get(1);
    }

    private void setupChatRecyclerView() {
        Query query = db.collection("chats").document(chatId).collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING);

        messageListener = query.addSnapshotListener((querySnapshot, e) -> {
            if (e != null) {
                Log.e(TAG, "Listen failed: " + e.getMessage(), e);
                return;
            }
            if (querySnapshot != null) {
                for (DocumentChange dc : querySnapshot.getDocumentChanges()) {
                    Message message = dc.getDocument().toObject(Message.class);
                    message.setId(dc.getDocument().getId());
                    if (message.getSenderId().equals(Objects.requireNonNull(auth.getCurrentUser()).getUid())) {
                        message.setSenderUsername("Me");
                    } else {
                        message.setSenderUsername(getIntent().getStringExtra("otherUsername"));
                    }
                    int index = -1;
                    for (int i = 0; i < messageList.size(); i++) {
                        if (messageList.get(i).getId().equals(message.getId())) {
                            index = i;
                            break;
                        }
                    }
                    switch (dc.getType()) {
                        case ADDED:
                            if (index == -1) { // Only add if it doesn't already exist
                                messageList.add(message);
                                adapter.notifyItemInserted(messageList.size() - 1);
                                recyclerView.smoothScrollToPosition(messageList.size() - 1);
                            }
                            break;
                        case MODIFIED:
                            if (index != -1) {
                                messageList.set(index, message);
                                adapter.notifyItemChanged(index);
                            }
                            break;
                        case REMOVED:
                            if (index != -1) { // Only remove if it exists
                                messageList.remove(index);
                                adapter.notifyItemRemoved(index);
                                adapter.notifyItemRangeChanged(index, messageList.size());
                            }
                            break;
                    }
                }
            }
        });
    }

    private void sendMessage(String messageText) {
        Timestamp timestamp = Timestamp.now();
        Message message = new Message(messageText, timestamp);
        message.setSenderId(Objects.requireNonNull(auth.getCurrentUser()).getUid());
        Log.d(TAG, "sendMessage called with text: " + messageText);

        db.collection("chats").document(chatId).collection("messages")
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    messageInput.setText("");
                    sendNotification(messageText);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to send message: " + e.getMessage()));
    }

    private void sendNotification(String message) {
        String currentUsername = getIntent().getStringExtra("currentUsername");
        Log.d(TAG, "sendNotification called for message: " + message + " to token: " + recipientFcmToken);

        if (recipientFcmToken == null) {
            Log.w(TAG, "Skipping notification: No recipient token available");
            return;
        }

        try {
            JSONObject jsonObject = new JSONObject();
            JSONObject notificationObj = new JSONObject();
            notificationObj.put("title", currentUsername + ": New Message");
            notificationObj.put("body", message);

            JSONObject dataObj = new JSONObject();
            dataObj.put("userId", Objects.requireNonNull(auth.getCurrentUser()).getUid());
            dataObj.put("username", currentUsername);

            jsonObject.put("notification", notificationObj);
            jsonObject.put("data", dataObj);
            jsonObject.put("to", recipientFcmToken);
            jsonObject.put("tag", message.hashCode() + System.currentTimeMillis());

            callApi(jsonObject);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send notification: " + e.getMessage());
        }
    }

    private void callApi(JSONObject jsonObject) {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        OkHttpClient client = new OkHttpClient();
        String url = "https://fcm.googleapis.com/fcm/send";
        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Authorization", "Bearer YOUR_ACTUAL_FCM_SERVER_KEY_HERE")
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Log.e(TAG, "Notification request failed: " + e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseMessage = response.message();
                String responseBody = response.body() != null ? response.body().string() : "No response body";
                Log.d(TAG, "FCM Response: " + response.code() + " - " + responseMessage + " - " + responseBody);
            }
        });
    }

    private void showMessageOptions(int position) {
        if (position < 0 || position >= messageList.size()) {
            Log.e(TAG, "Invalid position for message options: " + position);
            return;
        }

        Message message = messageList.get(position);
        String currentUserId = Objects.requireNonNull(auth.getCurrentUser()).getUid();

        if (!message.getSenderId().equals(currentUserId)) {
            Toast.makeText(this, "You can only edit or delete your own messages", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Message Options")
                .setItems(new CharSequence[]{"Edit", "Delete"}, (dialog, which) -> {
                    switch (which) {
                        case 0: // Edit
                            editMessage(position);
                            break;
                        case 1: // Delete
                            deleteMessage(position);
                            break;
                    }
                });
        builder.show();
    }

    private void editMessage(int position) {
        if (position < 0 || position >= messageList.size()) {
            Log.e(TAG, "Invalid position for editing: " + position);
            return;
        }

        Message message = messageList.get(position);
        EditText editText = new EditText(this);
        editText.setText(message.getText());

        new AlertDialog.Builder(this)
                .setTitle("Edit Message")
                .setView(editText)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newText = editText.getText().toString().trim();
                    if (!newText.isEmpty()) {
                        message.setText(newText);
                        message.setTimestamp(Timestamp.now());
                        db.collection("chats").document(chatId).collection("messages")
                                .document(message.getId())
                                .set(message)
                                .addOnSuccessListener(aVoid -> {
                                    adapter.notifyItemChanged(position);
                                    Toast.makeText(this, "Message edited", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to edit message: " + e.getMessage()));
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deleteMessage(int position) {
        if (position < 0 || position >= messageList.size()) {
            Log.e(TAG, "Invalid position for deletion: " + position);
            return;
        }

        Message message = messageList.get(position);
        String messageId = message.getId();

        // Remove the message locally first
        messageList.remove(position);
        adapter.notifyItemRemoved(position);
        adapter.notifyItemRangeChanged(position, messageList.size());

        // Delete from Firestore
        db.collection("chats").document(chatId).collection("messages")
                .document(messageId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Message deleted", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Message deleted successfully at position: " + position);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete message: " + e.getMessage());
                    Toast.makeText(this, "Failed to delete message", Toast.LENGTH_SHORT).show();
                    // Revert the local change if Firestore deletion fails
                    messageList.add(position, message);
                    adapter.notifyItemInserted(position);
                    adapter.notifyItemRangeChanged(position, messageList.size());
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) {
            messageListener.remove();
        }
    }
}