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

/**
 * @fileoverview Synchronize local storage with bm-core.
 */

goog.provide('net.bluemind.sync.SyncEngine');
goog.provide('bluemind.sync.SyncEngine');

goog.require('bluemind.net.OnlineHandler');
goog.require('bluemind.storage.StorageHelper');
goog.require('net.bluemind.sync.SyncService');
goog.require('goog.array');
goog.require('goog.async.Delay');
goog.require('goog.log');
goog.require('goog.log.Logger');
goog.require('goog.events');
goog.require('goog.events.EventTarget');
goog.require('goog.events.EventHandler');
goog.require('goog.net.EventType');
goog.require('goog.structs.Map');
/**
 * The sync engine is designed to work in background to keep the local storage
 * and the remote server data's synced.
 * 
 * @constructor
 * @extends {goog.events.EventTarget}
 */
net.bluemind.sync.SyncEngine = function() {
  goog.base(this);
  this.delay_ = new goog.async.Delay(this.sync_, net.bluemind.sync.SyncEngine.INTERVAL, this);
  this.services_ = new Array();
  this.attempt_ = 1;
  var that = this;
  this.handler = new goog.events.EventHandler(this);
  restClient.addListener(function(connected) {
    if (connected) {
      that.execute();
    }
  });

};
goog.inherits(net.bluemind.sync.SyncEngine, goog.events.EventTarget);

goog.addSingletonGetter(net.bluemind.sync.SyncEngine);

/**
 * Interval between two synchronizations
 * 
 * @type {number}
 * @const
 */
net.bluemind.sync.SyncEngine.INTERVAL = 30000;

/**
 * Sync in progress
 * 
 * @type {boolean}
 */
net.bluemind.sync.SyncEngine.prototype.syncing = false;

/**
 * @type {goog.log.Logger}
 * @protected
 */
net.bluemind.sync.SyncEngine.prototype.logger = goog.log.getLogger('net.bluemind.sync.SyncEngine');

/**
 * Synchronization services
 * 
 * @type {Array.<net.bluemind.sync.SyncService>}
 * @private
 */
net.bluemind.sync.SyncEngine.prototype.services_;

/**
 * Asynchronus mechanism.
 * 
 * @type {goog.async.Delay}
 * @private
 */
net.bluemind.sync.SyncEngine.prototype.delay_;

/**
 * Sync attempt count.
 * 
 * @type {number} attempt.
 * @private
 */
net.bluemind.sync.SyncEngine.prototype.attempt_;

/**
 * Sync needed flag.
 * 
 * @type {boolean} needSync
 * @private
 */
net.bluemind.sync.SyncEngine.prototype.needSync_ = false;

/**
 * Start the sync engine 'daemon'
 * 
 * @param {number=} opt_interval Optionnal interval for first execution.
 */
net.bluemind.sync.SyncEngine.prototype.start = function(opt_interval) {
  if (!this.delay_.isActive()) {
    this.delay_.start(opt_interval);
  }
};

/**
 * Stop the sync engine.
 */
net.bluemind.sync.SyncEngine.prototype.stop = function() {
  this.delay_.stop();
};

/**
 * Advance the execution of the synchronization to now. SyncEngine sync cannot
 * be execute if the 'daemon' is not started.
 */
net.bluemind.sync.SyncEngine.prototype.execute = function() {
  if (this.delay_.isActive()) {
    this.delay_.fireIfActive();
  } else {
    // already syncinc or not started
    this.needSync = true;
  }
};

/**
 * Add a new service to synchronize
 * 
 * @param {net.bluemind.sync.SyncService} service Service to register.
 * @return {net.bluemind.sync.SyncEngine} current object for chaining commands
 */
net.bluemind.sync.SyncEngine.prototype.registerService = function(service) {
  goog.log.info(this.logger, "Register sync service" + service.getName());
  goog.array.insert(this.services_, service);
  this.handler.listen(service, 'needsync', this.partialSync_);
  return this;
};

net.bluemind.sync.SyncEngine.prototype.partialSync_ = function(e) {
  goog.log.info(this.logger, "Partial sync", e);
  this.doSync_([ e.target ]);
}
/**
 * Remove a synchronized service
 * 
 * @param {net.bluemind.sync.SyncService} service Service to unregister.
 * @return {net.bluemind.sync.SyncEngine} current object for chaining commands
 */
net.bluemind.sync.SyncEngine.prototype.unregisterService = function(service) {
  goog.log.info(this.logger, "Unregister sync service" + service.getName());
  goog.array.remove(this.services_, service);
  this.handler.unlisten(service, 'needsync', this.partialSync_);
  return this;
};

/**
 * Send local update to the remote server. Get updated data from bm-core, and
 * store it into the local storage.
 * 
 * @private
 */
net.bluemind.sync.SyncEngine.prototype.sync_ = function() {
  this.doSync_(this.services_);
};

net.bluemind.sync.SyncEngine.prototype.doSync_ = function(services) {

  if (bluemind.net.OnlineHandler.getInstance().isOnline()) {
    this.syncing = true;
    this.needSync = false;
    if (this.attempt_ == 3) {
      this.dispatchEvent('fail');
    }

    this.notifyStart_();
    try {
      var list = [];
      for (var i = 0; i < services.length; i++) {
        var service = services[i];
        if (service.isEnabled()) {
          list.push(service.sync());
          goog.log.fine(this.logger, '[Global] : ' + service.getName() + ' synchronization service started');
        } else {
          goog.log.fine(this.logger, '[Global] : ' + service.getName() + ' synchronization service is not enabled');
        }
      }
      new goog.async.DeferredList(list).addCallback(function(r) {
        this.notifyStop_();
        this.complete_(r);
      }, this);

      this.attempt_++;
    } catch (e) {
      this.notifyStop_();
      goog.log.error(this.logger, '[Global] : Error during sync scheduling', e);
      this.complete_([ false, e ]);
    }
  } else {
    goog.log.info(this.logger, '[Global] : offline');
    this.attempt_ = 1;
    this.delay_.start(net.bluemind.sync.SyncEngine.INTERVAL);
  }
}

net.bluemind.sync.SyncEngine.prototype.syncCount_ = 0;

net.bluemind.sync.SyncEngine.prototype.notifyStart_ = function() {
  this.syncCount_++;
  this.dispatchEvent('start');
}

net.bluemind.sync.SyncEngine.prototype.notifyStop_ = function() {
  this.syncCount_--;
  if (this.syncCount_ == 0) {
    this.dispatchEvent('stop');
  }
}

/**
 * Called when all service has completed.
 * 
 * @param {Array} result Synchronisation result.
 * @private
 */

net.bluemind.sync.SyncEngine.prototype.complete_ = function(result) {
  try {
    var success = true;
    for (var i = 0; i < result.length; i++) {
      var r = result[i];
      if (!r[0]) {
        success = false;
        goog.log.error(this.logger, 'Synchronisation fatal error', r[1]);
        throw r[1];
      }
    }
    ;
    if (success) {
      this.attempt_ = 1;
    }
  } finally {
    this.syncing = false;
    if (this.needSync) {
      this.sync_();
    } else {
      var delay = net.bluemind.sync.SyncEngine.INTERVAL;
      this.delay_.start(delay);
    }
  }
};

/** @override */
net.bluemind.sync.SyncEngine.prototype.disposeInternal = function() {
  goog.base(this, 'disposeInternal');
  this.delay_.dispose();
  delete this.delay_;
};

bluemind.sync.SyncEngine = net.bluemind.sync.SyncEngine;
