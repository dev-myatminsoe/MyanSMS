/*
 * Copyright (C) 2009-2015 Felix Bechstein
 * 
 * This file is part of SMSdroid.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; If not, see <http://www.gnu.org/licenses/>.
 */
package me.myatminsoe.myansms;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MergeCursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import de.ub0r.android.lib.apis.Contact;

/**
 * Adapter for the list of {@link Conversation}s.
 *
 * @author flx
 */
public class MessageAdapter extends ResourceCursorAdapter {

    /**
     * Tag for logging.
     */
    static final String TAG = "msa";

    /**
     * General WHERE clause.
     */
    private static final String WHERE = "(" + Message.PROJECTION_JOIN[Message.INDEX_TYPE] + " != "
            + Message.SMS_DRAFT + " OR " + Message.PROJECTION_JOIN[Message.INDEX_TYPE]
            + " IS NULL)";

    /**
     * WHERE clause for drafts.
     */
    private static final String WHERE_DRAFT = "(" + Message.PROJECTION_SMS[Message.INDEX_THREADID]
            + " = ? AND " + Message.PROJECTION_SMS[Message.INDEX_TYPE] + " = " + Message.SMS_DRAFT
            + ")";
    // + " OR " + type + " = " + Message.SMS_PENDING;

    private final MessageListActivity mActivity;

    /**
     * Display Name (name if !=null, else address).
     */
    private String displayName = null;

    /**
     * Embed Zawgyi
     */
    private static boolean embedZawgyi = true;

    /**
     * Convert NCR.
     */
    private final boolean convertNCR;


    /**
     * View holder.
     */
    private static class ViewHolder {

        TextView tvBody;

        TextView tvDate;

        ImageView ivPhoto;

        View vRead, lBackground;

        LinearLayout lGravity;

        public Button btnDownload;

        public Button btnImport;
    }

    /**
     * Default Constructor.
     *
     * @param c {@link MessageListActivity}
     * @param u {@link Uri}
     */
    public MessageAdapter(final MessageListActivity c, final Uri u) {
        super(c, R.layout.messagelist_item, getCursor(c.getContentResolver(), u), true);
        mActivity = c;
        convertNCR = PreferencesActivity.decodeDecimalNCR(c);

        // Thread id
        int threadId = -1;
        if (u == null || u.getLastPathSegment() == null) {
            threadId = -1;
        } else {
            threadId = Integer.parseInt(u.getLastPathSegment());
        }
        final Conversation conv = Conversation.getConversation(c, threadId, false);

        // Address
        String address = null;

        //Name
        String name = null;
        if (conv == null) {
            address = null;
            name = null;
            displayName = null;
        } else {
            final Contact contact = conv.getContact();
            address = contact.getNumber();
            name = contact.getName();
            displayName = contact.getDisplayName();
        }

    }

    /**
     * Get the {@link Cursor}.
     *
     * @param cr {@link ContentResolver}
     * @param u  {@link Uri}
     * @return {@link Cursor}
     */
    private static Cursor getCursor(final ContentResolver cr, final Uri u) {
        final Cursor[] c = new Cursor[]{null, null};

        int tid = -1;
        try {
            tid = Integer.parseInt(u.getLastPathSegment());
        } catch (Exception e) {
        }

        try {

            c[0] = cr.query(u, Message.PROJECTION_JOIN, WHERE, null, null);
        } catch (NullPointerException e) {

            c[0] = null;
        } catch (SQLiteException e) {

        }

        final String[] sel = new String[]{String.valueOf(tid)};
        try {
            c[1] = cr.query(Uri.parse("content://sms/"), Message.PROJECTION_SMS, WHERE_DRAFT, sel,
                    Message.SORT_USD);
        } catch (NullPointerException e) {

            c[1] = null;
        } catch (SQLiteException e) {
            Log.e(TAG, "error getting drafts", e);
        }

        if (c[1] == null || c[1].getCount() == 0) {
            return c[0];
        }
        if (c[0] == null || c[0].getCount() == 0) {
            return c[1];
        }

        return new MergeCursor(c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void bindView(final View view, final Context context, final Cursor cursor) {
        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        final Message m = Message.getMessage(context, cursor);

        ViewHolder holder = (ViewHolder) view.getTag();
        if (holder == null) {
            holder = new ViewHolder();
            holder.tvBody = (TextView) view.findViewById(R.id.body);
            holder.tvDate = (TextView) view.findViewById(R.id.date);

            holder.lBackground = view.findViewById(R.id.layoutBackground);
            holder.lGravity = (LinearLayout) view.findViewById(R.id.layoutGravity);

            holder.ivPhoto = (ImageView) view.findViewById(R.id.picture);
            holder.vRead = view.findViewById(R.id.read);
            holder.btnDownload = (Button) view.findViewById(R.id.btn_download_msg);
            holder.btnImport = (Button) view.findViewById(R.id.btn_import_contact);
            view.setTag(holder);
        }

        int t = m.getType();

        String subject = m.getSubject();
        if (subject == null) {
            subject = "";
        } else {
            subject = ": " + subject;
        }
        // incoming / outgoing / pending
        boolean pending = false;

        switch (t) {
            case Message.SMS_DRAFT:
                // TODO case Message.SMS_PENDING:
                // case Message.MMS_DRAFT:
                pending = true;
            case Message.SMS_OUT: // handle drafts/pending here too
            case Message.MMS_OUT:
                holder.lGravity.setGravity(Gravity.RIGHT);
                holder.lBackground.setBackgroundResource(R.drawable.bubble_out);
                holder.lGravity.setPadding(myat.convertDpToPixel(30, context), myat.convertDpToPixel(4, context), myat.convertDpToPixel(16, context), myat.convertDpToPixel(4, context));
                holder.tvBody.setTextColor(Color.parseColor("#FFFFFF"));
                holder.tvDate.setTextColor(Color.parseColor("#F5F5F5"));

                break;
            case Message.SMS_IN:
            case Message.MMS_IN:
                holder.lGravity.setGravity(Gravity.LEFT);
                holder.lBackground.setBackgroundResource(R.drawable.bubble_in);
                holder.lGravity.setPadding(myat.convertDpToPixel(8, context), myat.convertDpToPixel(4, context), myat.convertDpToPixel(30, context), myat.convertDpToPixel(4, context));
                holder.tvBody.setTextColor(Color.parseColor("#000000"));
                holder.tvDate.setTextColor(Color.parseColor("#424242"));
            default:
                pending = false;
                break;
        }

        if (pending) {
            holder.lBackground.setBackgroundResource(R.drawable.bubble_out_pending);
        }

        final long time = m.getDate();
        holder.tvDate.setText(ConversationListActivity.getDate(context, time));

        // unread / read
        if (m.getRead() == 0) {
            holder.vRead.setVisibility(View.VISIBLE);
        } else {
            holder.vRead.setVisibility(View.INVISIBLE);
        }


        final Bitmap pic = m.getPicture();
        if (pic != null) {
            if (pic == Message.BITMAP_PLAY) {
                holder.ivPhoto.setImageResource(R.drawable.mms_play_btn);
            } else {
                holder.ivPhoto.setImageBitmap(pic);
            }
            holder.ivPhoto.setVisibility(View.VISIBLE);
            final Intent i = m.getContentIntent();
            holder.ivPhoto.setOnClickListener(SMSdroid.getOnClickStartActivity(context, i));
            holder.ivPhoto.setOnLongClickListener(m.getSaveAttachmentListener(mActivity));
        } else {
            holder.ivPhoto.setVisibility(View.GONE);
            holder.ivPhoto.setOnClickListener(null);
        }

        CharSequence text = m.getBody();
        if (text == null && pic == null) {
            final Button btn = holder.btnDownload;
            btn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(final View v) {
                    try {
                        Intent i = new Intent();
                        i.setClassName("com.android.mms",
                                "com.android.mms.transaction.TransactionService");
                        i.putExtra("uri", m.getUri().toString());
                        i.putExtra("type", 1);
                        ComponentName cn = context.startService(i);
                        if (cn != null) {
                            btn.setEnabled(false);
                            btn.setText(R.string.downloading_);
                        } else {
                            i = new Intent(Intent.ACTION_VIEW, Uri.parse(MessageListActivity.URI
                                    + m.getThreadId()));
                            context.startActivity(Intent.createChooser(i,
                                    context.getString(R.string.view_mms)));
                        }
                    } catch (SecurityException e) {
                        Log.e(TAG, "unable to start mms download", e);
                        Toast.makeText(context, R.string.error_start_mms_download,
                                Toast.LENGTH_LONG).show();
                    }
                }
            });
            holder.btnDownload.setVisibility(View.VISIBLE);
            holder.btnDownload.setEnabled(true);
        } else {
            holder.btnDownload.setVisibility(View.GONE);
        }
        if (text == null) {
            holder.tvBody.setVisibility(View.INVISIBLE);
            holder.btnImport.setVisibility(View.GONE);
        } else {
            if (convertNCR) {
                text = Converter.convertDecNCR2Char(text);
            }


            if (p.getBoolean("embed_zawgyi", true)) {
                Typeface tf = Typeface.createFromAsset(context.getAssets(), "zawgyi.ttf");
                holder.tvBody.setTypeface(tf);
            } else if (p.getBoolean("convert_font", false)) {
                if (myat.isTextZawGyiProbably(text.toString())) {
                    text = myat.zg2uni(text.toString());
                }
            }
            holder.tvBody.setText(text);
            String stext = text.toString();
            if (stext.contains("BEGIN:VCARD") && stext.contains("END:VCARD")) {
                final Button btn = holder.btnImport;
                btn.setVisibility(View.VISIBLE);
                btn.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        final Intent i = new Intent(Intent.ACTION_VIEW);
                        Uri uri = ContentUris
                                .withAppendedId(MessageProvider.CONTENT_URI, m.getId());
                        i.setDataAndType(uri, "text/x-vcard");
                        try {
                            context.startActivity(i);
                        } catch (ActivityNotFoundException e) {

                            Toast.makeText(context, "Activity not found: text/x-vcard",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
            } else {
                holder.btnImport.setVisibility(View.GONE);
            }
        }
    }
}
