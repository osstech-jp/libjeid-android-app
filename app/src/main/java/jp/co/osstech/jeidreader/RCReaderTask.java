package jp.co.osstech.jeidreader;

import android.content.Intent;
import android.graphics.Bitmap;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

import jp.co.osstech.libjeid.CardType;
import jp.co.osstech.libjeid.InvalidBACKeyException;
import jp.co.osstech.libjeid.JeidReader;
import jp.co.osstech.libjeid.ResidenceCardAP;
import jp.co.osstech.libjeid.RCKey;
import jp.co.osstech.libjeid.rc.*;
import jp.co.osstech.libjeid.rc.RCAddress;
import jp.co.osstech.libjeid.util.BitmapARGB;

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
        RCKey rckey = new RCKey(rcNumber);

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
            publishProgress("Basic Access Control開始");
            try {
                ap.startBAC(rckey, false, false);
            } catch (InvalidBACKeyException e) {
                publishProgress("Basic Access Control失敗\n"
                        + "在留カード番号または特別永住者証明書番号が間違っています");
                return null;
            }
            publishProgress("Basic Access Control完了");
            publishProgress("Verify SM開始");
            try {
                ap.verifySM(rckey);
            } catch (IOException e) {
                publishProgress("Verify SM失敗\n"
                        + e.getMessage());
                return null;
            }
            publishProgress("Verify SM完了");
            JSONObject obj = new JSONObject();
            obj.put("rc-card-type", cardType.getType());
            /*
            publishProgress("券面（表）イメージ読み取り開始");
            RCCardFrontEntries cardFrontEntries = ap.readCardFrontEntries();
            byte[] png = cardFrontEntries.toPng();
            String src = "data:image/png;base64," + Base64.encodeToString(png, Base64.DEFAULT);
            obj.put("rc-front-image", src);

            publishProgress("顔写真読み取り開始");
            RCFacePhoto photo = ap.readFacePhoto();
            BitmapARGB argb = photo.getPhotoARGB();
            Bitmap bitmap = Bitmap.createBitmap(argb.getData(),
                                                argb.getWidth(),
                                                argb.getHeight(),
                                                Bitmap.Config.ARGB_8888);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
            byte[] jpeg = os.toByteArray();
            src = "data:image/jpeg;base64," + Base64.encodeToString(jpeg, Base64.DEFAULT);
            obj.put("rc-photo", src);
            */

            publishProgress("住居地（裏面追記）の読み取り開始");
            RCAddress address = ap.readAddress();
            publishProgress(address.toString());

            publishProgress("裏面資格外活動包括許可欄の読み取り開始");
            RCComprehensivePermission comprehensivePermission = ap.readComprehensivePermission();
            publishProgress(comprehensivePermission.toString());

            publishProgress("裏面資格外活動個別許可欄の読み取り開始");
            RCIndividualPermission individualPermission = ap.readIndividualPermission();
            publishProgress(individualPermission.toString());

            publishProgress("裏面在留期間等更新申請欄の読み取り開始");
            RCUpdateStatus updateStatus = ap.readUpdateStatus();
            publishProgress(updateStatus.toString());

            publishProgress("電子署名の読み取り開始");
            RCSignature signature = ap.readSignature();
            publishProgress(signature.toString());

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
