package jp.co.osstech.jeidreader;

import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.security.Signature;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.util.TimeZone;

import android.util.Base64;
import android.content.Intent;
import android.graphics.Bitmap;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;
import org.json.JSONArray;

import jp.co.osstech.libjeid.*;
import jp.co.osstech.libjeid.ep.*;
import jp.co.osstech.libjeid.util.Hex;

public class EPReaderTask extends AsyncTask<Void, String, JSONObject>
{
    private static final String TAG = MainActivity.TAG;
    private WeakReference mRef;
    private Tag mNfcTag;
    private String passportNumber;
    private String birthDate;
    private String expireDate;

    public EPReaderTask(EPReaderActivity activity, Tag nfcTag) {
        mRef = new WeakReference<EPReaderActivity>(activity);
        mNfcTag = nfcTag;
    }

    @Override
    protected void onPreExecute() {
        EPReaderActivity activity = (EPReaderActivity)mRef.get();
        if (activity == null) {
            return;
        }
        passportNumber = activity.getPassportNumber();
        birthDate = activity.getBirthDate();
        expireDate = activity.getExpireDate();

        activity.hideKeyboard();
        activity.setMessage("# 読み取り開始、カードを離さないでください");
    }

    @Override
    protected JSONObject doInBackground(Void... args) {
        Log.d(TAG, getClass().getSimpleName() + "#doInBackground()");

        if (passportNumber.isEmpty()) {
            publishProgress("旅券番号を設定してください");
            return null;
        }

        if (birthDate.isEmpty()) {
            publishProgress("生年月日を設定してください");
            return null;
        }

        if (expireDate.isEmpty()) {
            publishProgress("有効期限を設定してください");
            return null;
        }
        MRZ mrz = new MRZ(passportNumber, birthDate, expireDate);

        long start = System.currentTimeMillis();
        JeidReader reader;
        try {
            reader = new JeidReader(mNfcTag);
        } catch (IOException e) {
            Log.e(TAG, "error", e);
            publishProgress("エラー: " + e);
            return null;
        }

        publishProgress("## パスポートの読み取り");
        try {
            CardType type = reader.detectCardType();
            publishProgress("CardType: " + type);
            if (type != CardType.EP) {
                publishProgress("パスポートではありません");
                return null;
            }

            PassportAP ap = reader.selectPassportAP();
            publishProgress("BAC start");
            ap.startBAC(mrz);
            publishProgress("BAC end");
            publishProgress("CommonData読み取り開始");
            EPCommonData commonData = ap.readCommonData();
            publishProgress(commonData.toString());

            JSONObject obj = new JSONObject();
            publishProgress("DG1読み取り開始");
            EPDataGroup1 dg1 = ap.readDataGroup1();
            publishProgress(dg1.getMRZ());

            MRZ dg1Mrz = new MRZ(dg1.getMRZ());
            if (!"JPN".equals(dg1Mrz.getIssuingCountry())) {
                publishProgress("日本発行のパスポートではありません");
                return null;
            }
            obj.put("ep-type", dg1Mrz.getDocumentCode());
            obj.put("ep-issuing-country", dg1Mrz.getIssuingCountry());
            obj.put("ep-passport-number", dg1Mrz.getPassportNumber());
            obj.put("ep-surname", dg1Mrz.getSurname());
            obj.put("ep-given-name", dg1Mrz.getGivenName());
            obj.put("ep-nationality", dg1Mrz.getNationality());
            obj.put("ep-date-of-birth", dg1Mrz.getBirthDate());
            obj.put("ep-sex", dg1Mrz.getSex());
            obj.put("ep-date-of-expiry", dg1Mrz.getExpirationDate());
            obj.put("ep-mrz", dg1.getMRZ());

            publishProgress("DG2読み取り開始");
            EPDataGroup2 dg2 = ap.readDataGroup2();
            byte[] jpeg = dg2.getFaceJpeg();
            String src = "data:image/jpeg;base64," + Base64.encodeToString(jpeg, Base64.DEFAULT);
            obj.put("ep-photo", src);
            publishProgress("読み取り完了");
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
        EPReaderActivity activity = (EPReaderActivity)mRef.get();
        if (activity == null) {
            return;
        }
        activity.addMessage(values[0]);
    }

    @Override
    protected void onPostExecute(JSONObject obj) {
        Log.d(TAG, getClass().getSimpleName() + "#onPostExecute()");
        EPReaderActivity activity = (EPReaderActivity)mRef.get();
        if (activity == null) {
            return;
        }

        if (obj == null) {
            return;
        }
        Intent intent = new Intent(activity, EPViewerActivity.class);
        intent.putExtra("json", obj.toString());
        activity.startActivity(intent);
    }
}
