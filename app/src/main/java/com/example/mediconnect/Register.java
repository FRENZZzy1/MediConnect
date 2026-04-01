package com.example.mediconnect;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Calendar;

public class Register extends AppCompatActivity {

    TextInputEditText etDOB, etFirstName, etLastName, etEmail, etPhone, etPassword;
    AutoCompleteTextView spinnerSex;
    ImageView imgProfilePreview;

    Uri imageUri;

    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();

        // Bind Views
        etDOB = findViewById(R.id.etDOB);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        spinnerSex = findViewById(R.id.spinnerSex);
        imgProfilePreview = findViewById(R.id.imgProfilePreview);

        // ── Date Picker ──
        etDOB.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                etDOB.setText(day + "/" + (month + 1) + "/" + year);
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        // ── Dropdown ──
        String[] options = {"Male", "Female", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, options);
        spinnerSex.setAdapter(adapter);
        spinnerSex.setOnClickListener(v -> spinnerSex.showDropDown());

        // ── Upload Image ──
        findViewById(R.id.btnUploadPhoto).setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(intent, 1);
        });

        // ── Register Button ──
        findViewById(R.id.btnRegister).setOnClickListener(v -> registerUser());

        // ── Edge Padding ──
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top,
                    systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // ── Handle Image Result ──
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            imgProfilePreview.setImageURI(imageUri);
        }
    }




    // ── Register User ──
    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email & Password required", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String userId = authResult.getUser().getUid();
                    uploadImageAndSaveData(userId);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ── Upload Image ──
    private void uploadImageAndSaveData(String userId) {

        if (imageUri != null) {
            StorageReference storageRef = FirebaseStorage.getInstance()
                    .getReference("profile_images/" + userId + ".jpg");

            storageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot ->
                            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                saveUserData(userId, uri.toString());
                            }))
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Image Upload Failed", Toast.LENGTH_SHORT).show());

        } else {
            saveUserData(userId, "");
        }
    }

    // ── Save Data ──
    private void saveUserData(String userId, String imageUrl) {

        String firstName = etFirstName.getText().toString();
        String lastName = etLastName.getText().toString();
        String phone = etPhone.getText().toString();
        String dob = etDOB.getText().toString();
        String sex = spinnerSex.getText().toString();
        String email = etEmail.getText().toString();

        DatabaseReference dbRef = FirebaseDatabase.getInstance()
                .getReference("Users");

        User user = new User(firstName, lastName, email, phone, dob, sex, imageUrl);

        dbRef.child(userId).setValue(user)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Registered Successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Database Error", Toast.LENGTH_SHORT).show());
    }

    public void SplashAct(View view){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }



}