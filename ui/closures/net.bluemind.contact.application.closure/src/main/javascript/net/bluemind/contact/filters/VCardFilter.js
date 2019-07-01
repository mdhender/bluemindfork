/*
 * BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */

goog.provide("net.bluemind.contact.filters.VCardFilter");

goog.require("net.bluemind.mvp.Filter");

/**
 * @constructor
 * 
 * @extends {net.bluemind.mvp.Filter}
 */
net.bluemind.contact.filters.VCardFilter = function() {
  net.bluemind.mvp.Filter.call(this);
};
goog.inherits(net.bluemind.contact.filters.VCardFilter, net.bluemind.mvp.Filter);

net.bluemind.contact.filters.VCardFilter.prototype.priority = 50;

net.bluemind.contact.filters.VCardFilter.prototype.filter = function(ctx) {
  var path = ctx.uri.getPath().toLowerCase();
  var check = false;
  if (goog.string.startsWith(path, '/vcard') || goog.string.startsWith(path, '/individual')) {
    path = '/individual';
    check = true;
  }
  if (goog.string.startsWith(path, '/group')) {
    check = true;
    path = '/group';
  }
  if (check) {
    var container = ctx.params.get('container');
    var uid = ctx.params.get('uid');

    // No containers or default containers
    if (!container) {
      container = ctx.session.get('addressbook.default');
    }

    var addressbook = goog.array.find(ctx.session.get('addressbooks'), function(adb) {
      return (adb['uid'] == container);
    });

    if (!addressbook) {
      ctx.helper('url').goTo('/');
    } else if (addressbook['writable'] && !uid) {
      ctx.helper('url').redirect(path + '/edit/?container=' + container);
    } else if (!uid) {
      ctx.helper('url').redirect(path + '/edit/?container=' + ctx.session.get('addressbook.default'));
    } else {
      return ctx.service('addressbook').getItem(container, uid).then(function(vcard) {
        if (!vcard) {
          // FIXME: UID does not exist and container is not writable.
          // => Error ?
          // => replace container to default one ?
          // Anyway a message must be shown.
          ctx.helper('url').goTo('/');
          return;
        }
        var kind = vcard['value']['kind'] || 'individual';
        var url = '/';
        if (kind == 'individual' || kind == 'group') {
          if (addressbook['writable'] && !addressbook['readOnly']) {
            var url = '/' + kind + '/edit/?uid=' + uid + '&container=' + container;
          } else {
            var url = '/' + kind + '/consult/?uid=' + uid + '&container=' + container;
          }
        } else {
          // +FIXME : Error message unhandled vcard type
          var url = '/individual/consult/?uid=' + uid + '&container=' + container;
        }
        ctx.helper('url').redirect(url);
      });
    }
  }
};
