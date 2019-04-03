var monthMap = {
    '01': 'JAN', '02': 'FEB', '03': 'MAR', '04': 'APR',
    '05': 'MAY', '06': 'JUN', '07': 'JUL', '08': 'AUG',
    '09': 'SEP', '10': 'OCT', '11': 'NOV', '12': 'DEC'
}

function addMessage(msg)
{
    var area = document.getElementById("msg");
    if (area) {
        area.innerHTML += htmlEscape(msg) + '<br/>';
    }
}

function clearMessage()
{
    var area = document.getElementById("msg");
    if (area) {
        area.innerHTML = '';
    }
}

function htmlEscape(str) {
    return str.replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function render(json) {
    data = JSON.parse(json);
    if ('ep-type' in data) {
        var type = data['ep-type'].replace(/</g, '');
        document.getElementById("ep-type").innerHTML = htmlEscape(type);
    }
    if ('ep-issuing-country' in data) {
        var issuerCountry = data['ep-issuing-country'];
        document.getElementById("ep-issuing-country").innerHTML = htmlEscape(issuerCountry);
    }
    if ('ep-passport-number' in data) {
        var passportNumber = data['ep-passport-number'];
        document.getElementById("ep-passport-number").innerHTML = htmlEscape(passportNumber);
    }
    if ('ep-surname' in data) {
        document.getElementById("ep-surname").innerHTML = htmlEscape(data['ep-surname']);
    }
    if ('ep-given-name' in data) {
        document.getElementById("ep-given-name").innerHTML = htmlEscape(data['ep-given-name']);
    }
    if ('ep-nationality' in data) {
        if (data['ep-nationality'] == 'JPN') {
            document.getElementById("ep-nationality").innerHTML = 'JAPAN';
        } else {
            document.getElementById("ep-nationality").innerHTML = htmlEscape(data['ep-nationality']);
        }
    }
    if ('ep-date-of-birth' in data) {
        var birthYear = data['ep-date-of-birth'].substr(0, 2);
        var birthMonth = data['ep-date-of-birth'].substr(2, 2);
        var birthDay = data['ep-date-of-birth'].substr(4, 2);
        document.getElementById("ep-date-of-birth").innerHTML
                = birthDay + "&ensp;" + monthMap[birthMonth] + "&ensp;XX" + birthYear;
    }
    if ('ep-sex' in data) {
        document.getElementById("ep-sex").innerHTML = htmlEscape(data['ep-sex']);
    }
    if ('ep-date-of-expiry' in data) {
        var expiryYear = "20" + data['ep-date-of-expiry'].substr(0, 2);
        var expiryMonth = data['ep-date-of-expiry'].substr(2, 2);
        var expiryDay = data['ep-date-of-expiry'].substr(4, 2);
        document.getElementById("ep-date-of-expiry").innerHTML
                = expiryDay + "&ensp;" + monthMap[expiryMonth] + "&ensp;" + expiryYear;
        if (passportNumber && issuerCountry == 'JPN') {
            var initial = passportNumber.substr(0, 1);
            if (initial == "M" || initial == "N") {
                document.getElementById("ep-date-of-issue").innerHTML
                        = expiryDay + "&ensp;" + monthMap[expiryMonth] + "&ensp;" + (expiryYear - 5);
            } else if (initial == "T") {
                document.getElementById("ep-date-of-issue").innerHTML
                        = expiryDay + "&ensp;" + monthMap[expiryMonth] + "&ensp;" + (expiryYear - 10);
            }
        }
    }
    if ('ep-mrz' in data) {
        var mrzTable = '<table class="ep-mrz-table"><tr>';
        for (var i = 0; i < data['ep-mrz'].length; i++) {
            mrzTable += '<td>' + data['ep-mrz'].substr(i, 1) + '</td>';
            if (i == (data['ep-mrz'].length / 2 | 0) - 1) {
              mrzTable += '</tr>\n<tr>'
            }
        }
        mrzTable += '</tr></table>';
        document.getElementById("ep-mrz").innerHTML = mrzTable;
    }
    if ('ep-photo' in data) {
        document.getElementById("ep-photo").src = data['ep-photo'];
    }

    var elm = document.getElementById("ep-authenticity-table");
    elm.innerHTML = '<tr><th></th><th></th></tr>\n';
    var tableLine;
    var successImg = '<img class="ep-authenticity-verified" src="verify-success.png"/>';
    var failedImg = '<img class="ep-authenticity-verified" src="verify-failed.png"/>';
    var unexecutedImg = '<img class="ep-authenticity-verified" src="verify-unexecuted.png"/>';
    tableLine = '<tr><td>';
    if ('ep-bac-result' in data) {
        if (data['ep-bac-result']) {
            tableLine += successImg;
        } else {
            tableLine += failedImg;
        }
    } else {
        tableLine += unexecutedImg;
    }
    tableLine += '</td><td>Basic Access Control</td></tr>\n';
    elm.innerHTML += tableLine;
    tableLine = '<tr><td>';
    if ('ep-aa-result' in data) {
        if (data['ep-aa-result']) {
            tableLine += successImg;
        } else {
            tableLine += failedImg;
        }
    } else {
        tableLine += unexecutedImg;
    }
    tableLine += '</td><td>Active Authentication</td></tr>\n';
    elm.innerHTML += tableLine;
    tableLine = '<tr><td>';
    if ('ep-pa-result' in data) {
        if (data['ep-pa-result']) {
            tableLine += successImg;
        } else {
            tableLine += failedImg;
        }
    } else {
        tableLine += unexecutedImg;
    }
    tableLine += '</td><td>Passive Authentication</td></tr>\n';
    elm.innerHTML += tableLine;
}

