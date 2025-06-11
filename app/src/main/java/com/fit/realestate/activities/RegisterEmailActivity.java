package com.fit.realestate.activities;

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

import com.fit.realestate.MyUtils;
import com.fit.realestate.R;
import com.fit.realestate.databinding.ActivityRegisterEmailBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterEmailActivity extends AppCompatActivity {
    // View binding
    private ActivityRegisterEmailBinding binding;
    // Tag to show  logs in logcat
    private static final String TAG = "REGISTER_EMAIL_TAG";
    // Firebase Auth for auth related tasks
    private FirebaseAuth firebaseAuth;
    // ProgressDialog to show while sign-up
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // activity_register_email.xml = ActivityRegisterEmailBinding
        binding = ActivityRegisterEmailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // get instance of firebase auth for Auth related tasks
        firebaseAuth = FirebaseAuth.getInstance();

        // init/setup ProgressDialog to show while sign-up
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Xin vui lòng đợi");
        progressDialog.setCanceledOnTouchOutside(false);

        // handle toolbarBackBtn click, go-back
        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // handle haveAccountTv click, go-back to LoginEmailActivity
        binding.haveAccountTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // handle registerBtn click, start user registeration
        binding.registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateData();
            }
        });
    }

    private String email, password, cPassword, phone;

    private void validateData() {
        // input data
        email = binding.emailEt.getText().toString().trim();
        password = binding.passwordEt.getText().toString();
        cPassword = binding.cPasswordEt.getText().toString();
        phone = binding.phoneNumberEt.getText().toString().trim();

        Log.d(TAG, "validateData: Email: " + email);
        Log.d(TAG, "validateData: Mật khẩu: " + password);
        Log.d(TAG, "validateData: Nhập lại mật khẩu: " + cPassword);
        Log.d(TAG, "validateData: Số điện thoại: " + phone);

        // validate data
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            // email pattern is invalid, show error
            binding.emailEt.setError("Email không hợp lệ!");
            binding.emailEt.requestFocus();
        } else if (phone.isEmpty()) {
            binding.phoneNumberEt.setError("Số điện thoại không được để trống");
            binding.phoneNumberEt.requestFocus();
        } else if (!phone.startsWith("0")) {
            binding.phoneNumberEt.setError("Số điện thoại phải bắt đầu bằng số 0");
            binding.phoneNumberEt.requestFocus();
        } else if (password.isEmpty()) {
            // password is not entered, show error
            binding.passwordEt.setError("Nhập mật khẩu");
            binding.passwordEt.requestFocus();
        } else if (!password.equals(cPassword)) {
            // password and confirm password is not same, show error
            binding.cPasswordEt.setError("Mật khẩu không khớp");
            binding.passwordEt.requestFocus();
        } else {
            // all data is valid, start sign-up
            registerUser();
        }
    }

    private void registerUser() {
        // show progress
        progressDialog.setMessage("Đang tạo tài khoản");
        progressDialog.show();

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        // User Register success, we also need to save user info to firebase db
                        Log.d(TAG, "onSuccess: Đăng ký thành công");
                        updateUserInfo();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // User Register failed
                        Log.e(TAG, "onFailure: ", e);
                        MyUtils.toast(RegisterEmailActivity.this, "Thất bại do " + e.getMessage());
                        progressDialog.dismiss();
                    }
                });
    }

    private void updateUserInfo() {
        // Change progress dialog message
        progressDialog.setMessage("Đang lưu thông tin người dùng...");

        // Get current timestamp e.g. to show user registration date/time
        long timestamp = MyUtils.timestamp();
        String registeredUserEmail = firebaseAuth.getCurrentUser().getEmail();
        String registeredUserUid = firebaseAuth.getUid();

        /* Setup data to save in firebase realtime db most of the data
           will be empty and will set in edit profile */
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("uid", registeredUserUid);
        hashMap.put("email", registeredUserEmail);
        hashMap.put("name", "");
        hashMap.put("timestamp", timestamp);
        hashMap.put("phoneCode", "");
        hashMap.put("phoneNumber", phone);
        hashMap.put("profileImageUrl", "");
        hashMap.put("dob", "");
        hashMap.put("userType", MyUtils.USER_TYPE_EMAIL); // possible values Email/Phone/Google
        hashMap.put("token", "");

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(registeredUserUid)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "onSuccess: Đã lưu thông tin...");
                        progressDialog.dismiss();

                        startActivity(new Intent(RegisterEmailActivity.this, MainActivity.class));
                        finishAffinity();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "onFailure: ", e);
                        MyUtils.toast(RegisterEmailActivity.this, "Lưu thất bại vì " + e.getMessage());
                        progressDialog.dismiss();
                    }
                });
    }
}