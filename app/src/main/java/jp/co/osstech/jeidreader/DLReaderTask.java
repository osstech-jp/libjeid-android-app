package jp.co.osstech.jeidreader;

import android.content.Intent;
import android.graphics.Bitmap;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.cert.CertificateException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import jp.co.osstech.libjeid.*;
import jp.co.osstech.libjeid.dl.*;
import jp.co.osstech.libjeid.util.Hex;

import org.json.JSONArray;
import org.json.JSONObject;

public class DLReaderTask extends AsyncTask<Void, String, JSONObject>
{
    private static final String TAG = MainActivity.TAG;
    private WeakReference mRef;
    private Tag mNfcTag;
    private String pin1;
    private String pin2;
    private InvalidPinException ipe1;
    private InvalidPinException ipe2;

    public DLReaderTask(DLReaderActivity activity, Tag nfcTag) {
        mRef = new WeakReference<DLReaderActivity>(activity);
        mNfcTag = nfcTag;
    }

    @Override
    protected void onPreExecute() {
        DLReaderActivity activity = (DLReaderActivity)mRef.get();
        if (activity == null) {
            return;
        }
        pin1 = activity.getPin1();
        pin2 = activity.getPin2();
        activity.hideKeyboard();
        activity.setMessage("# 読み取り開始、カードを離さないでください");
    }

    @Override
    protected JSONObject doInBackground(Void... args) {
        Log.d(TAG, getClass().getSimpleName() + "#doInBackground()");

        JeidReader reader;
        try {
            reader = new JeidReader(mNfcTag);
        } catch (IOException e) {
            Log.e(TAG, "error", e);
            publishProgress("エラー: " + e);
            return null;
        }

        publishProgress("## 運転免許証の読み取り開始");
        try {
            CardType type = reader.detectCardType();
            publishProgress("CardType: " + type);
            if (type != CardType.DL) {
                publishProgress("運転免許証ではありません");
                return null;
            }
            DriverLicenseAP ap = reader.selectDriverLicenseAP();
            DriverLicenseCommonData commonData = ap.getCommonData();
            publishProgress("## 共通データ要素");
            publishProgress(commonData.toString());

            if (pin1.isEmpty()) {
                publishProgress("暗証番号1を設定してください");
                return null;
            }

            try {
                ap.verifyPin1(pin1);
            } catch (InvalidPinException e) {
                ipe1 = e;
                return null;
            }
            if (!pin2.isEmpty()) {
                try {
                    ap.verifyPin2(pin2);
                } catch (InvalidPinException e) {
                    ipe2 = e;
                    return null;
                }
            }

            // 券面情報の取得
            DriverLicenseEntries entries = ap.getEntries();
            // 外字の取得
            DriverLicenseExternalCharactors extChars = ap.getExternalCharactors();
            JSONObject obj = new JSONObject();
            obj.put("dl-name", entries.getNameHtml(extChars));
            obj.put("dl-kana", entries.getKana());
            DriverLicenseDate birthDate = entries.getBirthDate();
            if (birthDate != null) {
                obj.put("dl-birth", birthDate.toString());
            }
            obj.put("dl-addr", entries.getAddrHtml(extChars));
            DriverLicenseDate issueDate = entries.getIssueDate();
            if (issueDate != null) {
                obj.put("dl-issue", issueDate.toString());
            }
            obj.put("dl-ref", entries.getRefNumber());
            obj.put("dl-color-class", entries.getColorClass());
            DriverLicenseDate expireDate = entries.getExpireDate();
            if (expireDate != null) {
                obj.put("dl-expire", expireDate.toString());
                Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));
                calendar.setTime(expireDate.toDate());
                calendar.set(Calendar.HOUR_OF_DAY, 23);
                calendar.set(Calendar.MINUTE, 59);
                calendar.set(Calendar.SECOND, 59);
                obj.put("dl-is-expired", Boolean.toString(new Date().after(calendar.getTime())));
            }
            obj.put("dl-number", entries.getLicenseNumber());

            String pscName = entries.getPscName();
            if (pscName != null) {
                obj.put("dl-sc", pscName.replace("公安委員会", ""));
            }
            int i = 1;
            for (String condition : entries.getConditions()) {
                obj.put(String.format(Locale.US, "dl-condition%d", i++), condition);
            }
            JSONArray categories = new JSONArray();
            for (DriverLicenseCategory category : entries.getCategories()) {
                JSONObject categoryObj = new JSONObject();
                categoryObj.put("tag", category.getTag());
                categoryObj.put("name", category.getName());
                categoryObj.put("date", category.getDate().toString());
                categoryObj.put("licensed", category.isLicensed());
                categories.put(categoryObj);
            }
            obj.put("dl-categories", categories);

            publishProgress(entries.toString());

            DriverLicenseChangedEntries changedEntries = ap.getChangedEntries();
            publishProgress(changedEntries.toString());

            JSONArray remarks = new JSONArray();
            if (changedEntries.isChanged()) {
                for (String addr : changedEntries.getNewAddrs()) {
                    JSONObject remarkObj = new JSONObject();
                    remarkObj.put("label", "新住所");
                    remarkObj.put("text", addr);
                    remarks.put(remarkObj);
                }
                for (String name : changedEntries.getNewNames()) {
                    JSONObject remarkObj = new JSONObject();
                    remarkObj.put("label", "新氏名");
                    remarkObj.put("text", name);
                    remarks.put(remarkObj);
                }
            }
            obj.put("dl-remarks", remarks);

            DriverLicenseSignature signature = ap.getSignature();
            if (!pin2.isEmpty()) {
                DriverLicenseRegisteredDomicile registeredDomicile = ap.getRegisteredDomicile();
                if (registeredDomicile != null) {
                    String value = registeredDomicile.getRegisteredDomicile();
                    if (value != null) {
                        obj.put("dl-registered-domicile", value);
                    }
                }
                publishProgress(registeredDomicile.toString());

                publishProgress("写真の読み取り中...");
                DriverLicensePhoto photo = ap.getPhoto();
                publishProgress("写真のデコード中...");
                Bitmap bitmap = photo.getBitmap();
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                byte[] jpeg = os.toByteArray();
                String src = "data:image/jpeg;base64," + Base64.encodeToString(jpeg, Base64.DEFAULT);
                obj.put("dl-photo", src);
                publishProgress(signature.toString());

                // 電子署名検証
                boolean verified = false;
                try {
                    signature.initVerify();
                    signature.update(entries.getEncoded());
                    signature.update(registeredDomicile.getEncoded());
                    signature.update(photo.getEncoded());
                    verified = signature.verify();
                } catch (CertificateException ce) {
                    // 有効な証明書が見つかりません
                    // もしくは無償版を利用した場合にこの例外が返ります
                    Log.e(TAG, ce.toString());
                    verified = false;
                }
                obj.put("dl-verified", verified);
                publishProgress("署名検証: " + verified);
            }

            String signatureSubject = signature.getSubject();
            publishProgress("Subject: " + signatureSubject);
            obj.put("dl-signature-subject", signatureSubject);
            String signatureSKI = Hex.encode(signature.getSubjectKeyIdentifier(), ":");
            publishProgress("Subject Key Identifier: " + signatureSKI);
            obj.put("dl-signature-ski", signatureSKI);
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
        DLReaderActivity activity = (DLReaderActivity)mRef.get();
        if (activity == null) {
            return;
        }
        activity.addMessage(values[0]);
    }

    @Override
    protected void onPostExecute(JSONObject obj) {
        Log.d(TAG, getClass().getSimpleName() + "#onPostExecute()");
        DLReaderActivity activity = (DLReaderActivity)mRef.get();
        if (activity == null) {
            return;
        }
        if (ipe1 != null) {
            int counter = ipe1.getCounter();
            String title;
            String msg;
            if (ipe1.isBlocked()) {
                title = "暗証番号1がブロックされています";
                msg = "警察署でブロック解除の申請をしてください。";
            } else {
                title = "暗証番号1が間違っています";
                msg = "暗証番号1を正しく入力してください。";
                msg += "のこり" + counter + "回間違えるとブロックされます。";
            }
            activity.addMessage(title);
            activity.addMessage(msg);
            activity.showInvalidPinDialog(title, msg);
            return;
        }
        if (ipe2 != null) {
            int counter = ipe2.getCounter();
            String title;
            String msg;
            if (ipe2.isBlocked()) {
                title = "暗証番号2がブロックされています";
                msg = "警察署でブロック解除の申請をしてください。";
            } else {
                title = "暗証番号2が間違っています";
                msg = "暗証番号2を正しく入力してください。";
                msg += "のこり" + counter + "回間違えるとブロックされます。";
            }
            activity.addMessage(title);
            activity.addMessage(msg);
            activity.showInvalidPinDialog(title, msg);
            return;
        }
        if (obj == null) {
            return;
        }

        Intent intent = new Intent(activity, DLViewerActivity.class);
        intent.putExtra("json", obj.toString());
        activity.startActivity(intent);
    }
}
