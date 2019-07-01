/**
 * @fileoverview About this file
 */

goog.provide('ydn.chrome.alarms');


/**
 *
 * @param {string} name
 * @param {Object} info
 * @return {Alarm}
 */
ydn.chrome.alarms.create = function(name, info) {
  if (goog.global.chrome && goog.global.chrome.alarms) {
    return chrome.alarms.create(name);
  } else {
    return null;
  }
};


ydn.chrome.setTimeout = function(cb, delay, opt) {

};
