package com.fit.realestate.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.fit.realestate.MyUtils;
import com.fit.realestate.R;
import com.fit.realestate.databinding.ActivityLoginPhoneBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class LoginPhoneActivity extends AppCompatActivity {
    // View binding
    private ActivityLoginPhoneBinding binding;
    // Tag to show logs in logcat
    private static final String TAG = "LOGIN_PHONE_TAG";
    // Firebase Auth for auth related tasks
    private FirebaseAuth firebaseAuth;
    // ProgressDialog to show while phone login, saving user info
    private ProgressDialog progressDialog;
    private PhoneAuthProvider.ForceResendingToken forceResendingToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallBacks;
    private String mVerificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // activity_phone_login.xml = ActivityLoginPhoneBinding
        binding = ActivityLoginPhoneBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // For the start show phone input UI and hide OTP UI
        binding.phoneInputRl.setVisibility(View.VISIBLE);
        binding.otpInputRl.setVisibility(View.GONE);

        // init/setup ProgressDialog to show while login
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Xin vui lòng đợi");
        progressDialog.setCanceledOnTouchOutside(false);

        // Firebase Auth for auth related tasks
        firebaseAuth = FirebaseAuth.getInstance();

        phoneLoginCallBack();

        // Handle toolbarBackBtn click, go-back
        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        binding.sendOtpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateData();
            }
        });

        binding.resendOtpTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resendVerificationCode(forceResendingToken);
            }
        });

        binding.verifyOtpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String otp = binding.otpEt.getText().toString().trim();

                if(otp.isEmpty()) {
                    binding.otpEt.setError("Nhập mã OTP");
                    binding.otpEt.requestFocus();
                } else if (otp.length() < 6) {
                    binding.otpEt.setError("Độ dài mã OTP phải đủ 6 ký tự");
                    binding.otpEt.requestFocus();
                } else {
                    verifyPhoneNumberWithCode(otp);
                }
            }
        });
    }

    private String phoneCode = "", phoneNumber = "", phoneNumberWithCode = "";

    private void validateData() {
        phoneCode = binding.phoneCodeTil.getSelectedCountryCodeWithPlus();
        phoneNumber = binding.phoneNumberEt.getText().toString().trim();
        if (phoneNumber.startsWith("0")) {
            phoneNumber = phoneNumber.substring(1);
        }
        phoneNumberWithCode = phoneCode + phoneNumber;

        Log.d(TAG, "validateData: Mã quốc gia của số điện thoại: " + phoneCode);
        Log.d(TAG, "validateData: Số điện thoại: " + phoneNumber);
        Log.d(TAG, "validateData: Số điện thoại có mã là: " + phoneNumberWithCode);

        if (phoneNumber.isEmpty()) {
            binding.phoneNumberEt.setError("Nhập số điện thoại");
            binding.phoneNumberEt.requestFocus();
        } else {
            startPhoneNumberVerification();
        }
    }

    private void startPhoneNumberVerification() {
        // show progress
        progressDialog.setMessage("Đang gửi mã OTP tới " + phoneNumberWithCode);
        progressDialog.show();

        // Setup Phone Auth Options with phone number, time out, callback etc
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(phoneNumberWithCode)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(mCallBacks)
                .build();

        // Start phone verification with PhoneAuthOptions
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void resendVerificationCode(PhoneAuthProvider.ForceResendingToken token) {
        progressDialog.setMessage("Đang gửi lại mã OTP tới " + phoneNumberWithCode);
        progressDialog.show();

        // Setup Phone Auth Options with phone number, time out, callback etc
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(phoneNumberWithCode)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(mCallBacks)
                .setForceResendingToken(token)
                .build();

        // Start phone verification with PhoneAuthOptions
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void verifyPhoneNumberWithCode(String otp) {
        Log.d(TAG, "verifyPhoneNumberWithCode: Mã OTP: " + otp);

        // Show progress
        progressDialog.setMessage("Đang xác thực mã OTP...");
        progressDialog.show();

        /* PhoneAuthCredential with verification ID and OTP to signIn user
           with signInWithPhoneAuthCredential */
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, otp);
        signInWithPhoneAuthCredential(credential);
    }

    private void phoneLoginCallBack() {
        Log.d(TAG, "phoneLoginCallBack: ");

        mCallBacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                super.onCodeSent(verificationId, token);
                Log.d(TAG, "onCodeSent: ");

                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                forceResendingToken = token;

                // OTP is sent so hide progress for now
                progressDialog.dismiss();

                // OTP is sent so hide Phone UI and show OTP UI
                binding.phoneInputRl.setVisibility(View.GONE);
                binding.otpInputRl.setVisibility(View.VISIBLE);

                // Show toast for success sending OTP
                MyUtils.toast(LoginPhoneActivity.this, "Mã OTP được gửi tới " + phoneNumberWithCode);

                // Show user a message that Please type the verification code sent to phone number user has input
                binding.loginPhoneLabel.setText("Kiểm tra mã OTP đã được gửi ở điện thoai");
            }

            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                Log.d(TAG, "onVerificationCompleted: ");

                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Log.e(TAG, "onVerificationFailed: ", e);

                progressDialog.dismiss();
                MyUtils.toast(LoginPhoneActivity.this, "Xác thực thất bại vì " + e.getMessage());
            }
        };
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        Log.d(TAG, "signInWithPhoneAuthCredential: ");

        // Show progress
        progressDialog.setMessage("Đang đăng nập...");
        progressDialog.show();

        // SignIn in to firebase auth using Phone Credentials
        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        Log.d(TAG, "onSuccess: ");

                        /* SignIn Success, let's check if the user is new (New Account Register)
                           or existing (Existing Login) */
                        if (authResult.getAdditionalUserInfo().isNewUser()) {
                            Log.d(TAG, "onSuccess: Người dùng mới, tài khoản đã được tạo...");
                            // New User, Account created. Let's save user info to firebase realtime database
                            updateUserInfo();
                        } else {
                            Log.d(TAG, "onSuccess: Người dùng đã tồn tại, đăng nhập...");
                            /* New User, Account created. No need to save user info to firebase
                               realtime database, Start MainActivity */
                            startActivity(new Intent(LoginPhoneActivity.this, MainActivity.class));
                            finishAffinity();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // SignIn failed, show exception message
                        Log.e(TAG, "onFailure: ", e);
                        progressDialog.dismiss();
                        MyUtils.toast(LoginPhoneActivity.this, "Đăng nhập thất bại do " + e.getMessage());
                    }
                });
    }

    private void updateUserInfo() {
        Log.d(TAG, "updateUserInfo: ");

        progressDialog.setMessage("Đang lưu thông tin người dùng...");
//        progressDialog.show();
        progressDialog.dismiss();

        long timestamp = MyUtils.timestamp();
        String registeredUserUid = firebaseAuth.getUid();

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("uid", registeredUserUid);
        hashMap.put("email", "");
        hashMap.put("name", "");
        hashMap.put("timestamp", timestamp);
        hashMap.put("phoneCode", "" + phoneCode);
        hashMap.put("phoneNumber", "" + phoneNumber);
        hashMap.put("profileImageUrl", "");
        hashMap.put("dob", "");
        // possible values Email/Phone/Google
        hashMap.put("userType", "" + MyUtils.USER_TYPE_PHONE);
        hashMap.put("token", ""); // FCM token to send push notifications

        // set data to firebase db
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(registeredUserUid)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        // User info save success
                        Log.d(TAG, "onSuccess: Đã lưu thông tin người dùng...");
                        progressDialog.dismiss();

                        // Start MainActivity
                        startActivity(new Intent(LoginPhoneActivity.this, MainActivity.class));
                        finishAffinity();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // User info save failed
                        Log.e(TAG, "onFailure: ", e);
                        progressDialog.dismiss();
                        MyUtils.toast(LoginPhoneActivity.this, "Thất bại khi lưu do " + e.getMessage());
                    }
                });
    }
}