package com.example.mecore;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
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

    private EditText messageInput;
    private RecyclerView recyclerView;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Sign in anonymously if the user isn't signed in
        if (auth.getCurrentUser() == null) {
            auth.signInAnonymously()
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Signed in anonymously", Toast.LENGTH_SHORT).show();
                            initializeChat();
                        } else {
                            Toast.makeText(this, "Anonymous sign-in failed", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
        } else {
            initializeChat();
        }
    }

    @SuppressLint("SetTextI18n")
    private void initializeChat() {
        // Get the recipient's user ID and username from the Intent
        String recipientId = getIntent().getStringExtra("recipientId");
        String otherUsername = getIntent().getStringExtra("otherUsername");

        if (recipientId == null) {
            Toast.makeText(this, "Unable to start chat: Missing recipient ID", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Construct the chatId (e.g., user1_user2)
        String currentUserId = Objects.requireNonNull(auth.getCurrentUser()).getUid();
        chatId = createChatId(currentUserId, recipientId);

        // Initialize UI
        messageInput = findViewById(R.id.messageEditText);
        ImageButton sendMessageBtn = findViewById(R.id.sendButton);
        ImageButton backBtn = findViewById(R.id.back_btn);
        TextView otherUsernameTextView = findViewById(R.id.chatTitle);
        recyclerView = findViewById(R.id.chat_recycler_view);

        // Set the other user's username
        if (otherUsername != null) {
            otherUsernameTextView.setText("Chat with " + otherUsername);
        }

        // Set up back button
        backBtn.setOnClickListener(v -> onBackPressed());

        // Initialize message list and adapter
        messageList = new ArrayList<>();
        adapter = new ChatAdapter(messageList);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setReverseLayout(true); // Show newest messages at the bottom
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);

        // Send message
        sendMessageBtn.setOnClickListener(v -> {
            String messageText = messageInput.getText().toString().trim();
            if (!messageText.isEmpty()) {
                sendMessage(messageText);
            }
        });

        // Listen for messages
        setupChatRecyclerView();
    }

    private String createChatId(String user1, String user2) {
        // Sort the user IDs to ensure consistency (e.g., user1_user2 or user2_user1)
        List<String> userIds = Arrays.asList(user1, user2);
        userIds.sort(String::compareTo);
        return userIds.get(0) + "_" + userIds.get(1);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void setupChatRecyclerView() {
        Query query = db.collection("chats").document(chatId).collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING);

        messageListener = query.addSnapshotListener((querySnapshot, e) -> {
            if (e != null) {
                Log.e(TAG, "Listen failed: " + e.getMessage(), e);
                Toast.makeText(this, "Failed to load messages: " + e.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }
            if (querySnapshot != null) {
                messageList.clear();
                for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot) {
                    try {
                        Message message = doc.toObject(Message.class);
                        if (message != null) {
                            messageList.add(message);
                        }
                    } catch (Exception ex) {
                        Log.e(TAG, "Failed to deserialize message: " + doc.getId(), ex);
                    }
                }
                adapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(0); // Scroll to the newest message
            }
        });
    }

    private void sendMessage(String messageText) {
        Timestamp timestamp = Timestamp.now();
        Message message = new Message(messageText, timestamp);
        message.setSenderId(Objects.requireNonNull(auth.getCurrentUser()).getUid());

        db.collection("chats").document(chatId).collection("messages")
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    messageInput.setText("");
                    sendNotification(messageText);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to send message: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void sendNotification(String message) {
        // Placeholder: You'll need the recipient's FCM token
        String recipientFcmToken = "RECIPIENT_FCM_TOKEN"; // Replace with actual token

        try {
            JSONObject jsonObject = new JSONObject();
            JSONObject notificationObj = new JSONObject();
            notificationObj.put("title", "New Message");
            notificationObj.put("body", message);

            JSONObject dataObj = new JSONObject();
            dataObj.put("userId", Objects.requireNonNull(auth.getCurrentUser()).getUid());

            jsonObject.put("notification", notificationObj);
            jsonObject.put("data", dataObj);
            jsonObject.put("to", recipientFcmToken);

            callApi(jsonObject);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send notification: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                .header("Authorization", "Bearer YOUR_FCM_SERVER_KEY") // Replace with your FCM server key
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Failed to send notification: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseMessage = response.message();
                if (response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Notification sent", Toast.LENGTH_SHORT).show());
                } else {
                    runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Notification failed: " + responseMessage, Toast.LENGTH_SHORT).show());
                }
            }
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