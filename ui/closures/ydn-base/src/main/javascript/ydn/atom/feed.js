/**
 * Created with IntelliJ IDEA.
 * User: mbikt
 * Date: 9/21/12
 * Time: 1:04 PM
 * To change this template use File | Settings | File Templates.
 */

goog.provide('ydn.atom.Feed');
goog.require('ydn.atom.Atom');


/**
 * @param {AtomFeed} data
 * @constructor
 * @extends {ydn.atom.Atom}
 */
ydn.atom.Feed = function(data) {
  goog.base(this, data);
};
goog.inherits(ydn.atom.Entry, ydn.atom.Atom);



/**
 * @const
 * @type {string}
 */
ydn.atom.Entry.TYPE = 'ydn.atom.Feed';