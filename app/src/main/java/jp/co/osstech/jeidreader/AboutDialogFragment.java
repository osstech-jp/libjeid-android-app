package jp.co.osstech.jeidreader;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.fragment.app.DialogFragment;

public class AboutDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity()
            .getLayoutInflater()
            .inflate(R.layout.about_dialog, null);

        ((TextView)view.findViewById(R.id.app_version))
            .setText(BuildConfig.VERSION_NAME);

        ((TextView)view.findViewById(R.id.libjeid_version))
            .setText(jp.co.osstech.libjeid.BuildConfig.VERSION_NAME);

        return new AlertDialog.Builder(getActivity())
            .setTitle("IDリーダー")
            .setView(view)
            .setPositiveButton("閉じる", null)
            .create();
    }
}
