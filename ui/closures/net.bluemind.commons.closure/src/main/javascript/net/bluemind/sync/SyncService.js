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
 * @fileoverview Synchronization service interface.
 */

goog.provide('net.bluemind.sync.SyncService');
goog.provide('net.bluemind.sync.SyncService.Observer');

goog.require('net.bluemind.concurrent.CrossWindowLock');
goog.require('goog.async.Deferred');
goog.require('goog.events.EventHandler');
goog.require('goog.Timer');
goog.require('goog.Disposable');
goog.require('goog.log');
goog.require('goog.log.Logger');
goog.require('goog.events.EventTarget');
/**
 * Synchronization. A synchrinsation service is in charge of syncing one kind of
 * object.
 * 
 * @constructor
 * @extends {goog.events.EventTarget}
 */
net.bluemind.sync.SyncService = function() {
  goog.base(this);
  this.logger = goog.log.getLogger('net.bluemind.sync.SyncService');
};
goog.inherits(net.bluemind.sync.SyncService, goog.events.EventTarget);

/**
 * @type {goog.log.Logger}
 * @protected
 */
net.bluemind.sync.SyncService.prototype.logger;

/**
 * return sync server unique name.
 * 
 * @return {string} Service name.
 */
net.bluemind.sync.SyncService.prototype.getName = goog.abstractMethod;

/**
 * Send local changes to the server and store remote changes to the local
 * storage. This method
 * 
 * @return {goog.async.Deferred} Sync process.
 */
net.bluemind.sync.SyncService.prototype.sync = function() {
  goog.log.info(this.logger, '[' + this.getName() + '] : start of sync process');
  var sync = new goog.async.Deferred();
  sync.addErrback(this.error, this);
  try {
    var name = this.getName() + 'Sync'
    var lock = new net.bluemind.concurrent.CrossWindowLock(name);

    if (lock.tryLock()) {
      var observer = new net.bluemind.sync.SyncService.Observer(this, sync, lock);
      sync.addBoth(goog.partial(this.complete, observer), this);
      observer.start();
      this.syncInternal().chainDeferred(sync);
    } else {
      goog.log.warning(this.logger, '[' + this.getName() + '] : could not acquire lock');
      sync.callback();
      goog.Timer.callOnce(function() {
        this.needSync();
      }, 1000, this);
    }
  } catch (e) {
    goog.log.warning(this.logger, '[' + this.getName() + '] : error while acquiring lock', e);
    sync.errback(e);
  }
  return sync;
};

/**
 * Internal synchronisation process. This method must be override. This method
 * is the critical section of the sync lock.
 * 
 * @return {goog.async.Deferred}
 * @protected
 */
net.bluemind.sync.SyncService.prototype.syncInternal = goog.abstractMethod

/**
 * If this method return false, the sync service will not try to sync even if
 * registered in the sync engine
 * 
 * @return {boolean}
 */
net.bluemind.sync.SyncService.prototype.isEnabled = function() {
  return true;
};

net.bluemind.sync.SyncService.prototype.needSync = function() {
  this.dispatchEvent('needsync');
}
/**
 * Synchronisation has failed
 * 
 * @param {Error} error Sync result.
 * @protected
 */
net.bluemind.sync.SyncService.prototype.error = function(error) {
  goog.log.error(this.logger, '[' + this.getName() + '] : Failure during sync process', error);
  if (error && (error == 401 || error.errorCode == 'FORBIDDEN' || error.errorCode == 'AUTHENTICATION_FAIL')) {
    var uri = goog.global.location.pathname;
    goog.global.location.assign('/login/index.html?askedUri=' + goog.string.urlEncode(uri));
  }

  return error;
};

/**
 * Synchronisation has complete
 * 
 * @param {*} result Sync result
 * @protected
 */
net.bluemind.sync.SyncService.prototype.complete = function(observer, result) {
  var duration = goog.now() - observer.ts;
  observer.dispose();
  goog.log.info(this.logger, '[' + this.getName() + '] : end of sync process (' + duration + 'ms)');
};

/** @override */
net.bluemind.sync.SyncService.prototype.disposeInternal = function() {
  goog.base(this, 'disposeInternal');
};

/**
 * Monitor for synchronization tasks
 * 
 * @param {net.bluemind.sync.SyncService} service Parent service
 * @param {goog.async.Deferred} sync Sync task
 * @param {net.bluemind.concurrent.CrossWindowLock} lock Sync task lock.
 * @constructor
 * @extends {goog.Disposable}
 */
net.bluemind.sync.SyncService.Observer = function(service, sync, lock) {
  goog.base(this);
  this.name = service.getName() + ".Obeserver-" + goog.getUid(sync);
  this.sync = sync.addBoth(this.dispose, this);
  this.lock = lock;
  this.timer_ = new goog.Timer(100);
  this.handler_ = new goog.events.EventHandler(this);
  this.handler_.listen(this.timer_, goog.Timer.TICK, this.refreshLock);
};
goog.inherits(net.bluemind.sync.SyncService.Observer, goog.Disposable);

/**
 * Lock to ensure that there is no more than one synchronisation for all
 * bluemind applications instances at the same time
 * 
 * @type {net.bluemind.concurrent.CrossWindowLock}
 */
net.bluemind.sync.SyncService.Observer.prototype.lock;

/**
 * Start timestamp.
 * 
 * @type {number}
 */
net.bluemind.sync.SyncService.Observer.prototype.ts;

/**
 * Observer name
 * 
 * @type {string}
 * @protected
 */
net.bluemind.sync.SyncService.Observer.prototype.name;

/**
 * @type {goog.log.Logger}
 * @protected
 */
net.bluemind.sync.SyncService.Observer.prototype.logger = goog.log.getLogger('net.bluemind.sync.SyncService.Observer');

/**
 * Synchronization task.
 * 
 * @type {goog.async.Deferred}
 */
net.bluemind.sync.SyncService.Observer.prototype.sync;

/**
 * Event handler to easily manage event listening
 * 
 * @type {goog.events.EventHandler}
 * @private
 */
net.bluemind.sync.SyncService.Observer.prototype.handler_;

/**
 * Refresh timer for lock.
 * 
 * @type {goog.Timer}
 * @private
 */
net.bluemind.sync.SyncService.Observer.prototype.timer_;

/**
 * Check if task is alive : if so Check if lock is available : if so refresh
 * lock if not cancel the sync. if not (should) kill the task and cancel the
 * sync
 * 
 * @private
 */
net.bluemind.sync.SyncService.Observer.prototype.refreshLock = function() {
  if (!this.lock.tryLock()) {
    goog.log.warning(this.logger, '[' + this.name + '] : lock timer is on but lock has been acquired by someone else');
    if (this.sync.hasFired()) {
      this.sync.cancel(true);
    } else {
      this.sync.errback(new Error('[' + this.name + '] The lock has been stolen by someone else.'));
    }
  } else {
    goog.log.fine(this.logger, '[' + this.name + '] : refresh lock');
  }
};

/**
 * Start observing task
 */
net.bluemind.sync.SyncService.Observer.prototype.start = function() {
  this.timer_.start();
  this.ts = goog.now();
};

/** @override */
net.bluemind.sync.SyncService.Observer.prototype.disposeInternal = function() {
  goog.base(this, 'disposeInternal');
  if (this.lock)
    this.lock.dispose();
  this.handler_.dispose();
  this.timer_.dispose();
  this.timer_ = this.lock = this.handler_ = null;
};
