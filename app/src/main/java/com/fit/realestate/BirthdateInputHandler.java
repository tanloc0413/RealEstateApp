package com.fit.realestate;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.widget.EditText;
import android.os.Handler;

public class BirthdateInputHandler {

    public static void setupDateInput(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting;
            private String current = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;

                isFormatting = true;

                String raw = s.toString().replaceAll("[^\\d]", "");

                StringBuilder result = new StringBuilder();

                int rawLength = raw.length();
                int sel = rawLength;

                for (int i = 0; i < rawLength && i < 8; i++) {
                    result.append(raw.charAt(i));
                    // Tự động thêm / sau 2 và 4 ký tự
                    if ((i == 1 || i == 3) && i != rawLength - 1) {
                        result.append("/");
                    }
                }

                current = result.toString();

                editText.removeTextChangedListener(this);
                editText.setText(current);
                // Đặt con trỏ cuối chuỗi (xử lý xóa nhanh và chính xác hơn)
                editText.setSelection(current.length());
                editText.addTextChangedListener(this);

                isFormatting = false;
            }
        });
    }

    public static void setupLongDelete(EditText editText) {
        final Handler handler = new Handler();
        final long[] startTime = {0};
        final boolean[] isDeleting = {false};

        final Runnable clearTextRunnable = new Runnable() {
            @Override
            public void run() {
                if (isDeleting[0]) {
                    editText.setText(""); // Xóa toàn bộ
                    isDeleting[0] = false; // Chặn xóa tiếp
                }
            }
        };

        editText.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DEL) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (!isDeleting[0]) {
                        isDeleting[0] = true;
                        startTime[0] = System.currentTimeMillis();
                        handler.postDelayed(clearTextRunnable, 400); // Đợi 400ms rồi mới xóa
                    }
                } else if (event.getAction() == KeyEvent.ACTION_UP) {
                    handler.removeCallbacks(clearTextRunnable);
                    isDeleting[0] = false;
                }
            }
            return false;
        });
    }
}
