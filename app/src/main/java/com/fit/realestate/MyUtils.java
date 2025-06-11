package com.fit.realestate;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.text.format.DateFormat;
import android.widget.Toast;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.HashMap;

public class MyUtils {
    public static final String AD_STATUS_AVAILABLE = "AVAILABLE";
    public static final String AD_STATUS_SOLD = "SOLD";
    public static final String AD_STATUS_RENTED = "RENTED";

    // Constants
    public static final String USER_TYPE_GOOGLE = "Google";
    public static final String USER_TYPE_EMAIL = "Email";
    public static final String USER_TYPE_PHONE = "Phone";

    public static final int MAX_DISTANCE_TO_LOAD_PROPERTIES = 10;


    public static final String[] propertyTypes = {"Nhà ở", "Lô đất", "Thương mại - Dịch vụ"};

    public static final String[] propertyTypesHomes = {
            "Nhà riêng",
            "Nhà mặt phố",
            "Biệt thự",
            "Nhà liền kề",
            "Nhà cấp 4",
            "Nhà có gác",
            "Nhà phố thương mại",
            "Phòng trọ",
            "Penthouse",
            "Shophouse",
            "Căn hộ chung cư",
            "Căn hộ ven biển",
            "Căn hộ mini",
            "Căn hộ dịch vụ",
            "Căn hộ kinh doanh",
            "Khác"
    };

    public static final String[] propertyTypesPlots = {
            "Đất thổ cư",
            "Đất tái định cư",
            "Đất mặt tiền",
            "Đất nền dự án",
            "Đất thương mại, dịch vụ",
            "Đất lâm nghiệp",
            "Đất nông nghiệp",
            "Đất công nghiệp",
            "Đất khu công nghiệp",
            "Khác"
    };

    public static final String[] propertyTypesCommercial = {
            "Shop",
            "Ki-ốt",
            "Quán ăn",
            "Quán cafe",
            "Nhà hàng",
            "Nhà kho",
            "Nhà xưởng",
            "Tòa nhà",
            "Văn phòng",
            "Phòng trưng bày",
            "Mặt bằng kinh doanh",
            "Phòng khám",
            "Trung tâm y tế",
            "Khác"
    };

    public static final String[] propertyAreaSizeUnit = {
            "ha",
            "km²",
            "m²",
            "ft²",
            "thước",
            "sào",
            "mẫu"
    };

    public static final String PROPERTY_PURPOSE_ANY = "Any";
    public static final String PROPERTY_PURPOSE_SELL = "Sell";
    public static final String PROPERTY_PURPOSE_RENT = "Rent";



    public static void toast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static long timestamp() {
        return System.currentTimeMillis();
    }

    public static String formatTimestampDate(Long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);

        String date = DateFormat.format("dd/MM/yyyy", calendar).toString();

        return date;
    }

    public static String formatCurrency(Double price) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance();

        numberFormat.setMaximumFractionDigits(2);

        return numberFormat.format(price);
    }

    public static double caculateDistanceKm(
            double currentLatitude, double currentLongitude,
            double propertyLatitude, double propertyLongitude) {
        Location startPoint = new Location(LocationManager.NETWORK_PROVIDER);
        startPoint.setLatitude(currentLatitude);
        startPoint.setLongitude(currentLongitude);

        Location endPoint = new Location(LocationManager.NETWORK_PROVIDER);
        endPoint.setLatitude(propertyLatitude);
        endPoint.setLongitude(propertyLongitude);

        double distanceInMeters = startPoint.distanceTo(endPoint);
        double distanceInKm = distanceInMeters/1000;

        return distanceInKm;
    }

    public static void addToFavorite(Context context, String propertyId) {
//        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
//
//        if (firebaseAuth.getCurrentUser() == null) {
//            MyUtils.toast(context, "You're not logged-in!");
//        } else {
//            long timestamp = MyUtils.timestamp();
//
//            HashMap<String, Object> hashMap = new HashMap<>();
//            hashMap.put("propertyId", propertyId);
//            hashMap.put("timestamp", timestamp);
//
//            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
//            ref.child(firebaseAuth.getUid()).child("Favorities").child(propertyId)
//                    .setValue(hashMap)
//                    .addOnSuccessListener(new OnSuccessListener<Void>() {
//                        @Override
//                        public void onSuccess(Void unused) {
//                            MyUtils.toast(context, "Added to favorite...!");
//                        }
//                    })
//                    .addOnFailureListener(new OnFailureListener() {
//                        @Override
//                        public void onFailure(@NonNull Exception e) {
//                            MyUtils.toast(context, "Failed to add to favorite due to " + e.getMessage());
//                        }
//                    });
//        }
    }

    public static void removeFromFavorite(Context context, String propertyId) {
//        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
//
//        if (firebaseAuth.getCurrentUser() == null) {
//            MyUtils.toast(context, "You're not logged-in!");
//        } else {
//            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
//            ref.child(firebaseAuth.getUid()).child("Favorities").child(propertyId)
//                    .removeValue()
//                    .addOnSuccessListener(new OnSuccessListener<Void>() {
//                        @Override
//                        public void onSuccess(Void unused) {
//                            MyUtils.toast(context, "Removed from favorites...!");
//                        }
//                    })
//                    .addOnFailureListener(new OnFailureListener() {
//                        @Override
//                        public void onFailure(@NonNull Exception e) {
//                            MyUtils.toast(context, "Failed to remove from favorites due to " + e.getMessage());
//                        }
//                    });
//        }



    }

    public static String formatPhoneNumber(String phoneNumber) {
        // Đảm bảo số chỉ chứa chữ số
        if (phoneNumber == null) return "";

        phoneNumber = phoneNumber.replaceAll("[^\\d]", ""); // Xóa ký tự không phải số

        // Kiểm tra độ dài tối thiểu
        if (phoneNumber.length() < 9) return phoneNumber;

        // Định dạng: XXX.XXXX.XXX (áp dụng cho số 10 chữ số)
        if (phoneNumber.length() == 10) {
            return phoneNumber.replaceFirst("(\\d{3})(\\d{3})(\\d{4})", "$1.$2.$3");
        }
        // Định dạng: XXX.XXX.XXX (áp dụng cho số 10 chữ số)
        else if (phoneNumber.length() == 9) {
            return phoneNumber.replaceFirst("(\\d{3})(\\d{3})(\\d{3})", "$1.$2.$3");
        }
        // Định dạng: XXX.XXXX.XXXX (áp dụng cho số 10 chữ số)
        else if (phoneNumber.length() == 11) {
            return phoneNumber.replaceFirst("(\\d{3})(\\d{4})(\\d{4})", "$1.$2.$3");
        }

        // Các trường hợp khác trả về nguyên gốc
        return phoneNumber;
    }


}
