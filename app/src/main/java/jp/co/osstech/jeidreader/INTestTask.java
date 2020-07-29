package jp.co.osstech.jeidreader;

import android.nfc.Tag;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.PublicKey;
import java.security.Signature;

import jp.co.osstech.libjeid.*;
import jp.co.osstech.libjeid.in.*;
import jp.co.osstech.libjeid.util.Hex;

public class INTestTask extends AsyncTask<Void, String, Boolean>
{
    private static final String TAG = MainActivity.TAG;
    private WeakReference mRef;
    private Tag mNfcTag;
    private String authPin;
    private String signPin;

    public INTestTask(INTestActivity activity, Tag nfcTag) {
        mRef = new WeakReference<INTestActivity>(activity);
        mNfcTag = nfcTag;
    }

    @Override
    protected void onPreExecute() {
        INTestActivity activity = (INTestActivity)mRef.get();
        if (activity == null) {
            return;
        }
        authPin = activity.getAuthPin();
        signPin = activity.getSignPin();
        activity.hideKeyboard();
        activity.setMessage("# テスト開始、カードを離さないでください");
    }

    @Override
    protected Boolean doInBackground(Void... args) {
        Log.d(TAG, getClass().getSimpleName() + "#doInBackground()");

        if (authPin.isEmpty()) {
            publishProgress("暗証番号を設定してください");
            return false;
        }

        if (signPin.isEmpty()) {
            publishProgress("パスワードを設定してください");
            return false;
        }

        long start = System.currentTimeMillis();
        JeidReader reader;
        try {
            reader = new JeidReader(mNfcTag);
        } catch (IOException e) {
            Log.e(TAG, "error", e);
            return false;
        }

        publishProgress("## 券面事項入力補助APのテスト");
        try {
            INTextAP textAp = reader.selectINTextAP();
            textAp.verifyPin(authPin);
            INTextMyNumber textMyNumber = textAp.readMyNumber();
            publishProgress("マイナンバー: " + textMyNumber.getMyNumber());
            INTextAttributes textAttrs = textAp.readAttributes();
            publishProgress("氏名: " + textAttrs.getName());
            publishProgress("性別: " + textAttrs.getSexString());
            publishProgress("生年月日: " + textAttrs.getBirth());
            publishProgress("住所: " + textAttrs.getAddr());
        } catch (Exception e) {
            Log.e(TAG, "error", e);
            publishProgress("エラー: " + e);
            return false;
        }

        publishProgress("## 公的個人認証APのテスト");
        try {
            JPKIAP jpki = reader.selectJPKIAP();

            JPKICertificate authCert = jpki.getAuthCert();
            publishProgress("認証用証明書: " + authCert.getSubjectX500Principal());
            jpki.verifySignPin(signPin);
            JPKICertificate signCert = jpki.getSignCert();
            publishProgress("署名用証明書: " + signCert.getSubjectX500Principal());
            JPKIAttributes attrs = signCert.getJPKIAttributes();
            if (attrs == null) {
                publishProgress("署名用証明書属性: null");
            } else {
                publishProgress("署名用証明書属性: ");
                publishProgress("  氏名: " + attrs.getName());
                publishProgress("  氏名(代替文字): "
                                + attrs.getNameAltString());
                publishProgress("  性別: " + attrs.getSexString());
                publishProgress("  生年月日: " + attrs.getBirth());
                publishProgress("  住所: " + attrs.getAddr());
                publishProgress("  住所(代替文字): "
                                + attrs.getAddrAltString());
            }
            JPKICertificate authCACert = jpki.getAuthCACert();
            publishProgress("認証用CA証明書: " + authCACert.getSubjectX500Principal());
            JPKICertificate signCACert = jpki.getSignCACert();
            publishProgress("署名用CA証明書: " + signCACert.getSubjectX500Principal());

            String text = "hello";
            publishProgress("署名対象: " + text);
            String signAlgo = "SHA1withRSA";
            JPKISignature authSignature = jpki.getAuthSignature(signAlgo);
            byte[] input = text.getBytes();
            authSignature.update(input);
            byte[] signed = authSignature.sign(authPin);
            byte[] digest = authSignature.getDigest();
            publishProgress(signAlgo + ": " + Hex.encode(digest));
            //publishProgress("signedData: " + Hex.encode(signed));
            publishProgress("認証用署名: 成功");
            PublicKey authPubKey = authCert.getPublicKey();
            Signature verifier = Signature.getInstance("SHA1withRSA");
            verifier.initVerify(authPubKey);
            verifier.update(input);
            if (verifier.verify(signed)) {
                publishProgress("認証用署名の検証: 成功");
            } else {
                publishProgress("認証用署名の検証: 失敗");
                return false;
            }

            JPKISignature signSignature = jpki.getSignSignature("SHA1withRSA");
            signSignature.update(input);
            signed = signSignature.sign(signPin);
            //publishProgress("signedData: " + Hex.encode(signed));
            publishProgress("署名用署名: 成功");

            PublicKey signPubKey = signCert.getPublicKey();
            verifier.initVerify(signPubKey);
            verifier.update(input);
            if (verifier.verify(signed)) {
                publishProgress("署名用署名の検証: 成功");
            } else {
                publishProgress("署名用署名の検証: 失敗");
                return false;
            }

            long end = System.currentTimeMillis();
            publishProgress("処理時間(sec): " + ((float)(end - start) / 1000));
        } catch (Exception e) {
            Log.e(TAG, "error", e);
            publishProgress("エラー: " + e);
            return false;
        }

        return true;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        Log.d(TAG, getClass().getSimpleName() + "#onProgressUpdate()");
        INTestActivity activity = (INTestActivity)mRef.get();
        if (activity == null) {
            return;
        }
        activity.addMessage(values[0]);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        Log.d(TAG, getClass().getSimpleName() + "#onPostExecute()");
        INTestActivity activity = (INTestActivity)mRef.get();
        if (activity == null ||
            activity.isFinishing() ||
            activity.isDestroyed()) {
            return;
        }
        if (result) {
            activity.addMessage("# テスト結果: Passed");
        } else {
            activity.addMessage("# テスト結果: Failed");
        }
    }
}
