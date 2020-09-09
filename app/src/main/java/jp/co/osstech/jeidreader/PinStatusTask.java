package jp.co.osstech.jeidreader;

import android.nfc.Tag;
import android.os.AsyncTask;
import android.util.Log;

import java.lang.ref.WeakReference;

import jp.co.osstech.libjeid.CardType;
import jp.co.osstech.libjeid.DriverLicenseAP;
import jp.co.osstech.libjeid.INTextAP;
import jp.co.osstech.libjeid.INVisualAP;
import jp.co.osstech.libjeid.JPKIAP;
import jp.co.osstech.libjeid.JeidReader;
import jp.co.osstech.libjeid.ResidenceCardAP;
import jp.co.osstech.libjeid.rc.RCCardType;

public class PinStatusTask extends AsyncTask<Void, String, Exception>
{
    private static final String TAG = MainActivity.TAG;
    private WeakReference mRef;
    private Tag mNfcTag;
    private ProgressDialogFragment mProgress;

    public PinStatusTask(PinStatusActivity activity, Tag nfcTag) {
        mRef = new WeakReference<PinStatusActivity>(activity);
        mNfcTag = nfcTag;
    }

    @Override
    protected void onPreExecute() {
        PinStatusActivity activity = (PinStatusActivity)mRef.get();
        if (activity == null) {
            return;
        }
        activity.setMessage("");
        mProgress = new ProgressDialogFragment();
        mProgress.show(activity.getSupportFragmentManager(), "progress");
    }

    @Override
    protected Exception doInBackground(Void... args) {
        try {
            JeidReader reader = new JeidReader(mNfcTag);
            int counter;

            CardType type = reader.detectCardType();
            switch (type) {
            case IN:
                publishProgress("カード種別: マイナンバーカード");
                INTextAP textAP = reader.selectINTextAP();
                counter = textAP.getPin();
                publishProgress("券面入力補助AP 暗証番号: " + counter);
                counter = textAP.getPinA();
                publishProgress("券面入力補助AP 照合番号A: " + counter);
                counter = textAP.getPinB();
                publishProgress("券面入力補助AP 照合番号B: " + counter);

                INVisualAP visualAP = reader.selectINVisualAP();
                counter = visualAP.getPinA();
                publishProgress("券面AP 照合番号A: " + counter);
                counter = visualAP.getPinB();
                publishProgress("券面AP 照合番号B: " + counter);

                JPKIAP jpkiAP = reader.selectJPKIAP();
                counter = jpkiAP.getAuthPin();
                publishProgress("JPKI-AP ユーザー認証PIN: " + counter);
                counter = jpkiAP.getSignPin();
                publishProgress("JPKI-AP デジタル署名PIN: " + counter);
                return null;
            case DL:
                publishProgress("カード種別: 運転免許証");
                DriverLicenseAP dlAP = reader.selectDriverLicenseAP();
                counter = dlAP.getPin1();
                publishProgress("暗証番号1: " + counter);
                counter = dlAP.getPin2();
                publishProgress("暗証番号2: " + counter);
                break;
            case JUKI:
                publishProgress("カード種別: 住基カード");
                break;
            case EP:
                publishProgress("カード種別: パスポート");
                break;
            case RC:
                ResidenceCardAP rcAP = reader.selectResidenceCardAP();
                RCCardType rcCardType = rcAP.readCardType();
                if (rcCardType.getType().equals("1")) {
                    publishProgress("カード種別: 在留カード");
                } else if (rcCardType.getType().equals("2")) {
                    publishProgress("カード種別: 特別永住者証明書");
                } else {
                    publishProgress("カード種別: 在留カード等(不明)");
                }
                break;
            default:
                publishProgress("カード種別: 不明");
                break;
            }
        } catch (Exception e) {
            Log.e(TAG, "error at " + getClass().getSimpleName(), e);
            return e;
        }
        return null;
    }

    protected void onProgressUpdate(String... values) {
        PinStatusActivity activity = (PinStatusActivity)mRef.get();
        if (activity == null) {
            return;
        }
        activity.addMessage(values[0]);
    }

    @Override
    protected void onPostExecute(Exception e) {
        mProgress.dismissAllowingStateLoss();
        PinStatusActivity activity = (PinStatusActivity)mRef.get();
        if (activity == null ||
            activity.isFinishing()) {
            return;
        }
        if (e != null) {
            activity.addMessage("エラー: " + e);
        }
    }
}
