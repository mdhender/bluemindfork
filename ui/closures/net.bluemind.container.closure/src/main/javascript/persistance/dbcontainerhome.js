/**
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

/**
 * @fileoverview Get container data from Web Storage.
 */

goog.provide('net.bluemind.container.persistance.DBContainerHome');

goog.require('net.bluemind.container.persistance.IContainerHome');
goog.require('bluemind.storage.StorageHelper');
goog.require('bluemind.string');
goog.require('goog.array');
goog.require('goog.async.Deferred');
goog.require('goog.async.DeferredList');
goog.require('ydn.db.algo.NestedLoop');
goog.require('ydn.db.Storage');
goog.require('ydn.db.KeyRange');
goog.require('ydn.db.Key');

/**
 * Ask web storage for container data.
 * 
 * @param {ydn.db.Storage} database
 * @implements {net.bluemind.container.persistance.IContainerHome}
 * @constructor
 */
net.bluemind.container.persistance.DBContainerHome = function(database) {
  this.storage_ = database;
};

net.bluemind.container.persistance.DBContainerHome.prototype.markSettingsUpdate = function(id) {
  return this.storage_.put('csettings', {
    'id' : id
  });
};

net.bluemind.container.persistance.DBContainerHome.prototype.settingsApplied = function(id) {
  return this.storage_.remove([ id ]);
};

net.bluemind.container.persistance.DBContainerHome.prototype.getSettingsChanges = function() {
  return this.storage_.values('csettings').then(function(changes) {
    var uids = goog.array.map(changes, function(c) {
      return c['id'];
    });

    return this.getContainers(uids);
  }, null, this).then(function(containers) {
    return goog.array.map(containers, function(c) {
      return {
        'uid' : c['uid'],
        'settings' : c['settings']
      };
    });
  }, null, this);
}

/** @override */
net.bluemind.container.persistance.DBContainerHome.prototype.getContainers = function(opt_uids) {
  return this.storage_.values('container', opt_uids);
};

/** @override */
net.bluemind.container.persistance.DBContainerHome.prototype.getContainer = function(id) {
  return this.storage_.get('container', id);
};

net.bluemind.container.persistance.DBContainerHome.prototype.deleteContainer = function(id) {
  return this.storage_.remove(new ydn.db.Key('container', id)).then(function() {
    return this.storage_.remove('item', 'container', ydn.db.KeyRange.only(id));
  }, null, this).then(function() {
    return this.storage_.remove(new ydn.db.Key('last_sync', id));
  }, null, this).then(function() {
    return this.storage_.remove('changes', 'container', ydn.db.KeyRange.only(id));
  }, null, this);
};

/**
 * @override
 * @suppress {checkTypes}
 */
net.bluemind.container.persistance.DBContainerHome.prototype.sync = function(type, remotes) {
  return this.getContainers().addCallback(function(locals) {
    var luids = goog.array.map(locals, function(c) {
      return c['uid'];
    });
    var ruids = goog.array.map(remotes, function(c) {
      return c['uid'];
    });

    var deletes = goog.array.map(goog.array.filter(luids, function(uid) {
      return !goog.array.contains(ruids, uid);
    }), function(uid) {
      return new ydn.db.Key('container', uid);
    });

    var deferred = goog.async.Deferred.succeed();
    if (deletes.length > 0) {
      deferred.addCallback(function() {
        return this.storage_.remove(deletes);
      }, this);
    }
    return deferred.addCallback(function() {
      return this.storage_.put('container', remotes);
    }, this);
  }, this);

};

/** @override */
net.bluemind.container.persistance.DBContainerHome.prototype.setSyncVersion = function(containerId, version) {

  return this.storage_.put('last_sync', {
    'container' : containerId,
    'version' : version
  });
};

/** @override */
net.bluemind.container.persistance.DBContainerHome.prototype.getSyncVersion = function(containerId) {
  return this.storage_.values('last_sync', [ containerId ]).addCallback(function(lastsyncRows) {
    var version = null;
    if (lastsyncRows.length == 1 && lastsyncRows[0] != undefined) {
      version = lastsyncRows[0].version;
    }
    return version;
  }, this);
};

/**
 * @override
 * @suppress {checkTypes}
 */
net.bluemind.container.persistance.DBContainerHome.prototype.store = function(containers) {
  return this.storage_.put('container', containers);
};
