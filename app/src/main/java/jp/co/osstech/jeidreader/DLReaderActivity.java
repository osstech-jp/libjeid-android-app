package jp.co.osstech.jeidreader;

import android.app.AlertDialog;
import android.content.DialogInterface;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;

public class DLReaderActivity
    extends BaseActivity
{
    EditText editPin1;
    EditText editPin2;
    boolean isShowingDialog = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_dl_reader);
        super.onCreate(savedInstanceState);

        editPin1 = (EditText)findViewById(R.id.edit_dl_pin1);
        editPin2 = (EditText)findViewById(R.id.edit_dl_pin2);
    }

    @Override
    public void onTagDiscovered(final Tag tag) {
        Log.d(TAG, getClass().getSimpleName() + "#onTagDiscovered()");
        if (isShowingDialog) {
            Log.d(TAG, getClass().getSimpleName() + "showing dialog");
            return;
        }
        DLReaderTask task = new DLReaderTask(this, tag);
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

    protected String getPin1() {
        return editPin1.getText().toString();
    }

    protected String getPin2() {
        return editPin2.getText().toString();
    }

}
