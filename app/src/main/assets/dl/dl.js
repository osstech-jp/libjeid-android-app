
function addMessage(msg)
{
    var area = document.getElementById("msg")
    if (area) {
        area.innerHTML += htmlEscape(msg) + '<br/>';
    }
}

function clearMessage()
{
    var area = document.getElementById("msg")
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

function full2half(str) {
    return str.replace(/[ａ-ｚ０-９（）]/g, function(s) {
        return String.fromCharCode(s.charCodeAt(0) - 0xFEE0);
    });
}

function render(json) {
    data = JSON.parse(json);
    if ('dl-name' in data) {
        document.getElementById("dl-name").innerHTML = dlstr2html(data['dl-name']);
    }
    if ('dl-birth' in data) {
        document.getElementById("dl-birth").innerHTML = htmlEscape(data['dl-birth'])
            + '生';
    }
    if ('dl-addr' in data) {
        document.getElementById("dl-addr").innerHTML = dlstr2html(data['dl-addr']);
    }
    if ('dl-issue' in data) {
        document.getElementById("dl-issue").innerHTML = htmlEscape(data['dl-issue']);
    }
    if ('dl-ref' in data) {
        document.getElementById("dl-ref").innerHTML = htmlEscape(data['dl-ref']);
    }
    if ('dl-expire' in data) {
        document.getElementById("dl-expire").innerHTML = htmlEscape(data['dl-expire'])
            + 'まで有効';
    }
    if ('dl-is-expired' in data) {
      if (data['dl-is-expired']) {
        document.getElementById("dl-is-expired").style.display ="block";
      }
    }
    if ('dl-color-class' in data) {
        var color;
        var display;
        if (data['dl-color-class'] == '優良') {
            color = 'dl-color-gold';
            display = 'inline';
        } else if (data['dl-color-class'] == '新規') {
            color = 'dl-color-green';
            display = 'none';
        } else {
            color = 'dl-color-blue';
            display = 'none';
        }
        document.getElementById("dl-expire").classList.add(color);
        document.getElementById("dl-color-class").style.display = display;
    }

    var elm = document.getElementById('dl-condition1');
    if ('dl-condition1' in data) {
        elm.innerHTML = full2half(data['dl-condition1']);
    } else {
        elm.innerHTML = '';
    }

    var elm = document.getElementById('dl-condition2');
    if ('dl-condition2' in data) {
        elm.innerHTML = full2half(data['dl-condition2']);
    } else {
        elm.innerHTML = '';
    }

    var elm = document.getElementById('dl-condition3');
    if ('dl-condition3' in data) {
        elm.innerHTML = full2half(data['dl-condition3']);
    } else {
        elm.innerHTML = '';
    }

    var elm = document.getElementById('dl-condition4');
    if ('dl-condition4' in data) {
        elm.innerHTML = full2half(data['dl-condition4']);
    } else {
        elm.innerHTML = '';
    }

    if ('dl-number' in data) {
        document.getElementById("dl-number").innerHTML = '第　' + htmlEscape(data['dl-number']) + '　号';
    }
    if ('dl-sc' in data) {
        document.getElementById("dl-sc").innerHTML = htmlEscape(data['dl-sc']);
    }
    if ('dl-photo' in data) {
        document.getElementById("dl-photo").src = data['dl-photo'];
    }

    if ('dl-verified' in data) {
        document.getElementById("dl-verified").style.display = "inline-block";
        if (data['dl-verified']) {
            //document.getElementById("dl-verified").innerHTML = "✔";
            document.getElementById("dl-verified").src = "verify-success.png";
        } else {
            //document.getElementById("dl-verified").innerHTML = "✖";
            document.getElementById("dl-verified").src = "verify-failed.png";
        }
    }else{
        document.getElementById("dl-verified").style.display = "none";
    }

    var elm = document.getElementById("dl-name-etc");
    elm.innerHTML = '<tr><th style="width: 30%"></th><th style="width: 70%"></th></tr>\n';
    if ('dl-name' in data) {
        elm.innerHTML += '<tr><td>氏名</td><td>' +
            dlstr2html(data['dl-name']) +
            '</td></tr>\n';
    }
    if ('dl-kana' in data) {
        elm.innerHTML += "<tr><td>呼び名(カナ)</td><td>" +
            htmlEscape(data['dl-kana']) +
            "</td></tr>\n";
    }
    if ('dl-addr' in data) {
        elm.innerHTML += "<tr><td>住所</td><td>" +
            dlstr2html(data['dl-addr']) +
            "</td></tr>\n";
    }
    if ('dl-registered-domicile' in data) {
        elm.innerHTML += "<tr><td>本籍</td><td>" +
            dlstr2html(data['dl-registered-domicile']) +
            "</td></tr>\n";
    }

    var elm = document.getElementById("dl-categories");
    elm.innerHTML = '<tr><th style="width: 30%"></th><th style="width: 70%"></th></tr>\n';
    if ('dl-categories' in data) {
        var categories = data['dl-categories'];
        var traction = false;
        var traction2 = false;
        for(var i=0; i<categories.length; i++) {
            var cat = categories[i];
            var html = "<tr><td>" +
                htmlEscape(cat['name']) +
                "</td><td>" +
                (cat['licensed']?htmlEscape(cat['date']):'なし') +
                "</td></tr>\n";
            elm.innerHTML += html;

            switch(cat['tag']) {
            case 0x22:
                document.getElementById("dl-cat-22-date").innerHTML = cat['date'];
                break;
            case 0x23:
                document.getElementById("dl-cat-23-date").innerHTML = cat['date'];
                break;
            case 0x24:
                document.getElementById("dl-cat-24-date").innerHTML = cat['date'];
                break;
            case 0x25:
            case 0x26:
            case 0x27:
            case 0x28:
            case 0x29:
            case 0x2a:
            case 0x2b:
            case 0x2d:
            case 0x2e:
            case 0x2f:
            case 0x31:
            case 0x32:
            case 0x33:
                if(cat['licensed']){
                    var id = 'dl-cat-' + cat['tag'].toString(16) + '-text';
                    var catElm = document.getElementById(id);
                    if (catElm) {
                        catElm.style.display = "block";
                    }
                }
                break;
            case 0x2c:
                traction = cat['licensed'];
                break;
            case 0x30:
                traction2 = cat['licensed'];
                break;
            }
        }
        if (traction || traction2) {
            var catElm = document.getElementById('dl-cat-30-text');
            if (catElm) {
                catElm.style.display = "block";
                if (traction && !traction2) {
                    catElm.className = "dl-cat-cell-2";
                    catElm.innerHTML = "け引";
                } else if (!traction && traction2) {
                    catElm.className = "dl-cat-cell-3";
                    catElm.innerHTML = "け引二";
                } else if (traction && traction2) {
                    catElm.className = "dl-cat-cell-3";
                    catElm.innerHTML = "引<div class=\"dl-cat-dot\"></div>引二";
                }
            }
        }
    }

    var elm = document.getElementById("dl-signature");
    elm.innerHTML = '<tr><th style="width: 30%"></th><th style="width: 70%"></th></tr>\n';
    if ('dl-signature-subject' in data) {
        elm.innerHTML += '<tr><td>Subject</td><td>' +
            htmlEscape(data['dl-signature-subject']) +
            '</td></tr>\n';
    }
    if ('dl-signature-ski' in data) {
        elm.innerHTML += "<tr><td>Subject Key Identifier</td><td>" +
            htmlEscape(data['dl-signature-ski']) +
            "</td></tr>\n";
    }
    if ('dl-verified' in data) {
        if (data['dl-verified']) {
            elm.innerHTML += "<tr><td>署名検証</td><td>成功</td>";
        }else{
            elm.innerHTML += "<tr><td>署名検証</td><td>失敗</td>";
        }
    }

    var elm = document.getElementById('dl-changes');
    if ('dl-changes' in data) {
        var changes = data['dl-changes'];
        changes.sort(function(a, b) {
            if (a['ad'] > b['ad']) {
                return 1;
            } else if (a['date'] < b['date']) {
                return -1;
            } else {
                return 0;
            }
        });
        for(var i=0; i<changes.length; i++) {
            var label = changes[i]['label'];
            var value = changes[i]['value'];
            var psc = changes[i]['psc'];
            var date = changes[i]['date'];
            if (i == 0) {
                date += "<br/>\n";
            } else {
                date += "&nbsp;";
            }
            elm.innerHTML += date + htmlEscape(label) + "："
                + dlstr2html(value) + "<div class=\"dl-changes-seal\">" + htmlEscape(psc) + "</div><br/>";
        }
    }
}

