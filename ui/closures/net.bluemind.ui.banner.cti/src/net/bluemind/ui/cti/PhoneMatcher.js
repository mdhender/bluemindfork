goog.provide("net.bluemind.ui.cti.PhoneMatcher");

goog.require('net.bluemind.addressbook.api.AddressBooksClient');
goog.require('net.bluemind.addressbook.api.AddressBookClient');
goog.require('relief.rpc.RPCService');
goog.require('goog.async.DeferredList');
goog.require('relief.cache.Cache');
goog.require("goog.Disposable");
goog.require("goog.array");

/**
 * @constructor
 * @extends {goog.Disposable}
 */
net.bluemind.ui.cti.PhoneMatcher = function() {
  goog.base(this);
};
goog.inherits(net.bluemind.ui.cti.PhoneMatcher, goog.Disposable);

/**
 * Returns whether the suggestions should be updated? <b>Override this to
 * prevent updates eg - when token is empty.</b>
 * 
 * @param {string} token Current token in autocomplete.
 * @param {number} maxMatches Maximum number of matches required.
 * @param {string=} opt_fullString Complete text in the input element.
 * @return {boolean} Whether new matches be requested.
 * @protected
 */
net.bluemind.ui.cti.PhoneMatcher.prototype.shouldRequestMatches = function(token, maxMatches, opt_fullString) {
  return true;
};

/**
 * Handles the XHR response.
 * 
 * @param {string} token The XHR autocomplete token.
 * @param {Function} matchHandler The AutoComplete match handler.
 * @param {Array.<Object>} result Search result.
 */
net.bluemind.ui.cti.PhoneMatcher.prototype.onMatch = function(token, matchHandler, result) {
  matchHandler(token, result);
};

/**
 * Retrieve a set of matching rows from the server via ajax.
 * 
 * @param {string} token The text that should be matched; passed to the server
 *          as the 'token' query param.
 * @param {number} maxMatches The maximum number of matches requested from the
 *          server; passed as the 'max_matches' query param. The server is
 *          responsible for limiting the number of matches that are returned.
 * @param {Function} matchHandler Callback to execute on the result after
 *          matching.
 * @param {string=} opt_fullString The full string from the input box.
 */
net.bluemind.ui.cti.PhoneMatcher.prototype.requestMatchingRows = function(token, maxMatches, matchHandler,
    opt_fullString) {

  if (!this.shouldRequestMatches(token, maxMatches, opt_fullString)) {
    return;
  }

  var callback = goog.bind(this.onMatch, this, token, matchHandler);

  var rpc = new relief.rpc.RPCService(new relief.cache.Cache(), new goog.structs.Map({
    'X-BM-ApiKey' : goog.global['bmcSessionInfos']['sid'],
    'Accept' : 'application/json'
  }));
  var client = new net.bluemind.addressbook.api.AddressBooksClient(rpc, '');
  var escapedToken = token.replace(/([:+\-!\(\){}\[\]^"~*?\\]|[&\|]{2})/g,  "\\$1");
  var query = {
    'from' : 0,
    'size' : 10,
    'escapeQuery' : false,
    'query' : '_exists_: value.communications.tels.value ' + //
    ' AND ( value.identification.formatedName.value:' + escapedToken + //
    ' OR value.communications.tels.value:' + escapedToken + ' )'
  };

  client.search(query).then(function(res) {
    var futures = goog.array.map(res['values'], function(entry) {
      var abClient = new net.bluemind.addressbook.api.AddressBookClient(rpc, '', entry['containerUid']);
      return abClient.getComplete(entry['uid']).then(function(n) {
        n['container'] = entry['containerUid'];
        return n;
      })
    });

    return goog.Promise.all(futures);
  }).then(function(items) {
    return goog.array.flatten(

    goog.array.map(items, function(item) {
      var photo = null;
      if (item['value']['identification']['photo']) {
        photo = '/api/addressbooks/' + item['container'] + '/' + item['uid'] + '/photo';
      } else {
        if (item['value']['kind'] == 'group') {
          photo = 'images/no-dlist-picture.png';
        } else {
          photo = NO_PICTURE;
        }
      }
      return goog.array.map(item['value']['communications']['tels'], function(tel) {
        return {
          'photo' : photo,
          'phone' : tel['value'],
          'name' : item['value']['identification']['formatedName']['value']
        };
      });
    }));
  }).then(callback);

};

var NO_PICTURE = "data:image/png;base64,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";
