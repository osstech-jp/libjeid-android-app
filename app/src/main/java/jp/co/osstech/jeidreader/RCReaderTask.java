package jp.co.osstech.jeidreader;

import android.content.Intent;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;

import jp.co.osstech.libjeid.CardType;
import jp.co.osstech.libjeid.InvalidBACKeyException;
import jp.co.osstech.libjeid.JeidReader;
import jp.co.osstech.libjeid.ResidenceCardAP;
import jp.co.osstech.libjeid.RCCommonData;
import jp.co.osstech.libjeid.RCCardType;


public class RCReaderTask extends AsyncTask<Void, String, JSONObject>
{
    private static final String TAG = MainActivity.TAG;
    private WeakReference mRef;
    private Tag mNfcTag;
    private String rcNumber;

    public RCReaderTask(RCReaderActivity activity, Tag nfcTag) {
        mRef = new WeakReference<RCReaderActivity>(activity);
        mNfcTag = nfcTag;
    }

    @Override
    protected void onPreExecute() {
        RCReaderActivity activity = (RCReaderActivity)mRef.get();
        if (activity == null) {
            return;
        }
        rcNumber = activity.getRcNumber();

        activity.hideKeyboard();
        activity.setMessage("# 読み取り開始、カードを離さないでください");
    }

    @Override
    protected JSONObject doInBackground(Void... args) {
        Log.d(TAG, getClass().getSimpleName() + "#doInBackground()");

        if (rcNumber.isEmpty()) {
            publishProgress("在留カード番号または特別永住者証明書番号を設定してください");
            return null;
        }

        long start = System.currentTimeMillis();
        JeidReader reader;
        try {
            reader = new JeidReader(mNfcTag);
        } catch (IOException e) {
            Log.e(TAG, "error", e);
            publishProgress("エラー: " + e);
            return null;
        }

        publishProgress("## 在留カードまたは特別永住者証明書の読み取り");
        try {
            CardType type = reader.detectCardType();
            publishProgress("CardType: " + type);
            if (type != CardType.RC) {
                publishProgress("在留カード等ではありません");
                return null;
            }
            ResidenceCardAP ap = reader.selectResidenceCardAP();
            RCCommonData commonData = ap.readCommonData();
            publishProgress("commonData: " + commonData);
            RCCardType cardType = ap.readCardType();
            publishProgress("cardType: " + cardType);
            /*
            publishProgress("Basic Access Control開始");
            try {
                ap.startBAC(mrz);
            } catch (InvalidBACKeyException e) {
                publishProgress("Basic Access Control失敗\n"
                        + "在留カード等番号または有効期限が間違っています");
                return null;
            }
            publishProgress("Basic Access Control完了");
            */
            JSONObject obj = new JSONObject();
            return obj;
        } catch (Exception e) {
            Log.e(TAG, "error", e);
            publishProgress("エラー: " + e);
            return null;
        }
    }

    @Override
    protected void onProgressUpdate(String... values) {
        Log.d(TAG, getClass().getSimpleName() + "#onProgressUpdate()");
        RCReaderActivity activity = (RCReaderActivity)mRef.get();
        if (activity == null) {
            return;
        }
        activity.addMessage(values[0]);
    }

    @Override
    protected void onPostExecute(JSONObject obj) {
        Log.d(TAG, getClass().getSimpleName() + "#onPostExecute()");
        RCReaderActivity activity = (RCReaderActivity)mRef.get();
        if (activity == null) {
            return;
        }

        if (obj == null) {
            return;
        }
        Intent intent = new Intent(activity, RCViewerActivity.class);
        intent.putExtra("json", obj.toString());
        activity.startActivity(intent);
    }
}
