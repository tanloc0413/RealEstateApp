package com.example.realestate;

import android.content.Context;
import android.text.format.DateFormat;
import android.widget.Toast;

import java.text.NumberFormat;
import java.util.Calendar;

public class MyUtils {
    public static final String AD_STATUS_AVAILABLE = "AVAILABLE";
    public static final String AD_STATUS_SOLD = "SOLD";
    public static final String AD_STATUS_RENTED = "RENTED";

    // Constants
    public static final String USER_TYPE_GOOGLE = "Google";
    public static final String USER_TYPE_EMAIL = "Email";
    public static final String USER_TYPE_PHONE = "Phone";


    //
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
}
