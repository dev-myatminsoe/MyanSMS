package me.myatminsoe.myansms;

import android.content.Context;
import android.support.v7.widget.AppCompatImageButton;
import android.text.Editable;
import android.text.TextWatcher;

public final class MyTextWatcher implements TextWatcher {

    private static final String TAG = "TextWatcher";

    /**
     * Minimum length for showing sms length.
     */
    private static final int TEXT_LABLE_MIN_LEN = 50;

    private final Context context;

    private final AppCompatImageButton btn;

    /**
     * Constructor.
     *
     * @param ctx   {@link Context}
     * @param btn {@link AppCompatImageButton} holding "send" button
     */
    public MyTextWatcher(final Context ctx, final AppCompatImageButton btn) {
        context = ctx;
        this.btn = btn;
    }

    /**
     * {@inheritDoc}
     */
    public void afterTextChanged(final Editable s) {
        final int len = s.length();
        if (len == 0) {
            btn.setImageResource(R.drawable.icn_send_disabled);
        } else {
            btn.setImageResource(R.drawable.icn_send_enabled);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void beforeTextChanged(final CharSequence s, final int start, final int count,
                                  final int after) {
    }

    /**
     * {@inheritDoc}
     */
    public void onTextChanged(final CharSequence s, final int start, final int before,
                              final int count) {
    }
}
