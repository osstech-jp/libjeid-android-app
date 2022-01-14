package jp.co.osstech.jeidreader;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;

public class RCReaderActivity
    extends BaseActivity
{
    EditText rcNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_rc_reader);
        super.onCreate(savedInstanceState);
        this.enableNFC = true;
        rcNumber = (EditText)findViewById(R.id.edit_rc_number);
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
        RCReaderTask task = new RCReaderTask(this, tag);
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

    protected String getRcNumber() {
        return rcNumber.getText().toString();
    }
}
