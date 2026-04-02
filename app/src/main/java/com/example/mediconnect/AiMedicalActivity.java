package com.example.mediconnect;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AiMedicalActivity extends AppCompatActivity {

    private EditText etQuestion;
    private ImageButton btnSend;
    private LinearLayout chatContainer;
    private ScrollView scrollView;
    private ProgressBar progressBar;
    private String userName = "there";

    private static final String API_KEY = "DITO LAGAY API";
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_medical);

        etQuestion    = findViewById(R.id.etQuestion);
        btnSend       = findViewById(R.id.btnSend);
        chatContainer = findViewById(R.id.chatContainer);
        scrollView    = findViewById(R.id.scrollView);
        progressBar   = findViewById(R.id.progressBar);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String name = currentUser.getDisplayName();
            String email = currentUser.getEmail();
            if (name != null && !name.isEmpty()) {
                userName = name;
            } else if (email != null) {
                userName = email.split("@")[0];
            }
        }

        addMessage("Hi, " + userName + "! I'm MediBot 🩺, your AI Medical Assistant.\n\nDescribe your symptoms and I'll help you understand what might be going on. Remember, I'm here to guide — not replace your doctor!", false);

        btnSend.setOnClickListener(v -> {
            String question = etQuestion.getText().toString().trim();
            if (!question.isEmpty()) {
                addMessage(question, true);
                etQuestion.setText("");
                askOpenAI(question);
            }
        });
    }

    private void addMessage(String message, boolean isUser) {
        TextView tv = new TextView(this);
        tv.setText(message);
        tv.setPadding(36, 24, 36, 24);
        tv.setTextSize(14);
        tv.setLineSpacing(4, 1.2f);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = 20;

        if (isUser) {
            tv.setTextColor(0xFFFFFFFF);
            tv.setBackgroundResource(R.drawable.bg_user_bubble);
            params.gravity    = Gravity.END;
            params.leftMargin  = 120;
            params.rightMargin = 16;
        } else {
            tv.setTextColor(0xFF1F2937);
            tv.setBackgroundResource(R.drawable.bg_ai_bubble);
            params.gravity    = Gravity.START;
            params.rightMargin = 120;
            params.leftMargin  = 16;
        }

        tv.setLayoutParams(params);
        chatContainer.addView(tv);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void askOpenAI(String question) {
        progressBar.setVisibility(View.VISIBLE);
        btnSend.setEnabled(false);

        try {
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content",
                    "You are MediBot, a professional AI medical assistant for the MediConnect app. " +
                            "The user's name is " + userName + ". Always greet them by name at the start of your response. " +
                            "You can understand and respond in ANY language the user uses, including Filipino, Tagalog, Taglish, and English. " +
                            "Always reply in the SAME language the user used. " +
                            "Your role is ONLY to help patients understand their symptoms, possible causes, and general health advice. " +
                            "Always respond in a calm, friendly, and professional tone. " +
                            "Structure every response clearly with these sections:\n" +
                            "🔍 Possible Causes:\n" +
                            "💊 Suggestions:\n" +
                            "🏥 When to See a Doctor:\n\n" +
                            "Always end with a short reminder to consult a licensed doctor. " +
                            "If the user asks anything NOT related to health, medicine, or symptoms, respond in their language politely and redirect them. " +
                            "Never diagnose definitively. Only suggest possibilities based on symptoms.");

            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", question);

            JSONArray messages = new JSONArray();
            messages.put(systemMessage);
            messages.put(userMessage);

            JSONObject body = new JSONObject();
            body.put("model", "gpt-3.5-turbo");
            body.put("messages", messages);
            body.put("max_tokens", 600);

            RequestBody requestBody = RequestBody.create(
                    body.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnSend.setEnabled(true);
                        addMessage("⚠️ Network error: " + e.getMessage(), false);
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnSend.setEnabled(true);
                        try {
                            JSONObject json = new JSONObject(responseBody);
                            String reply = json.getJSONArray("choices")
                                    .getJSONObject(0)
                                    .getJSONObject("message")
                                    .getString("content");
                            addMessage(reply, false);
                        } catch (Exception e) {
                            addMessage("⚠️ Failed to get response. Please try again.", false);
                        }
                    });
                }
            });

        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            btnSend.setEnabled(true);
            addMessage("⚠️ Error: " + e.getMessage(), false);
        }
    }
}