package com.example.realestate.activities;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.realestate.MyUtils;
import com.example.realestate.R;
import com.example.realestate.databinding.ActivityChangePasswordBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {
    // view binding
    private ActivityChangePasswordBinding binding;
    // TAG for logs in logcat
    private static final String TAG = "CHANGE_PASSWORD_TAG";
    // Firebase Auth for auth related tasks
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    // ProgressDialog to show while password change process
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // init view binding... activity_change_password.xml = ActivityChangePasswordBinding
        binding = ActivityChangePasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // get instance of firebase auth for Auth related tasks
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        // init/setup ProgressDialog to show while change password
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        // handle toolbarBackBtn click, go-back
        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // handle submitBtn click, validate data to start password recovery
        binding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateData();
            }
        });
    }

    private String currentPassword = "", newPassword = "", confirmNewPassword = "";

    private void validateData() {
        Log.d(TAG, "validateData: ");

        // input data
        currentPassword = binding.currentPasswordEt.getText().toString().trim();
        newPassword = binding.newPasswordEt.getText().toString().trim();
        confirmNewPassword = binding.confirmNewPasswordEt.getText().toString().trim();

        // validate data
        if (currentPassword.isEmpty()) {
            /* Current Password Field (currentPasswordEt) is empty, show error
               in currentPasswordEt */
            binding.currentPasswordEt.setError("Enter Current Password!");
            binding.currentPasswordEt.requestFocus();
        } else if (newPassword.isEmpty()) {
            /* New Password Field (newPasswordEt) is empty, show error in
               confirmNewPasswordEt */
            binding.newPasswordEt.setError("Enter New Password!");
            binding.newPasswordEt.requestFocus();
        } else if (confirmNewPassword.isEmpty()) {
            /* Confirm New Password Field (confirmNewPasswordEt) is empty,
               show error in confirmNewPasswordEt */
            binding.confirmNewPasswordEt.setError("Enter Confirm Password!");
            binding.confirmNewPasswordEt.requestFocus();
        } else if (!newPassword.equals(confirmNewPassword)) {
            /* password in newPasswordEt & confirmNewPasswordEt doesn't match,
               show error in confirmNewPasswordEt */
            binding.confirmNewPasswordEt.setError("Password doesn't match");
            binding.confirmNewPasswordEt.requestFocus();
        } else {
            /* all data is validated, verify current password is correct first
               before updating password */
            authenticateUserForUpdatePassword();
        }
    }

    private void authenticateUserForUpdatePassword() {
        Log.d(TAG, "authenticateUserForUpdatePassword: ");

        // show progress
        progressDialog.setMessage("Authenticating User");
        progressDialog.show();

        AuthCredential authCredential = EmailAuthProvider.getCredential(firebaseUser.getEmail(), currentPassword);
        firebaseUser.reauthenticate(authCredential)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        // successfully authenticated, begin update
                        Log.d(TAG, "onSuccess: Authentication success");
                        updatePassword();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "onFailure: ", e);
                        progressDialog.dismiss();
                        MyUtils.toast(ChangePasswordActivity.this, "Failed to authenticate due to " + e.getMessage());
                    }
                });
    }

    private void updatePassword() {
        Log.d(TAG, "updatePassword: ");

        // show progress
        progressDialog.setMessage("Updating password");
        progressDialog.show();

        firebaseUser.updatePassword(newPassword)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        // password update success, you may do logout and move to login activity if you want
                        Log.d(TAG, "onSuccess: Password updated");
                        progressDialog.dismiss();
                        MyUtils.toast(ChangePasswordActivity.this, "Password updated");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // password update failure, show error message
                        Log.e(TAG, "onFailure: ", e);
                        progressDialog.dismiss();
                        MyUtils.toast(ChangePasswordActivity.this, "Failed to update due to " + e.getMessage());
                    }
                });
    }
}