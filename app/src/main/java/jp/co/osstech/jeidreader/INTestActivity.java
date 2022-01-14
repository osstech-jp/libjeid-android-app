package jp.co.osstech.jeidreader;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;

public class INTestActivity
    extends BaseActivity
{

    EditText editAuthPin;
    EditText editSignPin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.enableNFC = true;
        setContentView(R.layout.activity_test);

        editAuthPin = (EditText)findViewById(R.id.edit_jpki_auth_pin);
        editSignPin = (EditText)findViewById(R.id.edit_jpki_sign_pin);
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
        INTestTask task = new INTestTask(this, tag);
        task.execute();
    }

    protected String getAuthPin() {
        return editAuthPin.getText().toString();
    }
    protected String getSignPin() {
        return editSignPin.getText().toString();
    }

}
