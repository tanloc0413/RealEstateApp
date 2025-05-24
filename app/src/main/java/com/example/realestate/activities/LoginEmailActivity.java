package com.example.realestate.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.realestate.MyUtils;
import com.example.realestate.R;
import com.example.realestate.databinding.ActivityLoginEmailBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;


public class LoginEmailActivity extends AppCompatActivity {
    // View binding
    private ActivityLoginEmailBinding binding;
    // Tag to show logs in logcat
    private static final String TAG = "LOGIN_TAG";
    // ProgressDialog to show while sign-in
    private ProgressDialog progressDialog;
    // Firebase
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // activity_login_email.xml = ActivityLoginEmailBinding
        binding = ActivityLoginEmailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // init/setup ProgressDialog to show while sign-in
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        // get instance of firebase auth for Auth related tasks
        firebaseAuth = FirebaseAuth.getInstance();

        // handle toolbarBackBtn click, go-back
        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        binding.loginBtnTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        binding.noAccountTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginEmailActivity.this, RegisterEmailActivity.class));
            }
        });
    }

    private String email, password;

    private void validateData() {
        // input data
        email = binding.emailEt.getText().toString().trim();
        password = binding.passwordEt.getText().toString();

        Log.d(TAG, "validateData: Email: " + email);
        Log.d(TAG, "validateData: Password: " + password);

        // validate data
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            // email pattern is invalid, show error
            binding.emailEt.setError("Invalid Email!");
            binding.emailEt.requestFocus();
        } else if (password.isEmpty()) {
            // password is not entered, show error
            binding.passwordEt.setError("Enter Password");
            binding.passwordEt.requestFocus();
        } else {
            // email pattern is valid and password is enter, start login
            loginUser();
        }
    }

    private void loginUser() {
        // show progress
        progressDialog.setMessage("Logging In...");
        progressDialog.show();

        // start user login
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        // User login success
                        Log.d(TAG, "onSuccess: Logged In...");
                        progressDialog.dismiss();

                        // Start MainActivity
                        startActivity(new Intent(LoginEmailActivity.this, MainActivity.class));
                        finishAffinity();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // User login failure
                        Log.e(TAG, "onFailure: ", e);
                        progressDialog.dismiss();
                        MyUtils.toast(LoginEmailActivity.this, "Failure due to " + e.getMessage());
                    }
                });
    }
}