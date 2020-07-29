package jp.co.osstech.jeidreader;

import android.content.Intent;
import android.net.Uri;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;

import jp.co.osstech.libjeid.InvalidPinException;
import jp.co.osstech.libjeid.JPKIAP;
import jp.co.osstech.libjeid.JPKISignature;
import jp.co.osstech.libjeid.JeidReader;
import jp.co.osstech.libjeid.util.Hex;

public class SignTask extends AsyncTask<Void, String, Boolean>
{
    private static final String TAG = MainActivity.TAG;
    private WeakReference mRef;
    private Tag mNfcTag;
    private String mPin;
    private byte[] mInput;
    private ProgressDialogFragment mProgress;

    public SignTask(SignActivity activity, Tag nfcTag) {
        mRef = new WeakReference<SignActivity>(activity);
        mNfcTag = nfcTag;
    }

    @Override
    protected void onPreExecute() {
        SignActivity activity;
        activity = (SignActivity)mRef.get();
        if (activity == null) {
            return;
        }
        mPin = activity.getPin();
        mInput = activity.getText().getBytes();
        activity.hideKeyboard();
        activity.setMessage("");
        mProgress = new ProgressDialogFragment();
        mProgress.show(activity.getSupportFragmentManager(), "progress");
    }

    private void outputFile(SignActivity activity, String filename, byte[] data) throws IOException {
        File file = new File(activity.getExternalFilesDir(null), filename);
        FileOutputStream writer = new FileOutputStream(file);
        writer.write(Base64.encode(data, Base64.DEFAULT));
        writer.close();
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file));
        activity.sendBroadcast(intent);
    }

    @Override
    protected Boolean doInBackground(Void... args) {
        Log.d(TAG, getClass().getSimpleName() + "#doInBackground()");

        SignActivity activity = (SignActivity)mRef.get();
        if (activity == null) {
            return false;
        }

        String signAlgo = activity.getSignAlgo();

        int type = activity.getType();
        if (type == JPKIAP.TYPE_AUTH) {
            publishProgress("認証用署名を開始");
        } else {
            publishProgress("署名用署名を開始");
        }

        if (mPin.isEmpty()) {
            publishProgress("暗証番号を入力してください。");
            return false;
        }

        try {
            publishProgress("input: " + Hex.encode(mInput));

            publishProgress("write input file: input.txt");
            outputFile(activity, "input.txt", mInput);

            JeidReader reader = new JeidReader(mNfcTag);
            JPKIAP jpki = reader.selectJPKIAP();

            X509Certificate cert;
            if (type == JPKIAP.TYPE_AUTH) {
                cert = jpki.getAuthCert();
            } else {
                jpki.verifySignPin(mPin);
                cert = jpki.getSignCert();
            }
            byte[] certDer;
            try {
                certDer = cert.getEncoded();
            } catch (Exception e) {
                publishProgress("DERエンコードエラー");
                return false;
            }
            publishProgress("write cert file: cert.txt");
            outputFile(activity, "cert.txt", certDer);

            publishProgress("Signature Algorithm: " + signAlgo);

            JPKISignature signature;
            byte[] signed;
            if (type == JPKIAP.TYPE_AUTH) {
                signature = jpki.getAuthSignature(signAlgo);
                signature.update(mInput);
                signed = signature.sign(mPin);
            } else {
                signature = jpki.getSignSignature(signAlgo);
                signature.update(mInput);
                signed = signature.sign(mPin);
            }
            //publishProgress("signed: " + Hex.encode(signed));
            publishProgress("write signed file: signed.txt");
            outputFile(activity, "signed.txt", signed);

            byte[] digest = signature.getDigest();
            publishProgress("digest: " + Hex.encode(digest));

            publishProgress("write digest file: digest.txt");
            outputFile(activity, "digest.txt", digest);

            publishProgress("署名完了");

            PublicKey pubkey = cert.getPublicKey();
            Signature verifier = Signature.getInstance(signAlgo);
            verifier.initVerify(pubkey);
            verifier.update(mInput);
            if (verifier.verify(signed)) {
                publishProgress("署名の検証: 成功");
            } else {
                publishProgress("署名の検証: 失敗");
                return false;
            }

        } catch (NoSuchAlgorithmException e) {
            publishProgress("ダイジェストエラー");
            return false;
        } catch (InvalidPinException e) {
            publishProgress("エラー: PINが間違っています。のこり: " + e.getCounter());
            return false;
        } catch (IOException e) {
            Log.e(TAG, "error", e);
            publishProgress("エラー: " + e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "error", e);
            publishProgress("エラー: " + e);
            return false;
        }

        return true;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        SignActivity activity = (SignActivity)mRef.get();
        if (activity == null) {
            return;
        }
        activity.addMessage(values[0]);
    }

    protected void onPostExecute(Boolean result) {
        mProgress.dismiss();
        SignActivity activity = (SignActivity)mRef.get();
        if (activity == null ||
            activity.isFinishing() ||
            activity.isDestroyed()) {
            return;
        }
        if (result) {
            activity.addMessage("成功");
        } else {
            activity.addMessage("失敗");
        }
    }
}
