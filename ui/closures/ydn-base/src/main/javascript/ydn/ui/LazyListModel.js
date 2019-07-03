/**
 * @fileoverview Lazy list data model.
 */


goog.provide('ydn.ui.LazyListModel');


/**
 * Lazy list data model.
 * @param {number=} opt_count number of items.
 * @constructor
 * @struct
 * @extends {goog.events.EventTarget}
 */
ydn.ui.LazyListModel = function(opt_count) {
  ydn.ui.LazyListModel.base(this, 'constructor');
  /**
   * @protected
   * @type {number}
   */
  this.count = opt_count || 0;

  var me = this;
  var flash = function() {
    setTimeout(function() {
      me.count = (me.count * (Math.random() + 0.6)) | 0;
      me.dispatchEvent(new goog.events.Event(goog.events.EventType.CHANGE));
      flash();
    }, Math.random() * 1000 * 15);
  };
  flash();
};
goog.inherits(ydn.ui.LazyListModel, goog.events.EventTarget);


/**
 * @return {number}
 */
ydn.ui.LazyListModel.prototype.getCount = function() {
  return this.count;
};


/**
 * @param {number} at
 * @return {Object}
 */
ydn.ui.LazyListModel.prototype.getItemAt = function(at) {
  return {'id': at, 'name': 'Item ' + at};
};
