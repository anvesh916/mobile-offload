package com.nebuxe.mobileoffloading.views;

import android.app.Dialog;
import android.content.Context;
import android.view.Window;

import com.nebuxe.mobileoffloading.R;


public class DialogView {
    private Context context;

    public DialogView(Context context) {
        this.context = context;
    }

    public Dialog getDialog(int layout, boolean cancelable) {
        Dialog dialog = getDialog(layout);
        dialog.setCancelable(cancelable);
        return dialog;
    }

    public Dialog getDialog(int layout) {
        Dialog dialog = new Dialog(context);

        dialog.setCancelable(false);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(layout);

        try {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        return dialog;
    }

}