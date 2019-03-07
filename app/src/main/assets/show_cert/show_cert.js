function addMessage(msg)
{
    var area = document.getElementById('msg');
    if (area) {
        area.innerHTML += htmlEscape(msg) + '<br/>';
    }
}

function clearMessage()
{
    var area = document.getElementById('msg');
    if (area) {
        area.innerHTML = '';
    }
}

function htmlEscape(str) {
    return str.replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/'/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function displayTabContent(id) {
    var page = document.getElementById(id);
    var currentPage;
    if (id === 'content_general') {
       currentPage = document.getElementById('content_details');
    } else if (id === 'content_details') {
        currentPage = document.getElementById('content_general');
    }
    currentPage.style.display = 'none';
    page.style.display = 'block';
}

function displayFieldContent(id) {
    var hiddenEle = document.getElementById(id + '-hidden');
    var dummyEle = document.getElementById(id + '-short');
    var labelEle = document.getElementById(id + '-label');
    if (document.getElementById(id).checked) {
        dummyEle.style.display = 'none';
        hiddenEle.style.display = 'block';
        labelEle.setAttribute('suffix', ' [全表示中]');
    } else {
        hiddenEle.style.display = 'none';
        dummyEle.style.display = 'block';
        labelEle.setAttribute('suffix', ' [タップで全表示]');
    }
}

function generateFieldLine(id, label, value, prefix) {
    var omitLength = 128;
    var line;
    if (value.length > omitLength) {
        line = '<div class=\"cert-field cert-field-reversible\">'
            + '<input id="' + prefix + id + '" name="' + prefix + id + '" '
            + 'type="checkbox" onchange="displayFieldContent(\'' + prefix + id + '\')">'
            + '<label class="cert-field-label cert-field-reversible-label" for="' + prefix + id + '" '
            + 'id="' + prefix + id + '-label" suffix=" [タップで全表示]">' + label + '</label>'
            + '<div class="cert-field-value cert-field-reversible-value-hidden" id="' + prefix + id + '-hidden' + '">'
            + value
            + '</div>'
            + '<div class="cert-field-value cert-field-reversible-value-short" for="' + prefix + id + '" '
            + 'id="' + prefix + id + '-short' + '">'
            + value.substr(0, omitLength) + ' . . .'
            + '</div></div>';
    } else {
        line = '<div class=\"cert-field\">'
            + '<div class=\"cert-field-label\">'
            + label
            + '</div>'
            + '<div class=\"cert-field-value\">'
            + value
            + '</div></div>';
    }
    return line;
}

function render(json) {
    var data = JSON.parse(json);
    var general_str = '';
    var details_str = '';
    var labels = {
        'showcert-subject': '発行先 (Subject)',
        'showcert-issuer': '発行者 (Issuer)',
        'showcert-not-before': '有効期限の開始 (Not Before)',
        'showcert-not-after': '有効期限の終了 (Not After)',
        'showcert-version': 'バージョン (Version)',
        'showcert-serial-number': 'シリアル番号 (Serial Number)',
        'showcert-public-key-alg': '公開鍵のアルゴリズム (Public Key Algorithm)',
        'showcert-public-key-alg-params': '公開鍵のアルゴリズムのパラメータ (Public Key Algorithm Parameters)',
        'showcert-public-key-rsa-size': 'RSA 鍵のサイズ (RSA Key Size)',
        'showcert-public-key-rsa-modulus': 'RSA モジュラス (RSA Modulus)',
        'showcert-public-key-rsa-exponent': 'RSA 公開指数 (RSA Public Exponent)', 
        'showcert-public-key': '公開鍵 (Public Key)',
        'showcert-subject-key-identifier': 'X509v3 サブジェクトキー識別子 (Subject Key Identifier)',
        'showcert-key-usage': 'X509v3 キー使用法 (Key Usage)',
        'showcert-subject-alt-name': 'X509v3 サブジェクト代替名 (Subject Alternative Name)',
        'showcert-issuer-alt-name': 'X509v3 発行者代替名 (Issuer Alternative Name)',
        'showcert-basic-constraints': 'X509v3 基本制限 (Basic Constraints)',
        'showcert-crl-distribution-point': 'X509v3 CRL配布ポイント (CRL Distribution Point)',
        'showcert-certificate-policies': 'X509v3 証明書ポリシー (Certificate Policies)',
        'showcert-authority-key-identifier': 'X509v3 機関キー識別子 (Authority Key Identifier)',
        'showcert-extended-key-usage': 'X509v3 拡張キー使用法 (Extended Key Usage)',
        'showcert-authority-information-access': 'X509v3 機関情報アクセス (Authority Information Access)',
        'showcert-sig-alg': '署名アルゴリズム (Signature Algorithm)',
        'showcert-sig-alg-params': '署名アルゴリズムパラメータ (Signature Algorithm Parameters)',
        'showcert-signature': '署名 (Signature)',
        'showcert-fingerprint-sha1': 'SHA1 フィンガープリント (SHA1 Fingerprint)',
        'showcert-fingerprint-sha256': 'SHA256 フィンガープリント (SHA256 Fingerprint)'
        };
    var general_list = ['showcert-subject', 'showcert-issuer', 'showcert-not-before', 'showcert-not-after'];
    var details_list = ['showcert-version', 'showcert-serial-number', 'showcert-subject', 'showcert-issuer',
                        'showcert-not-before', 'showcert-not-after', 'showcert-public-key-alg',
                        'showcert-public-key-alg-params', 'showcert-public-key-rsa-size',
                        'showcert-public-key-rsa-modulus', 'showcert-public-key-rsa-exponent', 'showcert-public-key',
                        'showcert-key-usage', 'showcert-extended-key-usage', 'showcert-subject-alt-name',
                        'showcert-certificate-policies', 'showcert-issuer-alt-name', 'showcert-basic-constraints',
                        'showcert-crl-distribution-point', 'showcert-authority-information-access',
                        'showcert-authority-key-identifier', 'showcert-subject-key-identifier', 'showcert-sig-alg',
                        'showcert-sig-alg-params', 'showcert-signature', 'showcert-fingerprint-sha1',
                        'showcert-fingerprint-sha256'];
    
    var id, label, value;
    var unsupportedOidIndex = 0;
    for (var i in general_list) {
        if (general_list[i] in data && data[general_list[i]]) {
            id = general_list[i];
            label = htmlEscape(labels[general_list[i]]);
            value = htmlEscape(data[general_list[i]]);
            general_str += '\n' + generateFieldLine(id, label, value, 'general_');
        }
    }
    for (var i in details_list) {
        if (details_list[i] in data && data[details_list[i]]) {
            id = details_list[i];
            label = htmlEscape(labels[details_list[i]]);
            value = htmlEscape(data[details_list[i]]);
            details_str += '\n' + generateFieldLine(id, label, value, 'details_');
        }
    }
    while ('showcert-unsupported-oid' + unsupportedOidIndex in data
            && data['showcert-unsupported-oid' + unsupportedOidIndex]) {
        id = 'showcert-unsupported-oid' + unsupportedOidIndex;
        label = htmlEscape(data['showcert-unsupported-oid' + unsupportedOidIndex]);
        if ('showcert-unsupported-oid' + unsupportedOidIndex + '-value' in data
                && data['showcert-unsupported-oid' + unsupportedOidIndex + '-value']) {
            value = htmlEscape(data['showcert-unsupported-oid' + unsupportedOidIndex + '-value']);
            details_str += '\n' + generateFieldLine(id, label, value, 'details_');      
        }
        unsupportedOidIndex++;
    }

    document.getElementById('cert-fields-general').innerHTML = general_str;
    document.getElementById('cert-fields-details').innerHTML = details_str;
    displayTabContent('content_general');
    document.getElementById("tab_general").checked = true;
}

