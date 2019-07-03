/**
 * @fileoverview Bidirectional message.
 *                                                 `
 * @author kyawtun@yathit.com (Kyaw Tun)
 */


goog.provide('ydn.msg.Message');
goog.require('ydn.async.Deferred');
goog.require('goog.json');



/**
 * Send request to the change and stream result.
 * @param {string} req
 * @constructor
 * @struct
 */
ydn.msg.Message = function(req) {
  /**
   * @protected
   * @type {string}
   */
  this.id = 'i' + this.count();
  /**
   * @protected
   * @type {string|undefined}
   */
  this.name_;
  /**
   * @protected
   * @type {string|undefined}
   */
  this.group_;
  /**
   * @protected
   * @type {string}
   */
  this.req = req;
  /**
   * True if not done.
   * @type {boolean}
   * @private
   */
  this.alive_ = true;
  /**
   * @type {*}
   * @private
   */
  this.data_ = null;
  /**
   * Error message.
   * @type {Error}
   * @private
   */
  this.error_ = null;
  /**
   * Promise obj.
   * @final
   * @type {!ydn.async.Deferred}
   * @private
   */
  this.df_ = new ydn.async.Deferred();
  /**
   * Time out id.
   * @type {number} timeout id.
   * @private
   */
  this.tid_ = NaN;
};


/**
 * @define {boolean} debug flag.
 */
ydn.msg.Message.DEBUG = false;


/**
 * @return {string}
 */
ydn.msg.Message.prototype.getId = function() {
  return this.id;
};


/**
 * @return {string}
 */
ydn.msg.Message.prototype.getReq = function() {
  return this.req;
};


/**
 * @param {string} group
 * @param {string} ref
 */
ydn.msg.Message.prototype.setRef = function(group, ref) {
  this.group_ = group;
  this.name_ = ref;
};


/**
 * @private
 * @type {number}
 */
ydn.msg.Message.counter_ = 0;


/**
 * Promise time out.
 * This value should be sufficiently large for all requests.
 * Note: Twitter user profile API take around 15 sec.
 * Note: Sync process could take very long.
 * @type {number}
 */
ydn.msg.Message.TIMEOUT = ydn.time.MINUTE * 5;


/**
 * Get result as promise.
 * @return {!ydn.async.Deferred}
 */
ydn.msg.Message.prototype.promise = function() {
  return this.df_;
};


/**
 * @protected
 * @return {number}
 */
ydn.msg.Message.prototype.count = function() {
  ydn.msg.Message.counter_++;
  return ydn.msg.Message.counter_;
};


/**
 * Check for error.
 * @return {boolean}
 */
ydn.msg.Message.prototype.isError = function() {
  return !!this.error_;
};


/**
 * Listen message.
 * @param {*} msg
 * @return {boolean} true if listened, false if not relevant.
 */
ydn.msg.Message.prototype.listen = function(msg) {
  if (msg && msg['id'] == this.id) {
    this.data_ = msg['data'];
    var error = msg['error'];
    if (error) {
      if (error instanceof Error) {
        this.error_ = error;
      } else if (goog.isObject(error)) {
        this.error_ = new Error(error['message']);
        if (error['name']) {
          this.error_.name = error['name'];
        }
        this.error_['source'] = error;
        this.error_['result'] = this.data_;
      } else if (goog.isString(error)) {
        if (goog.json.isValid(error)) {
          try {
            var err_obj = JSON.parse(error);
            this.error_ = new Error(err_obj['message']);
            for (var k in err_obj) {
              this.error_[k] = err_obj[k];
            }
          } catch (e) {
            this.error_ = new Error(error);
          }
        } else {
          this.error_ = new Error(error);
        }
      } else {
        this.error_ = new Error(error);
      }
    }
    if (msg['done']) {
      this.alive_ = false;
      if (this.error_) {
        this.df_.errback(this.error_);
      } else {
        this.df_.callback(this.data_);
      }
      window.clearTimeout(this.tid_);
    } else {
      this.df_.notify(msg['data']);
    }

    return true;
  } else {
    return false;
  }
};


/**
 * @inheritDoc
 */
ydn.msg.Message.prototype.toString = function() {
  return 'Message:' + this.id + (this.alive_ ? '*' : '');
};


/**
 * Finish messaging.
 * @return {boolean}
 */
ydn.msg.Message.prototype.hasDone = function() {
  return !this.alive_;
};


/**
 * @param {*} data
 */
ydn.msg.Message.prototype.setData = function(data) {
  this.data_ = data;
};


/**
 * @return {*} data message data.
 */
ydn.msg.Message.prototype.getData = function() {
  return this.data_;
};


/**
 * @return {Error} data message data.
 */
ydn.msg.Message.prototype.getError = function() {
  return this.error_;
};


/**
 * @return {Object}
 */
ydn.msg.Message.prototype.toJSON = function() {
  // here we are assuming message is immediately send to the channel after this
  // call.
  var me = this;
  this.tid_ = window.setTimeout(function() {
    if (me.alive_) {
      var err = new Error('message timeout');
      err.name = 'TimeOutError';
      var data = {
        'id': me.id,
        'req': me.req,
        'error': err
      };
      me.listen(data);
    }
  }, ydn.msg.Message.TIMEOUT);
  return {
    'id': this.id,
    'req': this.req,
    'group': this.group_,
    'name': this.name_,
    'data': this.data_
  };
};


