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
    if ('rc-front-image' in data) {
        document.getElementById("rc-card-front").src = data['rc-front-image'];
    }
    if ('rc-photo' in data) {
        document.getElementById("rc-card-photo").src = data['rc-photo'];
    } else {
        document.getElementById("rc-card-photo").classList.add("null-image");
    }
    if ('rc-card-type' in data) {
        switch(data['rc-card-type']) {
            case '1':
                document.getElementById("rc-rc-labels").style.display = "block";
                document.getElementById("rc-sprc-labels").style.display = "none";
                break;
            case '2':
                document.getElementById("rc-rc-labels").style.display = "none";
                document.getElementById("rc-sprc-labels").style.display = "block";
        }
    }
}

