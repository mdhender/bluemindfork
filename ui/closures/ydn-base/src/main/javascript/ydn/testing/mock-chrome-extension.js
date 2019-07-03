/**
 * @fileoverview Mock chrome extension api.
 */


goog.provide('ydn.testing.mockExtension');


/**
 * @define {string} extension origin
 */
ydn.testing.mockExtension.BASE_URL = '';


ydn.testing.mockExtension.Storage = function() {
  this.data_ = {};
};


ydn.testing.mockExtension.Storage.prototype.get = function(name, cb) {
  var obj = {};
  if (typeof name == 'string') {
    obj[name] = JSON.parse(JSON.stringify(this.data_[name] || null));
  } else {
    for (var i = 0; i < name.length; i++) {
      obj[name[i]] = JSON.parse(JSON.stringify(this.data_[name[i]] || null));
    }
  }
  if (!cb) {
    return;
  }
  setTimeout(function() {
    cb(obj);
  }, 1);

};


ydn.testing.mockExtension.Storage.prototype.set = function(obj, cb) {
  if (obj) {
    for (var name in obj) {
      this.data_[name] = JSON.parse(JSON.stringify(obj[name]));
    }
  } else {
    delete this.data_[name];
  }
  if (cb) {
    setTimeout(function() {
      cb(obj);
    }, 1);
  }
};


ydn.testing.mockExtension.Storage.prototype.clear = function(cb) {
  this.data_ = {};
  if (cb) {
    setTimeout(function() {
      cb();
    }, 1);
  }
};


if (!window.chrome) {
  window.chrome = {};
}


if (!chrome.extension) {
  chrome.extension = {
    'getURL': function(s) {
      if (!s) {
        return ydn.testing.mockExtension.BASE_URL;
      } else if (s.charAt(0) != '/' &&
          ydn.testing.mockExtension.BASE_URL.charAt(0) != '/') {
        s = '/' + s;
      } else if (s.charAt(0) == '/' &&
          ydn.testing.mockExtension.BASE_URL.charAt(0) == '/') {
        s = s.substring(1);
      }
      return ydn.testing.mockExtension.BASE_URL + s;
    }
  };
}

if (!chrome.storage) {
  chrome.storage = {
    'sync': new ydn.testing.mockExtension.Storage(),
    'local': new ydn.testing.mockExtension.Storage(),
    'onChanged': {
      'addListener': function() {}
    }
  };
}

if (!chrome.runtime) {
  chrome.runtime = {};
}
if (!chrome.runtime.getManifest) {
  chrome.runtime.getManifest = function() {
    var xhr = new XMLHttpRequest();
    xhr.open('GET', chrome.extension.getURL('manifest.json'), false);
    var manifest = {};
    xhr.onload = function() {
      manifest = JSON.parse(xhr.responseText);
    };
    xhr.send();
    return manifest;
  };
}

if (!chrome.i18n) {
  chrome.i18n = {
    getMessage: function(s) {
      return s;
    },
    getUILanguage: function() {
      return 'en-US';
    }
  };
}

if (!chrome.permissions) {
  chrome.permissions = {
    request: function(perm, cb) {
      setTimeout(function() {
        if (cb) {cb(true);}
      }, 10);
    },
    contains: function(perm, cb) {
      setTimeout(function() {
        if (cb) {cb(true);}
      }, 10);
    },
    remove: function(perm, cb) {
      setTimeout(function() {
        if (cb) {cb(true);}
      }, 10);
    }
  };
}

if (!window['YathitCrm']) {
  window['YathitCrm'] = {'Version': {'release': '1.4.2'},'sugarcrm': {'Version': {'release': '0.18.3'}}};
}

/**
 * chrome.identity.getAuthToken
 * @type {string}
 */
ydn.testing.mockExtension.auth_token = '123456';
if (!chrome.identity) {
  chrome.identity = {
    getAuthToken: function (detail, cb) {
      setTimeout(function () {
        cb(ydn.testing.mockExtension.auth_token);
      }, 10);
    },
    removeCachedAuthToken: function(detail, cb) {
      setTimeout(function () {
        cb();
      }, 10);
    },
    launchWebAuthFlow: function(detail, cb) {
      setTimeout(function () {
        cb('');
      }, 10);
    },
    onSignInChanged: {
      addListener: function(lis) {

      }
    }
  };
}