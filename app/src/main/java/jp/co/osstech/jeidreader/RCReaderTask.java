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
import jp.co.osstech.libjeid.InvalidACKeyException;
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
    private ProgressDialogFragment mProgress;

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
        mProgress = new ProgressDialogFragment();
        mProgress.show(activity.getSupportFragmentManager(), "progress");
    }

    @Override
    protected JSONObject doInBackground(Void... args) {
        Log.d(TAG, getClass().getSimpleName() + "#doInBackground()");

        long start = System.currentTimeMillis();
        JeidReader reader;
        try {
            reader = new JeidReader(mNfcTag);
        } catch (IOException e) {
            Log.e(TAG, "error", e);
            publishProgress("エラー: " + e);
            return null;
        }

        try {
            publishProgress("## カード種別");
            CardType type = reader.detectCardType();
            publishProgress("CardType: " + type);
            if (type != CardType.RC) {
                publishProgress("在留カード/特別永住者証明書ではありません");
                return null;
            }
            ResidenceCardAP ap = reader.selectResidenceCardAP();
            RCCommonData commonData = ap.readCommonData();
            publishProgress("commonData: " + commonData);
            RCCardType cardType = ap.readCardType();
            publishProgress("cardType: " + cardType);

            if (rcNumber.isEmpty()) {
                publishProgress("在留カード番号または特別永住者証明書番号を設定してください");
                return null;
            }
            RCKey rckey = new RCKey(rcNumber);
            publishProgress("## セキュアメッセージング用の鍵交換&認証");
            try {
                ap.startAC(rckey);
            } catch (InvalidACKeyException e) {
                publishProgress("失敗\n"
                        + "在留カード番号または特別永住者証明書番号が間違っています");
                return null;
            }
            publishProgress("完了");

            publishProgress("## カードから情報を取得します");
            RCFiles files = ap.readFiles();
            publishProgress("完了");
            JSONObject obj = new JSONObject();
            obj.put("rc-card-type", cardType.getType());
            RCCardEntries cardEntries = files.getCardEntries();
            byte[] png = cardEntries.toPng();
            String src = "data:image/png;base64," + Base64.encodeToString(png, Base64.DEFAULT);
            obj.put("rc-front-image", src);

            publishProgress("## 写真のデコード");
            RCPhoto photo = files.getPhoto();
            BitmapARGB argb = photo.getPhotoBitmapARGB();
            if (argb != null) {
                Bitmap bitmap = Bitmap.createBitmap(argb.getData(),
                        argb.getWidth(),
                        argb.getHeight(),
                        Bitmap.Config.ARGB_8888);
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                byte[] jpeg = os.toByteArray();
                src = "data:image/jpeg;base64," + Base64.encodeToString(jpeg, Base64.DEFAULT);
                obj.put("rc-photo", src);
            }
            publishProgress("完了");
            publishProgress("## 住居地（裏面追記）");
            RCAddress address = files.getAddress();
            publishProgress(address.toString());

            if ("1".equals(cardType.getType())) {
                publishProgress("## 裏面資格外活動包括許可欄");
                RCComprehensivePermission comprehensivePermission = files.getComprehensivePermission();
                publishProgress(comprehensivePermission.toString());

                publishProgress("## 裏面資格外活動個別許可欄");
                RCIndividualPermission individualPermission = files.getIndividualPermission();
                publishProgress(individualPermission.toString());

                publishProgress("## 裏面在留期間等更新申請欄");
                RCUpdateStatus updateStatus = files.getUpdateStatus();
                publishProgress(updateStatus.toString());
            }

            publishProgress("## 電子署名");
            RCSignature signature = files.getSignature();
            publishProgress(signature.toString());

            // チェックコードの検証
            publishProgress("## チェックコードの検証");
            boolean checkcodeVerified = false;
            try {
                checkcodeVerified = ap.verifySignature(signature, cardEntries, photo);
            } catch (IOException e) {
                Log.e(TAG, e.toString());
                publishProgress("検証中にエラー: " + e);
            }
            publishProgress("検証結果: " + checkcodeVerified);

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
        mProgress.dismiss();
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
