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
 * @fileoverview Handle appcache event.
 */

goog.provide('net.bluemind.net.AppCacheHandler');

goog.require('net.bluemind.mvp.Application');
goog.require('goog.async.Delay');
goog.require('goog.Disposable');
goog.require('goog.dom');
goog.require('goog.events.EventHandler');
goog.require('goog.events.EventType');

/**
 * Handle application cache event.
 * 
 * @extends {goog.Disposable}
 * @constructor
 */
net.bluemind.net.AppCacheHandler = function() {
  goog.base(this);
  this.handler_ = new goog.events.EventHandler(this);
  this.registerDisposable(this.handler_);
  this.appcaches_ = [];
  this.try_ = 0;
  this.handler_.listenOnce(goog.dom.getWindow(), goog.events.EventType.LOAD, function() {
    this.add_(goog.dom.getWindow());
    goog.array.forEach(/** @type {goog.array.ArrayLike} */
    (goog.dom.getWindow().frames), function(frame) {
      this.add_(frame);
    }, this);
  });
};
goog.inherits(net.bluemind.net.AppCacheHandler, goog.Disposable);

/**
 * Event handler to simplify event listening.
 * 
 * @type {goog.events.EventHandler}
 * @private
 */
net.bluemind.net.AppCacheHandler.prototype.handler_;

/**
 * Number of retry to download the appcache
 * 
 * @type {number}
 * @private
 */
net.bluemind.net.AppCacheHandler.prototype.try_;

/**
 * Window application cache.
 * 
 * @type {Array.<DOMApplicationCache>}
 * @private
 */
net.bluemind.net.AppCacheHandler.prototype.appcaches_;

/**
 * Listen to window's application cache
 * 
 * @param {Window} win Window containing the appcache to listen.
 */
net.bluemind.net.AppCacheHandler.prototype.add_ = function(win) {
  var appcache = win.applicationCache;
  if (appcache) {
    appcache.src = win.location.href;
    this.appcaches_.push(appcache);
    this.handler_.listen(appcache, 'cached', this.handleCacheEvent_);
    this.handler_.listen(appcache, 'checking', this.handleCacheEvent_);
    this.handler_.listen(appcache, 'downloading', this.handleCacheEvent_);
    this.handler_.listen(appcache, 'error', this.handleCacheError_);
    this.handler_.listen(appcache, 'noupdate', this.handleCacheEvent_);
    this.handler_.listenOnce(appcache, 'noupdate', this.forceRecheck_);
    this.handler_.listen(appcache, 'obsolete', this.handleCacheEvent_);
    this.handler_.listen(appcache, 'progress', this.handleCacheEvent_);
    this.handler_.listen(appcache, 'updateready', this.handleCacheEvent_);
    this.handleCacheStatus_(appcache);
  }
};

/**
 * Check if a new cache is available on page load.
 * 
 * @param {DOMApplicationCache} appcache Application cache.
 * @private
 */
net.bluemind.net.AppCacheHandler.prototype.handleCacheUpdate_ = function(appcache) {
  appcache.swapCache();
  net.bluemind.mvp.Application.reload();
};

/**
 * Handle appcache event
 * 
 * @param {goog.events.Event} e Appcache event.
 */
net.bluemind.net.AppCacheHandler.prototype.handleCacheEvent_ = function(e) {
  var appcache = /** @type {DOMApplicationCache} */ (e.target);
  this.handleCacheStatus_(appcache);
};

/**
 * Force to re-check the cache. Some time the manifest is not considered as an
 * update even though it has changed
 * 
 * @param {goog.events.Event} e Appcache event.
 */
net.bluemind.net.AppCacheHandler.prototype.forceRecheck_ = function(e) {
  var appcache = e.target;
  if (appcache.IDLE == 1 && appcache.status == appcache.IDLE) {
    new goog.async.Delay(function() {try {appcache.update();} catch(e) {}}, 2000).start();
  }
};

/**
 * Handle appcache error.
 * 
 * @param {goog.events.Event} e Appcache event.
 * @private
 */
net.bluemind.net.AppCacheHandler.prototype.handleCacheError_ = function(e) {
  var appcache = e.target;
  this.try_++;
  if (appcache.IDLE == 1 && this.try_ < 3) {
    new goog.async.Delay(function() {try {appcache.update();} catch(e) {}}, 500).start();
  }
};

/**
 * Handle appcache status.
 * 
 * @param {DOMApplicationCache} appcache Application cache.
 * @private
 */
net.bluemind.net.AppCacheHandler.prototype.handleCacheStatus_ = function(appcache) {
  if (appcache.IDLE == 1) {
    switch (appcache.status) {
    case appcache.UNCACHED:
      // Should not be possible
      break;
    case appcache.IDLE:
      break;
    case appcache.CHECKING:
      // Checking for update
      break;
    case appcache.DOWNLOADING:
      // Update founded and downloading
      break;
    case appcache.UPDATEREADY:
      // Update successfully downloaded
      this.handleCacheUpdate_(appcache);
      break;
    case appcache.OBSOLETE:
      // Manifest does not exist anymore on server 
      break;
    default:
      // Uknown 
      break;
    }
  }
};
