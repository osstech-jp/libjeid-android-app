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
        document.getElementById("rc-front-image").src = data['rc-front-image'];
        document.getElementById("rc-card-style").style.backgroundImage = "url(" + data['rc-front-image'] + ")";
        document.getElementById("rc-card-style").style.backgroundSize = "contain";
    }
    if ('rc-photo' in data) {
        document.getElementById("rc-photo").src = data['rc-photo'];
        document.getElementById("rc-card-photo").src = data['rc-photo'];
    }
    if ('rc-card-type' in data) {
      if (data['rc-card-type'] == '1') {
        document.getElementById("rc-rc-labels").style.display = "block";
      } else if (data['rc-card-type'] == '2') {
        document.getElementById("rc-sprc-labels").style.display = "block";
      }
    }
}

