/**
 * @fileoverview About this file
 */


goog.provide('ydn.msg.Channel');



/**
 * Sub channel
 * @param {ydn.msg.Pipe} pipe
 * @param {string} group
 * @param {string} name
 * @constructor
 * @struct
 */
ydn.msg.Channel = function(pipe, group, name) {
  this.main_ = pipe;
  this.group_ = group;
  this.name_ = name;
  /**
   * @final
   * @type {string}
   */
  this.id = this.group_ + (this.name_ ? '-' + this.name_ : '');
};


/**
 * Get channel name.
 * @return {string}
 */
ydn.msg.Channel.prototype.getName = function() {
  return this.name_;
};


/**
 * Send data.
 * @param {string} req request
 * @param {*=} opt_data
 * @return {!ydn.async.Deferred}
 */
ydn.msg.Channel.prototype.send = function(req, opt_data) {
  var msg = new ydn.msg.Message(req);
  msg.setRef(this.group_, this.name_);
  msg.setData(opt_data);
  var df = msg.promise();
  this.main_.sendMsg(msg);
  return df;
};



