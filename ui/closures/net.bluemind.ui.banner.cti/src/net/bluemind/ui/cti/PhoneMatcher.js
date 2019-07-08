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
          photo = 'images/nopicture.png';
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
