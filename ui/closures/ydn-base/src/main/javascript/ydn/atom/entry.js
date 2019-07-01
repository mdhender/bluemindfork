/**
 * @fileoverview GData entry.
 *
 * @author kyawtun@yathit.com (Kyaw Tun)
 */

goog.provide('ydn.atom.Entry');
goog.require('goog.log');
goog.require('ydn.atom.Atom');
goog.require('ydn.error');



/**
 *
 * @param {!AtomEntry} gdata
 * @constructor
 * @extends {ydn.atom.Atom}
 * @struct
 */
ydn.atom.Entry = function(gdata) {
  goog.base(this, gdata);
};
goog.inherits(ydn.atom.Entry, ydn.atom.Atom);


/**
 * @const
 * @type {string}
 */
ydn.atom.Entry.TYPE = 'ydn.atom.Entry';


/**
 * @param {string} text
 * @return {AtomText}
 */
ydn.atom.Entry.asText = function(text) {
  return /** @type {AtomText} */ (/** @type {Object} */({'$t': text}));
};


