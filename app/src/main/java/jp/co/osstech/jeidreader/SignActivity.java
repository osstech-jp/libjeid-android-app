package jp.co.osstech.jeidreader;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import jp.co.osstech.libjeid.JPKIAP;

public class SignActivity
    extends BaseActivity
{
    private int type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        type = intent.getIntExtra("TYPE", 0);
        if (type == JPKIAP.TYPE_AUTH) {
            setContentView(R.layout.activity_sign_jpki_auth);
        } else {
            setContentView(R.layout.activity_sign_jpki_sign);
        }
        TextView textView = (TextView)findViewById(R.id.message);
        EditText editPin = (EditText)findViewById(R.id.edit_pin);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, getClass().getSimpleName() + "#onNewIntent()");
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        SignTask task = new SignTask(this, tag);
        task.execute();
    }

    protected int getType() {
        return type;
    }

    protected String getPin() {
        EditText edit = (EditText)findViewById(R.id.edit_pin);
        return edit.getText().toString();
    }

    protected String getText() {
        TextView view = (TextView)findViewById(R.id.edit_text);
        return view.getText().toString();
    }

    protected String getSignAlgo() {
        Spinner spinner = (Spinner)findViewById(R.id.sign_algo_spinner);
        return spinner.getSelectedItem().toString();
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
