package jp.co.osstech.jeidreader;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

public class ShowCertActivity
    extends BaseActivity
{
    private String mType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_cert);

        TextView textView = (TextView)findViewById(R.id.message);
        TextView info = (TextView)findViewById(R.id.info);
        this.enableNFC = true;
        Intent intent = getIntent();
        mType = intent.getStringExtra("TYPE");
        switch (mType) {
        case "AUTH":
            info.setText(getString(R.string.show_auth_cert) + "を表示します。");
            break;
        case "SIGN":
            info.setText(getString(R.string.show_sign_cert) + "を表示します。");
            TextView textPin = (TextView)findViewById(R.id.text_pin);
            textPin.setVisibility(View.VISIBLE);
            EditText editPin = (EditText)findViewById(R.id.edit_pin);
            editPin.setVisibility(View.VISIBLE);
            break;
        case "AUTH_CA":
            info.setText(getString(R.string.show_auth_ca_cert) + "を表示します。");
            break;
        case "SIGN_CA":
            info.setText(getString(R.string.show_sign_ca_cert) + "を表示します。");
            break;
        default:
            Log.e(TAG, "Unknown type");
            finish();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, getClass().getSimpleName() + "#onNewIntent()");
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        this.onTagDiscovered(tag);
    }

    @Override
    public void onTagDiscovered(final Tag tag) {
        Log.d(TAG, getClass().getSimpleName() + "#onTagDiscovered()");
        if (!this.enableNFC) {
            Log.d(TAG, getClass().getSimpleName() + ": NFC disabled.");
            return;
        }
        ShowCertTask task = new ShowCertTask(this, tag, mType);
        task.execute();
    }

    protected void showInvalidPinDialog(String title, String msg) {
        Log.d(TAG, getClass().getSimpleName() + "#showInvalidPinDialog()");
        this.enableNFC = false;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setNeutralButton(
                "戻る",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        enableNFC = true;
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    protected String getPassword() {
        EditText edit = (EditText)findViewById(R.id.edit_pin);
        return edit.getText().toString();
    }

    protected void setMessage(String message) {
        TextView text = (TextView)findViewById(R.id.message);
        text.setText(message);
    }

    protected void addMessage(String message) {
        TextView text = (TextView)findViewById(R.id.message);
        text.setText(text.getText().toString() + "\n" + message);
        // 一番下にスクロール
        final ScrollView scroll = (ScrollView)findViewById(R.id.scroll);
        scroll.post(new Runnable() {
                public void run() {
                    scroll.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
    }
}

