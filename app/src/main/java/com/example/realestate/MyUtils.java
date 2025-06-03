package com.example.realestate;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.text.format.DateFormat;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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


    public static final String[] propertyTypes = {"Homes", "Plots", "Commercial"};
    public static final String[] propertyTypesHomes = {
            "House",
            "Flats",
            "Upper Portion",
            "Lower Portion",
            "Farm House",
            "Room",
            "Penthouse"
    };
    public static final String[] propertyTypesPlots = {
            "Residential Plot",
            "Commercial Plot",
            "Agricultural Plot",
            "Industrial Plot",
            "Plot File",
            "Plot Form"
    };
    public static final String[] propertyTypesCommercial = {
            "Office",
            "Shop",
            "Warehouse",
            "Factory",
            "Building",
            "Other"
    };

    public static final String[] propertyAreaSizeUnit = {
            "ha",
            "km²",
            "m²",
            "ft²",
            "Building",
            "Other"
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
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

        if (firebaseAuth.getCurrentUser() == null) {
            MyUtils.toast(context, "You're not logged-in!");
        } else {
            long timestamp = MyUtils.timestamp();

            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("propertyId", propertyId);
            hashMap.put("timestamp", timestamp);

            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
            ref.child(firebaseAuth.getUid()).child("Favorities").child(propertyId)
                    .setValue(hashMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            MyUtils.toast(context, "Added to favorite...!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            MyUtils.toast(context, "Failed to add to favorite due to " + e.getMessage());
                        }
                    });
        }
    }

    public static void removeFromFavorite(Context context, String propertyId) {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

        if (firebaseAuth.getCurrentUser() == null) {
            MyUtils.toast(context, "You're not logged-in!");
        } else {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
            ref.child(firebaseAuth.getUid()).child("Favorities").child(propertyId)
                    .removeValue()
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            MyUtils.toast(context, "Removed from favorites...!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            MyUtils.toast(context, "Failed to remove from favorites due to " + e.getMessage());
                        }
                    });
        }
    }
}
