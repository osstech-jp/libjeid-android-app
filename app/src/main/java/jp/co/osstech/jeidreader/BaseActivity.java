package jp.co.osstech.jeidreader;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcB;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public abstract class BaseActivity
    extends AppCompatActivity
{
    public static final String TAG = "JeidReader";

    protected NfcAdapter mNfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, getClass().getSimpleName() +
              "#onCreate(" + savedInstanceState + ")");
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, getClass().getSimpleName() + "#onResume()");
        super.onResume();

        invalidateOptionsMenu();

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            return;
        }
        Intent intent = new Intent(this, this.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
            getApplicationContext(), 0, intent, 0);
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndef.addDataType("*/*");
        } catch (MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }
        IntentFilter[] filters = new IntentFilter[] {
            ndef,
        };
        String[][] techLists = new String[][] {
            new String[] { NfcB.class.getName() },
            new String[] { IsoDep.class.getName() }
        };
        //mNfcAdapter.enableForegroundDispatch(this, pendingIntent, filters, techLists);
        mNfcAdapter.enableForegroundDispatch(this, pendingIntent, null, techLists);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, getClass().getSimpleName() + "#onPause()");
        super.onPause();
        if (mNfcAdapter == null) {
            return;
        }
        mNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, getClass().getSimpleName() + "#onCreateOptionsMenu()");
        getMenuInflater().inflate(R.menu.main, menu);
        if (mNfcAdapter == null) {
            menu.getItem(0).setIcon(ContextCompat.getDrawable(this, R.drawable.ic_nfc_no));
        } else {
            if (mNfcAdapter.isEnabled()) {
                menu.getItem(0).setIcon(ContextCompat.getDrawable(this, R.drawable.ic_nfc_on));
            } else {
                menu.getItem(0).setIcon(ContextCompat.getDrawable(this, R.drawable.ic_nfc_off));
            }

        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
        case R.id.menu_nfc_settings:
            // Android 4.1より前はNFC設定が無線の設定項目にある
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                intent = new Intent(Settings.ACTION_NFC_SETTINGS);
            } else {
                intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
            }
            startActivity(intent);
            break;
        case R.id.menu_about:
            StringBuilder sb = new StringBuilder();
            sb.append("libjeid: " + jp.co.osstech.libjeid.BuildConfig.VERSION_NAME + "\n");
            sb.append("\n");
            sb.append("Developed by OSSTech\n");
            new AlertDialog.Builder(this)
                .setTitle("IDリーダー " + BuildConfig.VERSION_NAME)
                .setMessage(sb.toString())
                .setPositiveButton("閉じる", null)
                .show();
            break;
        }
        return true;
    }

    protected void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) {
            return;
        }
        View view = getCurrentFocus();
        if (view == null) {
            return;
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    protected void setMessage(String message) {
        TextView view = (TextView)findViewById(R.id.message);
        view.setText(message);
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
