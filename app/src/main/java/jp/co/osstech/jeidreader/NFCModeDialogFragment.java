package jp.co.osstech.jeidreader;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;
import android.os.Bundle;
import android.content.SharedPreferences;
import androidx.fragment.app.DialogFragment;

public class NFCModeDialogFragment
    extends DialogFragment {
    SharedPreferences prefs;
    int selectedMode;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        prefs = getContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
        int nfcMode = prefs.getInt("nfc_mode", 0);
        String[] items = {
            "ForegroundDispatch",
            "ReaderMode"
        };
        return new AlertDialog.Builder(getActivity())
            .setTitle("NFCモード設定")
            .setPositiveButton("適用", new ApplyClickListener())
            .setNegativeButton("キャンセル", null)
            .setSingleChoiceItems(items,
                                  nfcMode,
                                  new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectedMode = which;
                    }
                })
            .create();
    }
    class ApplyClickListener
        implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("nfc_mode", selectedMode);
            editor.commit();
            // 設定を反映するためActivityを再起動
            Intent intent = getActivity().getIntent();
            getActivity().finish();
            startActivity(intent);
        }
    }
}
