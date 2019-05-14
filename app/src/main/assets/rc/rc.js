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
}

