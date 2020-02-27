function dlstr2html(str) {
    if (typeof str == "string") {
        return str;
    }
    var html = "";
    const style = 'height: 1em; position: relative; top: 0.14em;';
    for (c of str) {
        if (c['type'] == 'text/plain') {
            html += htmlEscape(c['value']);
        } else if (c['type'] == 'image/png') {
            html += '<img style="' + style + '" ' +
                'src="data:image/png;base64,' + c['value'] +  '" />';
        } else if (c['type'] == 'image/x-missing') {
            // 欠字
            html += "？";
        }
    }
    return html;
}

