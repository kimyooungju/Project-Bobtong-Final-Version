package com.example.project_bobtong;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Random;

public class SignupActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword, editTextConfirmPassword;
    private Button buttonSignup;
    private TextView textViewLogin;

    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonSignup = findViewById(R.id.buttonSignup);

        mDatabase = FirebaseDatabase.getInstance().getReference("users");

        buttonSignup.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();
            String confirmPassword = editTextConfirmPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                editTextEmail.setError("이메일을 입력해주세요");
                return;
            }

            if (TextUtils.isEmpty(password)) {
                editTextPassword.setError("비밀번호를 입력해주세요");
                return;
            }

            if (!password.equals(confirmPassword)) {
                editTextConfirmPassword.setError("비밀번호가 일치하지 않습니다.");
                return;
            }

            if (password.length() < 6) {
                editTextPassword.setError("비밀번호는 6글자 이상 입력해야 합니다.");
                return;
            }

            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if (user != null) {
                                String handle = generateHandle(email);
                                User newUser = new User(email, handle);
                                mDatabase.child(user.getUid()).setValue(newUser);

                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(handle)
                                        .build();

                                user.updateProfile(profileUpdates)
                                        .addOnCompleteListener(profileUpdateTask -> {
                                            if (profileUpdateTask.isSuccessful()) {
                                                startActivity(new Intent(SignupActivity.this, MainActivity.class));
                                                finish();
                                            }
                                        });
                            }
                        } else {
                            Toast.makeText(SignupActivity.this, "회원가입에 실패했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private String generateHandle(String email) {
        String characters = "abcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder handle = new StringBuilder("@");
        handle.append(email.substring(0, email.indexOf("@"))).append("-");
        for (int i = 0; i < 8; i++) {
            handle.append(characters.charAt(random.nextInt(characters.length())));
        }
        return handle.toString();
    }
}