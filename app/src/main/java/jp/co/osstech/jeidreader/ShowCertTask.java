package jp.co.osstech.jeidreader;

import android.content.Intent;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jp.co.osstech.libjeid.InvalidPinException;
import jp.co.osstech.libjeid.JPKIAP;
import jp.co.osstech.libjeid.JPKICertificate;
import jp.co.osstech.libjeid.JeidReader;
import jp.co.osstech.libjeid.util.Hex;

import org.json.JSONObject;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.pkcs.RSAPublicKey;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.asn1.x509.CertificatePolicies;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.PolicyInformation;
import org.bouncycastle.asn1.x509.PolicyQualifierInfo;
import org.bouncycastle.asn1.x509.ReasonFlags;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.x509.extension.X509ExtensionUtil;

public class ShowCertTask extends AsyncTask<Void, String, JSONObject>
{
    private static final String TAG = MainActivity.TAG;
    private WeakReference mRef;
    private Tag mNfcTag;
    private ProgressDialogFragment mProgress;
    private String mType;
    private InvalidPinException ipe;

    public ShowCertTask(ShowCertActivity activity, Tag nfcTag, String type) {
        mRef = new WeakReference<ShowCertActivity>(activity);
        mNfcTag = nfcTag;
        mType = type;
    }

    @Override
    protected void onPreExecute() {
        ShowCertActivity activity = (ShowCertActivity)mRef.get();
        if (activity == null) {
            return;
        }
        activity.setMessage("# 読み取り開始、カードを離さないでください");
        activity.hideKeyboard();
        activity.setMessage("");
        mProgress = new ProgressDialogFragment();
        mProgress.show(activity.getSupportFragmentManager(), "progress");
    }

    @Override
    protected JSONObject doInBackground(Void... args) {
        Log.d(TAG, getClass().getSimpleName() + "#doInBackground()");
        ShowCertActivity activity = (ShowCertActivity)mRef.get();

        try {
            JeidReader reader = new JeidReader(mNfcTag);
            JPKIAP jpkiAP = reader.selectJPKIAP();
            JPKICertificate cert = null;
            switch (mType) {
            case "AUTH":
                cert = jpkiAP.getAuthCert();
                break;
            case "AUTH_CA":
                cert = jpkiAP.getAuthCACert();
                break;
            case "SIGN":
                String password = activity.getPassword();
                if (password.isEmpty()) {
                    publishProgress("パスワードを入力してください。");
                    return null;
                }
                try {
                    jpkiAP.verifySignPin(password);
                    cert = jpkiAP.getSignCert();
                } catch (InvalidPinException e) {
                    ipe = e;
                    return null;
                }
                break;
            case "SIGN_CA":
                cert = jpkiAP.getSignCACert();
                break;
            }

            if (cert == null) {
                return null;
            }

            JSONObject obj = new JSONObject();
            obj.put("showcert-subject", cert.getSubjectX500Principal().toString());
            obj.put("showcert-issuer", cert.getIssuerX500Principal().toString());
            obj.put("showcert-not-before", cert.getNotBefore().toString());
            obj.put("showcert-not-after", cert.getNotAfter().toString());
            obj.put("showcert-version", Integer.toString(cert.getVersion()));
            obj.put("showcert-serial-number", cert.getSerialNumber().toString() + " (0x"
                    + cert.getSerialNumber().toString(16) + ")");

            Certificate x509Cert = Certificate.getInstance(cert.getEncoded());
            SubjectPublicKeyInfo spiObject = x509Cert.getSubjectPublicKeyInfo();
            obj.put("showcert-public-key-alg", cert.getPublicKey().getAlgorithm());
            obj.put("showcert-public-key-alg-params", spiObject.getAlgorithm().getParameters().toString());
            obj.put("showcert-public-key", Hex.encode(cert.getPublicKey().getEncoded(), ":"));

            if ("RSA".equalsIgnoreCase(cert.getPublicKey().getAlgorithm())) {
                RSAPublicKey rsaPublicKey = RSAPublicKey.getInstance(spiObject.parsePublicKey());
                obj.put("showcert-public-key-rsa-size", rsaPublicKey.getModulus().bitLength() + " bit");
                obj.put("showcert-public-key-rsa-modulus", Hex.encode(rsaPublicKey.getModulus().toByteArray(), ":"));
                obj.put("showcert-public-key-rsa-exponent", rsaPublicKey.getPublicExponent().toString() + " (0x"
                        + rsaPublicKey.getPublicExponent().toString(16) + ")");
            }

            obj.put("showcert-sig-alg", cert.getSigAlgName());
            obj.put("showcert-sig-alg-params", x509Cert.getSignatureAlgorithm().getParameters().toString());
            obj.put("showcert-signature", Hex.encode(cert.getSignature(), ":"));

            obj.put("showcert-fingerprint-sha1", Hex.encode(MessageDigest.getInstance("SHA-1").digest(cert.getEncoded()), ":"));
            obj.put("showcert-fingerprint-sha256", Hex.encode(MessageDigest.getInstance("SHA-256").digest(cert.getEncoded()), ":"));

            Set<String> criticalExtensionOIDs = cert.getCriticalExtensionOIDs();
            Set<String> extensionOIDs = new HashSet<>();
            extensionOIDs.addAll(criticalExtensionOIDs);
            extensionOIDs.addAll(cert.getNonCriticalExtensionOIDs());
            int unsupportedOidIndex = 0;
            for (String oid : extensionOIDs) {
                switch (oid) {
                case "2.5.29.14":
                    byte[] subjectKeyIdentifier = cert.getExtensionValue(oid);
                    if (subjectKeyIdentifier != null) {
                        obj.put("showcert-subject-key-identifier",
                                subjectKeyIdentifierToString(subjectKeyIdentifier)
                                + " (" + getCriticalLabel(criticalExtensionOIDs.contains(oid)) + ")");
                    }
                    break;
                case "2.5.29.15":
                    boolean[] keyUsage = cert.getKeyUsage();
                    if (keyUsage != null && keyUsage.length != 0) {
                        obj.put("showcert-key-usage", keyUsageToString(keyUsage)
                                + " (" + getCriticalLabel(criticalExtensionOIDs.contains(oid)) + ")");
                    }
                    break;
                case "2.5.29.17":
                    byte[] subjectAlternativeName = cert.getExtensionValue(oid);
                    if (subjectAlternativeName != null) {
                        obj.put("showcert-subject-alt-name", alternativeNameToString(subjectAlternativeName)
                                + " (" + getCriticalLabel(criticalExtensionOIDs.contains(oid)) + ")");
                    }
                    break;
                case "2.5.29.18":
                    byte[] issuerAlternativeName = cert.getExtensionValue(oid);
                    if (issuerAlternativeName != null) {
                        obj.put("showcert-issuer-alt-name", alternativeNameToString(issuerAlternativeName)
                                + " (" + getCriticalLabel(criticalExtensionOIDs.contains(oid)) + ")");
                    }
                    break;
                case "2.5.29.19":
                    int basicConstraints = cert.getBasicConstraints();
                    if (basicConstraints != -1) {
                            obj.put("showcert-basic-constraints", Integer.toString(basicConstraints)
                                    + " (" + getCriticalLabel(criticalExtensionOIDs.contains(oid)) + ")");
                    }
                    break;
                case "2.5.29.31":
                    byte[] crlDistributionPoint = cert.getExtensionValue(oid);
                    if (crlDistributionPoint != null) {
                        obj.put("showcert-crl-distribution-point",
                                crlDistributionPointToString(crlDistributionPoint)
                                + " (" + getCriticalLabel(criticalExtensionOIDs.contains(oid)) + ")");
                    }
                    break;
                case "2.5.29.32":
                    byte[] certificatePolicies = cert.getExtensionValue(oid);
                    if (certificatePolicies != null) {
                        obj.put("showcert-certificate-policies",
                                certificatePoliciesToString(certificatePolicies)
                                + " (" + getCriticalLabel(criticalExtensionOIDs.contains(oid)) + ")");
                    }
                    break;
                case "2.5.29.35":
                    byte[] authorityKeyIdentifier = cert.getExtensionValue(oid);
                    if (authorityKeyIdentifier != null) {
                        obj.put("showcert-authority-key-identifier",
                                authorityKeyIdentifierToString(authorityKeyIdentifier)
                                + " (" + getCriticalLabel(criticalExtensionOIDs.contains(oid)) + ")");
                    }
                    break;
                case "2.5.29.37":
                    List<String> extendedKeyUsage = cert.getExtendedKeyUsage();
                    if (extendedKeyUsage != null) {
                        obj.put("showcert-extended-key-usage",
                                extendedKeyUsageToString(extendedKeyUsage)
                                + " (" + getCriticalLabel(criticalExtensionOIDs.contains(oid)) + ")");
                    }
                    break;
                case "1.3.6.1.5.5.7.1.1":
                    byte[] authorityInformationAccess = cert.getExtensionValue(oid);
                    if (authorityInformationAccess != null) {
                        obj.put("showcert-authority-information-access",
                                authorityInformationAccessToString(authorityInformationAccess)
                                + " (" + getCriticalLabel(criticalExtensionOIDs.contains(oid)) + ")");
                    }
                    break;
                default:
                    obj.put("showcert-unsupported-oid" + Integer.toString(unsupportedOidIndex), oid);
                    obj.put("showcert-unsupported-oid" + Integer.toString(unsupportedOidIndex) + "-value",
                            Hex.encode(cert.getExtensionValue(oid), ":") + " ("
                            + getCriticalLabel(criticalExtensionOIDs.contains(oid)) + ")");
                    unsupportedOidIndex++;
                    break;
                }
            }
            return obj;
        } catch (Exception e) {
            Log.e(TAG, "error at " + getClass().getSimpleName() + "#doInBackground()", e);
            publishProgress("エラー: カードを読み取れませんでした" + e.getMessage());
            return null;
        }
    }

    @Override
    protected void onProgressUpdate(String... values) {
        ShowCertActivity activity = (ShowCertActivity)mRef.get();
        if (activity == null) {
            return;
        }
        activity.addMessage(values[0]);
    }

    @Override
    protected void onPostExecute(JSONObject obj) {
        Log.d(TAG, getClass().getSimpleName() + "#onPostExecute()");

        mProgress.dismiss();
        ShowCertActivity activity = (ShowCertActivity)mRef.get();
        if (activity == null) {
            return;
        }
        if (ipe != null) {
            int counter = ipe.getCounter();
            String title;
            String msg;
            if (ipe.isBlocked()) {
                title = "パスワードがブロックされています";
                msg = "市区町村窓口でブロック解除の申請をしてください。";
            } else {
                title = "パスワードが間違っています";
                msg = "パスワードを正しく入力してください。";
                msg += "のこり" + counter + "回間違えるとブロックされます。";
            }
            activity.addMessage(title);
            activity.addMessage(msg);
            activity.showInvalidPinDialog(title, msg);
            return;
        }
        if (obj == null) {
            activity.addMessage("エラー: カードを読み取れませんでした。");
            return;
        }
        Intent intent = new Intent(activity, ShowCertViewerActivity.class);
        intent.putExtra("json", obj.toString());
        activity.startActivity(intent);
    }

    private String keyUsageToString(final boolean[] keyUsage) {
        if (keyUsage == null || keyUsage.length != 9) {
            return "";
        }
        List<String> list = new ArrayList<>();
        if (keyUsage[0]) {
            list.add("電子署名");
        }
        if (keyUsage[1]) {
            list.add("否認防止");
        }
        if (keyUsage[2]) {
            list.add("鍵暗号");
        }
        if (keyUsage[3]) {
            list.add("データ暗号");
        }
        if (keyUsage[4]) {
            list.add("鍵交換");
        }
        if (keyUsage[5]) {
            list.add("電子証明書の検証");
        }
        if (keyUsage[6]) {
            list.add("CRLの署名検証");
        }
        if (keyUsage[7]) {
            list.add("鍵交換時のデータ暗号用");
        }
        if (keyUsage[8]) {
            list.add("鍵交換時のデータ複号用");
        }
        return listToString(list);
    }

    private String extendedKeyUsageToString(final List<String> extendedKeyUsage) {
        if (extendedKeyUsage == null) {
            return "";
        }
        List<String> list = new ArrayList<>();
        for (String oid : extendedKeyUsage) {
            if ("1.3.6.1.5.5.7.3.2".equals(oid)) {
                list.add("クライアント認証");
            } else {
                list.add(oid);
            }
        }
        return listToString(list);
    }

    private String subjectKeyIdentifierToString(byte[] bytes) throws Exception {
        if (bytes == null || bytes.length == 0) {
            return "";
        }

        SubjectKeyIdentifier subjectKeyIdentifier
                = SubjectKeyIdentifier.getInstance(X509ExtensionUtil.fromExtensionValue(bytes));
        byte[] keyId = subjectKeyIdentifier.getKeyIdentifier();
        return Hex.encode(keyId, ":");
    }

    private String alternativeNameToString(byte[] bytes) throws Exception {
        if (bytes == null || bytes.length == 0) {
            return "";
        }

        GeneralNames generalNames = GeneralNames.getInstance(X509ExtensionUtil.fromExtensionValue(bytes));
        return generalNamesToString(generalNames);
    }

    private String crlDistributionPointToString(byte[] bytes) throws Exception {
        if (bytes == null || bytes.length == 0) {
            return "";
        }

        List<String> list = new ArrayList<>();
        CRLDistPoint crlDistPoint = CRLDistPoint.getInstance(X509ExtensionUtil.fromExtensionValue(bytes));
        for (DistributionPoint distPoint : crlDistPoint.getDistributionPoints()) {
            DistributionPointName distributionPointName = distPoint.getDistributionPoint();
            String distributionPointNameStr;
            if (distributionPointName != null) {
                if (distributionPointName.getType() == DistributionPointName.FULL_NAME) {
                    GeneralNames distributionPointNames = (GeneralNames) distributionPointName.getName();
                    distributionPointNameStr = "(fullName=)" + generalNamesToString(distributionPointNames);
                } else {
                    distributionPointNameStr = "(nameRelativeToCRLIssuer=)"
                            + distributionPointName.getName().toString();
                }
            } else {
                distributionPointNameStr = "null";
            }

            ReasonFlags reasons = distPoint.getReasons();
            String reasonsStr;
            if (reasons != null) {
                reasonsStr = reasons.toString();
            } else {
                reasonsStr = "null";
            }

            GeneralNames crlIssuer = distPoint.getCRLIssuer();
            String crlIssuerStr = generalNamesToString(crlIssuer);

            list.add("{distributionPoint:" + distributionPointNameStr + ", reasons:" + reasonsStr
                    + ", cRLIssuer:" + crlIssuerStr + "}");
        }
        return listToString(list);
    }

    private String certificatePoliciesToString(byte[] bytes) throws Exception {
        if (bytes == null || bytes.length == 0) {
            return "";
        }

        List<String> list = new ArrayList<>();
        CertificatePolicies cpObject
                = CertificatePolicies.getInstance(X509ExtensionUtil.fromExtensionValue(bytes));
        PolicyInformation[] policyInformations = cpObject.getPolicyInformation();

        for (PolicyInformation information : policyInformations) {
            String policyId = information.getPolicyIdentifier().getId();

            ASN1Sequence policyQualifiers = information.getPolicyQualifiers();
            String policyQualifiersStr;
            if (policyQualifiers != null) {
                List<String> policyInfoStrList = new ArrayList<>();
                for (ASN1Encodable encodable : policyQualifiers) {
                    PolicyQualifierInfo policyQualiferInfo = PolicyQualifierInfo.getInstance(encodable);
                    String qualiferId = policyQualiferInfo.getPolicyQualifierId().getId();
                    String qualifer;
                    if ("1.3.6.1.5.5.7.2.1".equals(qualiferId)) {
                        qualiferId = "CPS";
                        qualifer = policyQualiferInfo.getQualifier().toString();
                    } else {
                        qualiferId = "userNotice";
                        qualifer = policyQualiferInfo.getQualifier().toString();
                    }
                    policyInfoStrList.add("(" + qualiferId + "=)" + qualifer);
                }
                policyQualifiersStr = listToString(policyInfoStrList);
            } else {
                policyQualifiersStr = "null";
            }

            list.add("{policyId:" + policyId + ", policyQualifiers:" + policyQualifiersStr + "}");
        }
        return listToString(list);
    }

    private String authorityKeyIdentifierToString(byte[] bytes) throws Exception {
        if (bytes == null || bytes.length == 0) {
            return "";
        }

        AuthorityKeyIdentifier akiObject
                = AuthorityKeyIdentifier.getInstance(X509ExtensionUtil.fromExtensionValue(bytes));
        String keyIdStr;
        byte[] keyId = akiObject.getKeyIdentifier();
        if (keyId != null) {
            keyIdStr = Hex.encode(keyId, ":");
        } else {
            keyIdStr = "null";
        }

        GeneralNames certIssuer = akiObject.getAuthorityCertIssuer();
        String certIssuerStr = generalNamesToString(certIssuer);

        BigInteger serialNumber = akiObject.getAuthorityCertSerialNumber();
        String serialNumberStr;
        if (serialNumber != null) {
            serialNumberStr = serialNumber.toString();
        } else {
            serialNumberStr = "null";
        }
        return "{keyId:" + keyIdStr + ", issuer:" + certIssuerStr + ", serial:" + serialNumberStr + "}";
    }

    private String authorityInformationAccessToString(byte[] bytes) throws Exception {
        if (bytes == null || bytes.length == 0) {
            return "";
        }

        List<String> list = new ArrayList<>();
        AuthorityInformationAccess aiaObject
                = AuthorityInformationAccess.getInstance(X509ExtensionUtil.fromExtensionValue(bytes));
        AccessDescription[] accessDescriptions = aiaObject.getAccessDescriptions();

        for (AccessDescription accessDescription : accessDescriptions) {
            String methodStr;
            String locationStr;
            if (X509ObjectIdentifiers.ocspAccessMethod.equals(accessDescription.getAccessMethod())) {
                methodStr = "OCSP";
                GeneralName generalName = accessDescription.getAccessLocation();
                locationStr = generalNameToString(generalName);
            } else {
                methodStr = accessDescription.getAccessMethod().getId();
                locationStr = accessDescription.getAccessLocation().toString();
            }
            list.add("{method:" + methodStr + ", location:" + locationStr + "}");
        }
        return listToString(list);
    }

    private String generalNamesToString(GeneralNames generalNames) {
        if (generalNames == null) {
            return "null";
        }

        List<String> list = new ArrayList<>();
        for (GeneralName generalName : generalNames.getNames()) {
            list.add(generalNameToString(generalName));
        }
        return listToString(list);
    }

    private String generalNameToString(GeneralName generalName) {
        if (generalName == null) {
            return "null";
        }

        String tagStr;
        String valueStr;
        switch (generalName.getTagNo()) {
        case GeneralName.otherName:
            tagStr = "otherName";
            valueStr = generalName.getName().toString();
            break;
        case GeneralName.rfc822Name:
            tagStr = "rfc822";
            valueStr = ((DERIA5String) generalName.getName()).getString();
            break;
        case GeneralName.dNSName:
            tagStr = "dnsName";
            valueStr = ((DERIA5String) generalName.getName()).getString();
            break;
        case GeneralName.x400Address:
            tagStr = "x400Address";
            valueStr = generalName.getName().toString();
            break;
        case GeneralName.directoryName:
            tagStr = "dirName";
            valueStr = (X500Name.getInstance(generalName.getName()).toString());
            break;
        case GeneralName.ediPartyName:
            tagStr = "ediPartyName";
            valueStr = generalName.getName().toString();
            break;
        case GeneralName.uniformResourceIdentifier:
            tagStr = "uri";
            valueStr = ((DERIA5String) generalName.getName()).getString();
            break;
        case GeneralName.iPAddress:
            tagStr = "ipAddress";
            byte[] ip = DEROctetString.getInstance(generalName.getName()).getOctets();
            StringBuilder strBuilder = new StringBuilder();
            if (ip.length == 4 || ip.length == 8) {
                for (int i = 0; i < ip.length; i++) {
                    if (i == 4) {
                        strBuilder.append('/');
                    }
                    strBuilder.append(ip[i] & 0xff);
                    if (i < 3 || (4 <= i && i < 7)) {
                        strBuilder.append('.');
                    }
                }
                valueStr = strBuilder.toString();
            } else if (ip.length == 16) {
                for (int i = 0; i < ip.length; i += 2) {
                    strBuilder.append(String.format("%02X", ip[i]));
                    strBuilder.append(String.format("%02X", ip[i + 1]));
                    if (i < 13) {
                        strBuilder.append(':');
                    }
                }
                valueStr = strBuilder.toString();
            } else {
                valueStr = generalName.getName().toString();
            }
            break;
        case GeneralName.registeredID:
            tagStr = "registeredID";
            valueStr = ASN1ObjectIdentifier.getInstance(generalName.getName()).getId();
            break;
        default:
            tagStr = Integer.toString(generalName.getTagNo());
            valueStr = generalName.getName().toString();
            break;
        }
        return "(" + tagStr + "=)" + valueStr + "";
    }

    private String getCriticalLabel(boolean isCritical) {
        if (isCritical) {
            return "Critical";
        } else {
            return "Non-critical";
        }
    }

    private String listToString(final List<String> list) {
        if (list == null || list.size() == 0) {
            return "null";
        }
        int i = 0;
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append('[');
        for (String str : list) {
            strBuilder.append(str);
            if (i < list.size() - 1) {
                strBuilder.append(", ");
            }
            i++;
        }
        strBuilder.append(']');
        return strBuilder.toString();
    }
}
