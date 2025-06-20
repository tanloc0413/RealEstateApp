package com.fit.realestate.activities;

import static android.content.Intent.getIntent;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.fit.realestate.MyUtils;
import com.fit.realestate.R;
import com.fit.realestate.adapters.AdapterProperty;
import com.fit.realestate.databinding.ActivityPropertyDetailBinding;
import com.fit.realestate.models.ModelProperty;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class PropertyDetailActivity extends AppCompatActivity {
    private ActivityPropertyDetailBinding binding;
    private static final String TAG = "PROPERTY_DETAIL";
    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;
    private Context mContext;
    private String propertyId = "";
    private boolean isFavorite = false;
    private String currentPropertyUid = "";
    private String sellerPhoneNumber = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPropertyDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // init/setup ProgressDialog to show while account verification
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Xin vui lòng đợi");
        progressDialog.setCanceledOnTouchOutside(false);

        // get instance of firebase auth for Auth related tasks
        firebaseAuth = FirebaseAuth.getInstance();

        propertyId = getIntent().getStringExtra("propertyId");
        checkIsFavorite(propertyId);
        loadMyProperty();

        // trở về trang trước
        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        // yêu thích bất động sản
        binding.favBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (firebaseAuth.getCurrentUser() == null) {
                    MyUtils.toast(PropertyDetailActivity.this, "Bạn chưa đăng nhập!");
                    return;
                }

                if (isFavorite) {
                    MyUtils.removeFromFavorite(PropertyDetailActivity.this, propertyId);
                    binding.favBtn.setImageResource(R.drawable.un_fav_black);
                    isFavorite = false;
                } else {
                    MyUtils.addToFavorite(PropertyDetailActivity.this, propertyId);
                    binding.favBtn.setImageResource(R.drawable.fav_red);
                    isFavorite = true;
                }
            }
        });

        binding.editBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PropertyDetailActivity.this, PropertyEditActivity.class);
                intent.putExtra("propertyId", propertyId);
                startActivity(intent);
            }
        });

        // xóa bất động sản
        binding.deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Kiểm tra xem user có đăng nhập không
                if (firebaseAuth.getCurrentUser() == null) {
                    MyUtils.toast(PropertyDetailActivity.this, "Bạn chưa đăng nhập!");
                    return;
                }

                // Kiểm tra xem có phải là chủ sở hữu bài đăng không
                if (!currentPropertyUid.equals(firebaseAuth.getUid())) {
                    MyUtils.toast(PropertyDetailActivity.this, "Bạn chỉ có thể xóa bài đăng của mình!");
                    return;
                }

                // Hiển thị dialog xác nhận xóa
                showDeleteConfirmDialog();
            }
        });

        // Thiết lập các button của delete confirm dialog ngay trong onCreate()
        View includeLayout = binding.deleteConfirmLayout.getRoot();
        View confirmDeleteBtn = includeLayout.findViewById(R.id.confirmDelete);
        View cancelDeleteBtn = includeLayout.findViewById(R.id.cancelDelete);

        if (confirmDeleteBtn != null) {
            confirmDeleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteProperty();
                }
            });
        }

        if (cancelDeleteBtn != null) {
            cancelDeleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hideDeleteConfirmDialog();
                }
            });
        }

        binding.callBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sellerPhoneNumber == null || sellerPhoneNumber.isEmpty()) {
                    Toast.makeText(PropertyDetailActivity.this, "Không có số điện thoại!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Sao chép số vào clipboard
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("phone", sellerPhoneNumber);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(PropertyDetailActivity.this, "Đã sao chép số điện thoại", Toast.LENGTH_SHORT).show();

                // Mở ứng dụng gọi điện
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + sellerPhoneNumber));
                startActivity(intent);
            }
        });

        binding.smsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sellerPhoneNumber == null || sellerPhoneNumber.isEmpty()) {
                    Toast.makeText(PropertyDetailActivity.this, "Không có số điện thoại!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Sao chép số điện thoại vào clipboard
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("phone", sellerPhoneNumber);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(PropertyDetailActivity.this, "Đã sao chép số điện thoại", Toast.LENGTH_SHORT).show();

                // Mở ứng dụng nhắn tin
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("sms:" + sellerPhoneNumber));

                // intent.putExtra("sms_body", "Chào bạn, tôi quan tâm đến bất động sản bạn đăng.");
                startActivity(intent);
            }
        });

        binding.addressEt.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                String address = binding.addressEt.getText().toString().trim();

                if (address.isEmpty()) {
                    Toast.makeText(PropertyDetailActivity.this, "Không có địa chỉ!", Toast.LENGTH_SHORT).show();
                    return true;
                }

                // Sao chép địa chỉ vào clipboard
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("address", address);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(PropertyDetailActivity.this, "Đã sao chép địa chỉ", Toast.LENGTH_SHORT).show();

                // Mở Google Maps tìm kiếm địa chỉ
                Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(address));
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");

                // Kiểm tra xem có ứng dụng Google Maps không
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    Toast.makeText(PropertyDetailActivity.this, "Không tìm thấy ứng dụng Google Maps", Toast.LENGTH_SHORT).show();
                }

                return true;
            }
        });

    }

    // Hiển thị layout xác nhận xóa
    private void showDeleteConfirmDialog() {
        // Hiển thị layout xác nhận xóa
        binding.deleteConfirmLayout.getRoot().setVisibility(View.VISIBLE);
    }

    private void hideDeleteConfirmDialog() {
        binding.deleteConfirmLayout.getRoot().setVisibility(View.GONE);
    }

    private void deleteProperty() {
        Log.d(TAG, "deleteProperty: Bắt đầu xóa property với ID: " + propertyId);

        // Hiển thị progress dialog
        progressDialog.setMessage("Đang xóa bài đăng...");
        progressDialog.show();

        // Xóa property từ Firebase Database
        DatabaseReference propertyRef = FirebaseDatabase.getInstance().getReference("Properties");
        propertyRef.child(propertyId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "deleteProperty: Xóa property thành công");

                    // Xóa property khỏi danh sách favorites của tất cả users
                    removePropertyFromAllFavorites();

                    // Xóa ảnh từ Firebase Storage (nếu có)
                    deletePropertyImages();

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "deleteProperty: Lỗi khi xóa property", e);
                    progressDialog.dismiss();
                    MyUtils.toast(PropertyDetailActivity.this, "Lỗi khi xóa bài đăng: " + e.getMessage());
                });
    }

    private void removePropertyFromAllFavorites() {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    if (userId != null) {
                        // Xóa property khỏi favorites của user này
                        DatabaseReference favRef = usersRef.child(userId).child("Favorites").child(propertyId);
                        favRef.removeValue();
                    }
                }
                Log.d(TAG, "removePropertyFromAllFavorites: Đã xóa khỏi tất cả favorites");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "removePropertyFromAllFavorites: Lỗi", error.toException());
            }
        });
    }

    private void deletePropertyImages() {
        // Nếu bạn lưu ảnh trên Firebase Storage, thêm code xóa ảnh ở đây
        // Ví dụ:
    /*
    DatabaseReference imageRef = FirebaseDatabase.getInstance().getReference("Properties");
    imageRef.child(propertyId).child("Images")
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String imageUrl = "" + ds.child("imageUrl").getValue();
                        // Xóa ảnh từ Firebase Storage
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
                            imageRef.delete();
                        }
                    }
                    onDeleteComplete();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "deletePropertyImages: Lỗi", error.toException());
                    onDeleteComplete();
                }
            });
    */

        // Nếu không dùng Firebase Storage, gọi trực tiếp
        onDeleteComplete();
    }

    private void onDeleteComplete() {
        progressDialog.dismiss();
        hideDeleteConfirmDialog();
        MyUtils.toast(PropertyDetailActivity.this, "Đã xóa bài đăng thành công!");

        // Quay về màn hình trước
        finish();
    }

    private void checkIsFavorite(String propertyId) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).child("Favorites").child(propertyId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        isFavorite = snapshot.exists();

                        if (isFavorite) {
                            binding.favBtn.setImageResource(R.drawable.fav_red);
                        } else {
                            binding.favBtn.setImageResource(R.drawable.un_fav_black);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(PropertyDetailActivity.this, "Lỗi kiểm tra yêu thích", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadMyProperty() {
        String propertyId = getIntent().getStringExtra("propertyId");

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Properties");
        ref.child(propertyId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String propertyId = getIntent().getStringExtra("propertyId");

                if (propertyId == null || propertyId.isEmpty()) {
                    Toast.makeText(PropertyDetailActivity.this, "Không có ID bất động sản", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                Log.d(TAG, "loadMyProperty: propertyId = " + propertyId);

                // Property data
                String title = "" + snapshot.child("title").getValue();
                String address = "" + snapshot.child("address").getValue();
                Double price = snapshot.child("price").getValue(Double.class);
                String timestamp = "" + snapshot.child("timestamp").getValue();
                String purpose = "" + snapshot.child("purpose").getValue();
                String category = "" + snapshot.child("category").getValue();
                String subcategory = "" + snapshot.child("subcategory").getValue();
                String floors = "" + snapshot.child("floors").getValue();
                String bedrooms = "" + snapshot.child("bedRooms").getValue();
                String bathrooms = "" + snapshot.child("bathRooms").getValue();
                String areaSize = "" + snapshot.child("areaSize").getValue();
                String areaSizeUnit = "" + snapshot.child("areaSizeUnit").getValue();
                String uid = "" + snapshot.child("uid").getValue();
                String description = "" + snapshot.child("description").getValue(); // Thêm description

                // Lưu uid của property owner để kiểm tra quyền xóa
                currentPropertyUid = uid;

                // Ẩn/hiện nút xóa và edit dựa trên quyền sở hữu
                if (firebaseAuth.getCurrentUser() != null &&
                        firebaseAuth.getUid().equals(currentPropertyUid)) {
                    binding.deleteBtn.setVisibility(View.VISIBLE);
                    binding.editBtn.setVisibility(View.VISIBLE);
                } else {
                    binding.deleteBtn.setVisibility(View.GONE);
                    binding.editBtn.setVisibility(View.GONE);
                }

                String titleAddress = "Địa chỉ: ";
                String contentAddress = address;

                if (price == null) price = 0.0;

                String formatDate = MyUtils.formatTimestampDate(Long.parseLong(timestamp));
                String formatPrice = MyUtils.formatCurrency(price);

                SpannableString spannableTitle = new SpannableString(titleAddress);
                spannableTitle.setSpan(new StyleSpan(Typeface.BOLD), 0, titleAddress.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannableTitle.setSpan(new ForegroundColorSpan(Color.BLACK), 0, titleAddress.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                SpannableString spannableContent = new SpannableString(contentAddress);

                SpannableStringBuilder builder = new SpannableStringBuilder();
                builder.append(spannableTitle);
                builder.append(spannableContent);

                // Set property UI
                binding.titleTv.setText(title);
                binding.addressEt.setText(builder);
                binding.priceEt.setText(formatPrice + "đ");
                binding.dateEt.setText(formatDate);
                binding.purposeEt.setText(purpose);
                binding.categoryEt.setText(category);
                binding.subcategoryEt.setText(subcategory);
                binding.floorEt.setText("Tầng: " + floors);
                binding.bedRoomEt.setText("Phòng ngủ: " + bedrooms);
                binding.bathRoomEt.setText("Phòng tắm: " + bathrooms);
                binding.areaSizeEt.setText("Diện tích: " + areaSize + areaSizeUnit);
                binding.descriptionEt.setText(description); // Set description

                loadPropertyMainImage(propertyId);
                loadSellerDetail(uid);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PropertyDetailActivity.this, "Lỗi khi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPropertyMainImage(String propertyId) {
        DatabaseReference imageRef = FirebaseDatabase.getInstance().getReference("Properties");
        imageRef.child(propertyId).child("Images")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String imageUrl = "" + ds.child("imageUrl").getValue();
                            Glide.with(PropertyDetailActivity.this)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.img6)
                                    .into(binding.propertyIv);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Lỗi tải ảnh", error.toException());
                    }
                });
    }


    private void loadSellerDetail(String uid) {
        DatabaseReference uidRef = FirebaseDatabase.getInstance().getReference("Users");
        uidRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String fullName = "" + snapshot.child("name").getValue();
                String phoneNumber = "" + snapshot.child("phoneNumber").getValue();
                String imageSeller = "" + snapshot.child("profileImageUrl").getValue();

                Log.d(TAG, "onDataChange: Ảnh người bán: " + imageSeller);

                String formatPhoneNumber = MyUtils.formatPhoneNumber(phoneNumber);

                sellerPhoneNumber = phoneNumber;

                binding.fullNameEt.setText(fullName);
                binding.phoneEt.setText(formatPhoneNumber);

                try {
                    Glide.with(PropertyDetailActivity.this)
                            .load(imageSeller)
                            .placeholder(R.drawable.person_black)
                            .into(binding.avtIv);
                } catch (Exception e) {
                    Log.e(TAG, "onDataChange: ", e);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PropertyDetailActivity.this,
                        "Không thể tải thông tin người bán", Toast.LENGTH_SHORT).show();
            }
        });
    }
}