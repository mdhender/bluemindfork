/**
 * @fileoverview HTTP client.
 */

goog.provide('ydn.client.base');
goog.require('goog.net.XhrManager');


/**
 * @type {goog.net.XhrManager}
 * @private
 */
ydn.client.base.xhr_manager_;


/**
 * Get singleton xhr manager.
 * @return {goog.net.XhrManager}
 */
ydn.client.base.getXhrManager = function() {
  if (!ydn.client.xhr_manager_) {
    ydn.client.xhr_manager_ = new goog.net.XhrManager();
  }
  return ydn.client.xhr_manager_;
};

