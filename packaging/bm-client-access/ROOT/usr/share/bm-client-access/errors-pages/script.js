function loadPage() {
  if (window.i18n && document.readyState == 'complete') {
    var elements = document.querySelectorAll('[data-l10n]');
    for (var i = 0; i < elements.length; i++) {
      var key = elements[i].getAttribute('data-l10n');
      elements[i].innerHTML = window.i18n[key];
    }
    var button = document.getElementById('redirect');
    button.className = 'big';
    button.disabled = false;    
  }
}

function loadL10n() {
  var locals = (window.navigator.languages || [window.navigator.language || window.navigator.browserLanguage || window.navigator.userLanguage]);
  locals = locals.map(function(l10n) {
    var language = (l10n.split(/[-_\W]/).shift());
    if (language == 'en') {
      return 'messages.properties';
    } else {
      return 'messages_' + language + '.properties';
    }
  });
  locals.push('messages.properties');
  locals = locals.reduce(function(unique, file) {
    if (unique.indexOf(file) < 0) {
      unique.push(file);
    }
    return unique;
  }, []);
  getL10n(locals);
}

function getL10n(locals) {
  var local = locals.shift();
  var xhr = new XMLHttpRequest();
  xhr.onreadystatechange = function () {
    if(xhr.readyState < 4) {
      return;
    }
    if(xhr.status !== 200) {
        return getL10n(locals);
    }
    if(xhr.readyState === 4) {
      window.i18n = parseProperties(xhr.responseText);
      loadPage();
    }           
  }
  var url = '/errors-pages/l10n/' + local;
  xhr.open('GET', url, true);
  xhr.send('');
};


function parseProperties(properties) {
  var lines = properties.split(/\r?\n/);
  var map = {};
  var property = '';
  lines.forEach(function(line) {
    if (/^\s*(\#|\!|$)/.test(line)) {
      return;
    }
    property += line;
    if (/(\\\\)*\\$/.test(property)) {
      return;
    }
    var match = /^\s*((?:[^\s:=\\]|\\.)+)\s*[:=\s]\s*(.*)$/.exec(property);
    map[match[1].trim()] = match[2].trim()
    property = '';
  });
  return map;
};

function retry(){
  setTimeout(function() {
    window.location.reload();
  }, 5000);
  var button = document.getElementById('redirect');
  button.className = 'big disabled loading';
  return false;
};


function home(){
  window.location.href = '/';
  return false;
};
