package jp.co.osstech.jeidreader;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;

public class PinStatusActivity
    extends BaseActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, getClass().getSimpleName() +
              "#onCreate(" + savedInstanceState + ")");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pinstatus);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, getClass().getSimpleName() + "#onNewIntent()");
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        PinStatusTask task = new PinStatusTask(this, tag);
        task.execute();

    }

}
