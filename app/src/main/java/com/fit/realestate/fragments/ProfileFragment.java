package com.fit.realestate.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.fit.realestate.MyUtils;
import com.fit.realestate.R;
import com.fit.realestate.activities.ChangePasswordActivity;
import com.fit.realestate.activities.MainActivity;
import com.fit.realestate.activities.MyPropertyListActivity;
import com.fit.realestate.activities.PostAddActivity;
import com.fit.realestate.activities.ProfileEditActivity;
import com.fit.realestate.databinding.FragmentProfileBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileFragment extends Fragment {
    // View binding
    private FragmentProfileBinding binding;
    // Tag to show logs in logcat
    private static final String TAG = "PROFILE_TAG";
    // Context for this fragment class
    private Context mContext;
    // Firebase Auth for auth related tasks
    private FirebaseAuth firebaseAuth;
    // ProgressDialog to show while verify account
    private ProgressDialog progressDialog;

    @Override
    public void onAttach(@NonNull Context context) {
        // get and init the context for this fragment class
        mContext = context;
        super.onAttach(context);
    }

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate/bind the layout for this fragment
        binding = FragmentProfileBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // init/setup ProgressDialog to show while account verification
        progressDialog = new ProgressDialog(mContext);
        progressDialog.setTitle("Xin vui lòng đợi");
        progressDialog.setCanceledOnTouchOutside(false);

        // get instance of firebase auth for Auth related tasks
        firebaseAuth = FirebaseAuth.getInstance();

        loadMyInfo();

        // handle postAdBtn click, start PostAddActivity
        binding.postAddBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, PostAddActivity.class);
                startActivity(intent);
            }
        });

        // handle logoutBtn click, logout user and start MainActivity
        binding.logoutCv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // logout user
                firebaseAuth.signOut();

                startActivity(new Intent(mContext, MainActivity.class));
                getActivity().finishAffinity();
            }
        });

        // handle editProfileCv click, start ProfileEditActivity
        binding.editProfileCv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(mContext, ProfileEditActivity.class));
            }
        });

        // handle changePasswordCv click, start ChangePasswordActivity
        binding.changePasswordCv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(mContext, ChangePasswordActivity.class));
            }
        });

        // handle myPropertiesCv click, start MyPropertyListActivity
        binding.myPropertiesCv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(mContext, MyPropertyListActivity.class));
            }
        });

        // Handle confirm delete button click
        binding.deleteAccountCv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDeleteConfirmation();
            }
        });

        // Handle cancel delete button click
        binding.deleteConfirmLayout.cancelDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideDeleteConfirmation();
            }
        });

        binding.verifyAccountCv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                verifyAccount();
            }
        });
    }

    private void verifyAccount() {
        Log.d(TAG, "verifyAccount: Starting email verification process");

        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            Log.e(TAG, "verifyAccount: User is null");
            Toast.makeText(mContext, "Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = user.getEmail();
        if (email == null || email.isEmpty()) {
            Log.e(TAG, "verifyAccount: Email is null or empty");
            Toast.makeText(mContext, "Vui lòng cập nhật email trước khi xác thực!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(mContext, ProfileEditActivity.class));
            return;
        }

        // Check if already verified
        if (user.isEmailVerified()) {
            Toast.makeText(mContext, "Email đã được xác thực!", Toast.LENGTH_SHORT).show();
            loadMyInfo(); // Refresh UI to hide verify button
            return;
        }

        // Show progress dialog
        progressDialog.setMessage("Đang gửi email xác thực...");
        progressDialog.show();

        // Send verification email
        user.sendEmailVerification()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progressDialog.dismiss();

                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: Verification email sent successfully");
                            Toast.makeText(mContext, "Link xác thực đã được gửi đến email của bạn!\nVui lòng kiểm tra hộp thư và làm theo hướng dẫn.", Toast.LENGTH_LONG).show();

                            // Optionally show a dialog to check verification status
                            showVerificationCheckDialog();
                        } else {
                            Log.e(TAG, "onComplete: Failed to send verification email", task.getException());
                            String errorMessage = task.getException() != null ?
                                    task.getException().getMessage() : "Lỗi không xác định";
                            Toast.makeText(mContext, "Không thể gửi email xác thực: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    // Method để hiển thị dialog kiểm tra trạng thái xác thực
    private void showVerificationCheckDialog() {
        // Tạo một dialog đơn giản để người dùng có thể refresh trạng thái
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(mContext);
        builder.setTitle("Xác thực Email");
        builder.setMessage("Email xác thực đã được gửi. Sau khi bạn nhấp vào link trong email, hãy nhấn 'Kiểm tra' để cập nhật trạng thái.");

        builder.setPositiveButton("Kiểm tra", (dialog, which) -> {
            checkVerificationStatus();
        });

        builder.setNegativeButton("Đóng", (dialog, which) -> {
            dialog.dismiss();
        });

        builder.setNeutralButton("Gửi lại", (dialog, which) -> {
            verifyAccount(); // Gửi lại email xác thực
        });

        builder.show();
    }

    // Method để kiểm tra trạng thái xác thực
    private void checkVerificationStatus() {
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            Toast.makeText(mContext, "Lỗi: Không tìm thấy người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Đang kiểm tra trạng thái xác thực...");
        progressDialog.show();

        // Reload user to get latest verification status
        user.reload().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                progressDialog.dismiss();

                if (task.isSuccessful()) {
                    if (user.isEmailVerified()) {
                        Toast.makeText(mContext, "Chúc mừng! Email đã được xác thực thành công!", Toast.LENGTH_LONG).show();
                        loadMyInfo(); // Refresh UI to hide verify button
                    } else {
                        Toast.makeText(mContext, "Email chưa được xác thực. Vui lòng kiểm tra hộp thư của bạn.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.e(TAG, "checkVerificationStatus: Failed to reload user", task.getException());
                    Toast.makeText(mContext, "Lỗi khi kiểm tra trạng thái xác thực", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showDeleteConfirmation() {
        Log.d(TAG, "showDeleteConfirmation: Showing delete confirmation dialog");
        binding.deleteConfirmLayout.getRoot().setVisibility(View.VISIBLE);
    }

    private void hideDeleteConfirmation() {
        Log.d(TAG, "hideDeleteConfirmation: Hiding delete confirmation dialog");
        binding.deleteConfirmLayout.getRoot().setVisibility(View.GONE);
    }

    private void deleteAccount() {
        Log.d(TAG, "deleteAccount: Starting account deletion process");

        // Show progress dialog
        progressDialog.setMessage("Đang xóa tài khoản...");
        progressDialog.show();

        // Get current user
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "deleteAccount: User is null");
            progressDialog.dismiss();
            Toast.makeText(mContext, "Lỗi: Không tìm thấy người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        Log.d(TAG, "deleteAccount: Deleting account for UID: " + uid);

        // First, delete user data from Realtime Database
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
        userRef.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Log.d(TAG, "onSuccess: User data deleted from database");

                // Now delete the Firebase Auth account
                user.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progressDialog.dismiss();

                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: Account deleted successfully");
                            Toast.makeText(mContext, "Tài khoản đã được xóa thành công", Toast.LENGTH_SHORT).show();

                            // Navigate to MainActivity
                            Intent intent = new Intent(mContext, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            getActivity().finish();
                        } else {
                            Log.e(TAG, "onComplete: Failed to delete account", task.getException());
                            Toast.makeText(mContext, "Lỗi khi xóa tài khoản: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();

                            // Try to restore user data if auth deletion failed
                            // Note: This is a simplified approach - in production you might want more robust error handling
                        }

                        hideDeleteConfirmation();
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "onFailure: Failed to delete user data from database", e);
                progressDialog.dismiss();
                Toast.makeText(mContext, "Lỗi khi xóa dữ liệu người dùng: " + e.getMessage(), Toast.LENGTH_LONG).show();
                hideDeleteConfirmation();
            }
        });
    }

    private void loadMyInfo() {
        // Reference of current user info in Firebase Realtime Database to get user info
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child("" + firebaseAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // get user info, spellings should be same as in firebase realtime database
                        String dob = "" + snapshot.child("dob").getValue();
                        String email = "" + snapshot.child("email").getValue();
                        String name = "" + snapshot.child("name").getValue();
//                        String phoneCode = "" + snapshot.child("phoneCode").getValue();
                        String phoneNumber = "" + snapshot.child("phoneNumber").getValue();
                        String profileImageUrl = "" + snapshot.child("profileImageUrl").getValue();
                        String timestamp = "" + snapshot.child("timestamp").getValue();
                        String userType = "" + snapshot.child("userType").getValue();

                        // concatenate phone code and phone number to make full phone number
                        String phone = phoneNumber;

                        // to avoid null or format exceptions
                        if (timestamp.equals("null")) {
                            timestamp = "0";
                        }

                        // format timestamp to dd/MM/yyyy
                        String formattedDate = MyUtils.formatTimestampDate(Long.parseLong(timestamp));

                        // set data to UI
                        binding.emailTv.setText(email);
                        binding.fullNameTv.setText(name);
                        binding.dobTv.setText(dob);
                        // format phone number
                        binding.phoneTv.setText(MyUtils.formatPhoneNumber(phone));
                        binding.memberSinceTv.setText(formattedDate);

                        /* check user type i.e Email/Phone/Google In case of Phone & Google
                           account is already verified but in case of Email account user
                           have to verify */
                        if (userType.equals(MyUtils.USER_TYPE_EMAIL)) {
                            // userType is Email, have to check if verified or not
                            FirebaseUser currentUser = firebaseAuth.getCurrentUser();

                            if (currentUser != null) {
                                boolean isVerified = currentUser.isEmailVerified();
                                Log.d(TAG, "loadMyInfo: Email user - isVerified: " + isVerified);

                                // Check if verified or not
                                if (isVerified) {
                                    // Verified, hide the Verify Account option
                                    binding.verifyAccountCv.setVisibility(View.GONE);
                                    binding.verificationTv.setText("Đã xác thực");
                                } else {
                                    // Not verified, show the Verify Account option
                                    binding.verifyAccountCv.setVisibility(View.VISIBLE);
                                    binding.verificationTv.setText("Chưa xác thực");
                                }
                            } else {
                                // No current user, hide verify option
                                binding.verifyAccountCv.setVisibility(View.GONE);
                                binding.verificationTv.setText("Chưa xác thực");
                            }
                        } else if (userType.equals(MyUtils.USER_TYPE_PHONE)) {
                            /* userType is Phone, already verified, hide the Verify Account option */
                            binding.verifyAccountCv.setVisibility(View.GONE);
                            binding.verificationTv.setText("Đã xác thực");
                        } else if (userType.equals(MyUtils.USER_TYPE_GOOGLE)) {
                            /* userType is Google, already verified, hide the Verify Account option */
                            binding.verifyAccountCv.setVisibility(View.GONE);
                            binding.verificationTv.setText("Đã xác thực");
                        } else {
                            // Unknown user type, hide verify option
                            binding.verifyAccountCv.setVisibility(View.GONE);
                            binding.verificationTv.setText("Chưa xác định");
                        }

                        // set profile image to profileIv
                        try {
                            Glide.with(mContext)
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.person_black)
                                    .into(binding.profileIv);
                        } catch (Exception e) {
                            Log.e(TAG, "onDataChange: ", e);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
}