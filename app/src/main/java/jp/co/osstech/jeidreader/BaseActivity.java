package jp.co.osstech.jeidreader;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
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
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public abstract class BaseActivity
    extends AppCompatActivity
    implements NfcAdapter.ReaderCallback
{
    public static final String TAG = "JeidReader";

    protected NfcAdapter mNfcAdapter;
    protected int nfcMode = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, getClass().getSimpleName() +
              "#onCreate(" + savedInstanceState + ")");
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("settings", Context.MODE_PRIVATE);
        nfcMode = prefs.getInt("nfc_mode", 0);
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

        if (nfcMode == 0) {
            Bundle options = new Bundle();
            //options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 500);
            mNfcAdapter.enableReaderMode(this,
                                         (NfcAdapter.ReaderCallback)this,
                                         NfcAdapter.FLAG_READER_NFC_B | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                                         options);
        } else {
            Log.d(TAG, "NFC mode: ReaderMode");
            Intent intent = new Intent(this, this.getClass());
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
            String[][] techLists = new String[][] {
                new String[] { NfcB.class.getName() },
                new String[] { IsoDep.class.getName() }
            };
            Log.d(TAG, "NFC mode: ForegroundDispatch");
            mNfcAdapter.enableForegroundDispatch(this, pendingIntent, null, techLists);
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, getClass().getSimpleName() + "#onPause()");
        super.onPause();
        if (mNfcAdapter == null) {
            return;
        }
        if (nfcMode == 0) {
            mNfcAdapter.disableReaderMode(this);
        } else {
            mNfcAdapter.disableForegroundDispatch(this);
        }
    }

    // ビューアーやメニューのActivityでこれが呼ばれます
    // サブクラスの**ReaderActivityでは適時overrideします
    public void onTagDiscovered(final Tag tag) {
        Log.d(TAG, getClass().getSimpleName() + "#onTagDiscovered()");
        Toast.makeText(this, "ビューアを閉じてください", Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        // NFCステータスアイコンを切り替え
        if (mNfcAdapter == null) {
            menu.getItem(0).setIcon(ContextCompat.getDrawable(this, R.drawable.ic_nfc_no));
        } else {
            if (mNfcAdapter.isEnabled()) {
                menu.getItem(0).setIcon(ContextCompat.getDrawable(this, R.drawable.ic_nfc_on));
            } else {
                menu.getItem(0).setIcon(ContextCompat.getDrawable(this, R.drawable.ic_nfc_off));
            }
        }
        // NFC modeを表示
        if (nfcMode == 0) {
            menu.getItem(1).setTitle("R");
        } else {
            menu.getItem(1).setTitle("F");
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
        case R.id.menu_nfc_settings:
            // NFC設定画面を開きます
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                intent = new Intent(Settings.ACTION_NFC_SETTINGS);
            } else {
                // Android 4.1より前はNFC設定が無線の設定項目にある
                intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
            }
            startActivity(intent);
            break;
        case R.id.menu_nfc_mode:
            new NFCModeDialogFragment()
                .show(getSupportFragmentManager(), "nfc_mode");
            break;
        case R.id.menu_about:
            AboutDialogFragment dialog = new AboutDialogFragment();
            dialog.show(getSupportFragmentManager(), "about");
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
