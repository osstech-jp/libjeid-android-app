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
    boolean isShowingDialog = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_rc_reader);
        super.onCreate(savedInstanceState);

        rcNumber = (EditText)findViewById(R.id.edit_rc_number);
    }

    @Override
    public void onTagDiscovered(final Tag tag) {
        Log.d(TAG, getClass().getSimpleName() + "#onTagDiscovered()");
        if (isShowingDialog) {
            Log.d(TAG, getClass().getSimpleName() + "showing dialog");
            return;
        }
        RCReaderTask task = new RCReaderTask(this, tag);
        task.execute();
    }

    protected void showInvalidPinDialog(String title, String msg) {
        Log.d(TAG, getClass().getSimpleName() + "#showInvalidPinDialog()");
        isShowingDialog = true;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setNeutralButton(
            "戻る",
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    isShowingDialog = false;
                }
            });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    protected String getRcNumber() {
        return rcNumber.getText().toString();
    }
}
