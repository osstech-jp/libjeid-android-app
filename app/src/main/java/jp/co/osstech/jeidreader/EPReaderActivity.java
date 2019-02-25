package jp.co.osstech.jeidreader;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;

public class EPReaderActivity
    extends BaseActivity
{
    EditText passportNumber;
    EditText birthDate;
    EditText expireDate;
    boolean isShowingDialog = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_ep_reader);
        super.onCreate(savedInstanceState);

        passportNumber = (EditText)findViewById(R.id.edit_ep_passport_number);
        birthDate = (EditText)findViewById(R.id.edit_ep_birth_date);
        expireDate = (EditText)findViewById(R.id.edit_ep_expire_date);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, getClass().getSimpleName() + "#onNewIntent()");
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (isShowingDialog) {
            Log.d(TAG, getClass().getSimpleName() + "showing dialog");
            return;
        }
        EPReaderTask task = new EPReaderTask(this, tag);
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

    protected String getPassportNumber() {
        return passportNumber.getText().toString();
    }

    protected String getBirthDate() {
        return birthDate.getText().toString();
    }

    protected String getExpireDate() {
        return expireDate.getText().toString();
    }
}
