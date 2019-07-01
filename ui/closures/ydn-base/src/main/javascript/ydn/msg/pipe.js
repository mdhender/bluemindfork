/**
 * @fileoverview Channel provider.
 *
 * Extension pages and content script pages communicate by using main pipe via
 * background page. Each page has only one main pipe. Message are send through
 * a channel in the pipe. A channel has group and name.
 *
 * To received message from background page, use listen method.
 *                                                 `
 * @author kyawtun@yathit.com (Kyaw Tun)
 */


goog.provide('ydn.msg.Pipe');
goog.require('goog.events.EventTarget');
goog.require('ydn.msg.Channel');
goog.require('ydn.msg.Event');
goog.require('ydn.msg.Message');
goog.require('ydn.crm.ch.Req');



/**
 * Channel provider.
 * <pre>
 *   ydn.msg.initPipe('dev');
 *   // send message
 *   ydn.msg.getMain().send('req').addCallback(...);
 *   // listen broadcast message of type ydn.msg.Event
 *   goog.events.listen(ydn.msg.getMain(), ydn.crm.ch.BReq.HOST_PERMISSION, ...);
 * </pre>
 * @param {string|exYdn.PipeInfo} name_or_info channel name or channel info.
 * @constructor
 * @struct
 * @extends {goog.events.EventTarget}
 */
ydn.msg.Pipe = function(name_or_info) {
  goog.base(this);
  var name = goog.isString(name_or_info) ? name_or_info : name_or_info.name;
  var info = goog.isString(name_or_info) ? null : name_or_info;
  /**
   * @final
   * @protected
   * @type {string}
   */
  this.name = name;
  /**
   * @final
   * @type {Array.<ydn.msg.Message>}
   * @private
   */
  this.messages_ = [];
  /**
   * Information about this channel.
   * @type {exYdn.PipeInfo}
   * @private
   */
  this.info_ = info;
  /**
   * Connection to event page.
   * @private
   * @type {Port}
   */
  this.port_ = null;
  /**
   *
   * @type {Object.<!ydn.msg.Channel>}
   * @private
   */
  this.sub_channels_ = {};

};
goog.inherits(ydn.msg.Pipe, goog.events.EventTarget);


/**
 * @inheritDoc
 */
ydn.msg.Pipe.prototype.disposeInternal = function() {
  if (this.port_) {
    var me = this;
    this.port_['disconnect'](function() {
      me.port_ = null;
      me.messages_.length = 0;
    });
  } else {
    this.messages_.length = 0;
  }
};


/**
 * @define {number} maximun number of messanger to track.
 */
ydn.msg.Pipe.MAX_MESSANGERS = 50;


/**
 * @define {boolean} debug flag.
 */
ydn.msg.Pipe.DEBUG = false;


/**
 * Request from background thread.
 * @enum {string}
 */
ydn.msg.RReq = {
  INFO: 'info',
  HTML_BODY_INNER: 'html-body-inner' // request html body content of current document.
};


/**
 * Message listener.
 * @param {exYdn.PostMessage} msg
 * @protected
 */
ydn.msg.Pipe.prototype.defaultListener = function(msg) {
  if (!msg) {
    return;
  }
  if (!msg.id) {
    // Respond message always has 'id', but request message from the background page
    // does not have id.
    if (ydn.msg.Pipe.DEBUG) {
      goog.global.console.log('receiving broadcast ' + JSON.stringify(msg));
    }
    if (msg.req == ydn.msg.RReq.INFO) {
      msg.data = this.info_;
      this.port_.postMessage(msg);
    } else if (msg.req == ydn.msg.RReq.HTML_BODY_INNER) {
      goog.global.console.log('html', msg);
      msg.req = ''; // clear request
      msg.done = true;
      msg.data = (!!document && !!document.body) ? document.body.innerHTML : '';
      this.port_.postMessage(msg);
    } else {
      this.dispatchEvent(new ydn.msg.Event(msg));
    }
    return;
  }
  for (var i = this.messages_.length - 1; i >= 0; i--) {
    if (ydn.msg.Pipe.DEBUG) {
      goog.global.console.log('handing ' + i + ' of ' + this.messages_.length);
    }
    if (this.messages_[i].getId() == msg.id) {
      if (ydn.msg.Pipe.DEBUG) {
        goog.global.console.log(this + ' receiving msg ' + msg['req'] + ':' + msg['id'], msg);
      }
      var handled = this.messages_[i].listen(msg);
      if (handled) {
        if (this.messages_[i].hasDone()) {
          this.messages_.splice(i, 1);
        } else if (i > ydn.msg.Pipe.MAX_MESSANGERS / 2) {
          // move last listening to the top of the queue, to that it has
          // longer life.
          var msgs = this.messages_.splice(i, 1);
          this.messages_.unshift(msgs[0]);
        }
        return;
      }
      break;
    }
  }
  // this.logger.finest('unhandled message ' + ydn.json.toShortString(msg));
};


/**
 * @protected
 * @param {Object} msg
 */
ydn.msg.Pipe.prototype.postMessage = function(msg) {
  this.getPort().postMessage(msg);
};


/**
 * Send message to background service.
 * @param {ydn.msg.Message} msgr
 * @final
 */
ydn.msg.Pipe.prototype.sendMsg = function(msgr) {

  var msg = msgr.toJSON();
  if (ydn.msg.Pipe.DEBUG) {
    goog.global.console.log(this + ' sending msg ' + msg['req'] + ':' + msg['id']);
  }

  if (goog.DEBUG && ydn.msg.Pipe.dry_run_) {
    var block = [ydn.crm.ch.Req.DNT_ADD, ydn.crm.ch.Req.DNT_REMOVE, ydn.crm.ch.Req.EXPORT_RECORD,
      ydn.crm.ch.Req.FEEDBACK, ydn.crm.ch.Req.GAPPS_NEW_CAL, ydn.crm.ch.Req.GDATA_UPDATE,
      ydn.crm.ch.Req.NEW_ENTRY, ydn.crm.ch.Req.REMOVE_SUGAR, ydn.crm.ch.Req.SYNC,
      ydn.crm.ch.Req.SYNC_FOR, ydn.crm.ch.SReq.NEW_RECORD, ydn.crm.ch.SReq.UPLOAD,
      ydn.crm.ch.SReq.DELETE_RECORD, ydn.crm.ch.SReq.IMPORT_GDATA, ydn.crm.ch.SReq.LINK,
      ydn.crm.ch.SReq.SET_REL, ydn.crm.ch.SReq.PUT_RECORD
    ];
    if (block.indexOf(msgr.getReq()) >= 0) {
      goog.global.console.log('dryRun: ' + msgr.getReq(), msgr);
      return;
    }
  }

  this.postMessage(msg);

  this.messages_.push(msgr);
  if (this.messages_.length > ydn.msg.Pipe.MAX_MESSANGERS) {
    this.messages_.length = this.messages_.length - 1;
  }

};


/**
 * @return {string}
 */
ydn.msg.Pipe.prototype.getChannelName = function() {
  return this.name;
};


/**
 * Get or create sub channel.
 * @param {ydn.msg.Group=} opt_group
 * @param {string=} opt_name
 * @return {!ydn.msg.Channel}
 */
ydn.msg.Pipe.prototype.getChannel = function(opt_group, opt_name) {
  var group = opt_group || ydn.msg.Group.MAIN;
  var ref = group + (opt_name ? '-' + opt_name : '');
  if (!this.sub_channels_[ref]) {
    this.sub_channels_[ref] = new ydn.msg.Channel(this, group, opt_name || '');
  }
  return this.sub_channels_[ref];
};


/**
 * Find a sub channel.
 * @param {ydn.msg.Group} group
 * @param {string=} opt_name
 * @return {ydn.msg.Channel}
 */
ydn.msg.Pipe.prototype.findChannel = function(group, opt_name) {
  for (var name in this.sub_channels_) {
    var parts = name.split('-');
    if (group == parts[0]) {
      if (!opt_name || opt_name == parts[1]) {
        return this.sub_channels_[name];
      }
    }
  }
  return null;
};


/**
 * @protected
 * @return {Port}
 */
ydn.msg.Pipe.prototype.getPort = function() {
  if (!this.port_) {
    var me = this;
    this.port_ = chrome.runtime.connect({'name': this.name});
    var listener = goog.bind(this.defaultListener, this);
    this.port_.onMessage.addListener(listener);
    this.port_.onDisconnect.addListener(function() {
      if (ydn.msg.Pipe.DEBUG) {
        goog.global.console.log('disconnected port ' + me.port_.name + ' ' + me.port_['url']);
      }
      me.port_.onMessage.removeListener(listener);
      me.port_ = null;
      me.messages_.length = 0;
    });
  }
  return this.port_;
};


if (goog.DEBUG) {
  /**
   * @inheritDoc
   */
  ydn.msg.Pipe.prototype.toString = function() {
    return 'Pipe:' + this.name;
  };
}


/**
 * @type {boolean}
 * @private
 */
ydn.msg.Pipe.dry_run_ = false;


/**
 * On development environment, Filter out modification request.
 */
ydn.msg.Pipe.dryRun = function() {
  ydn.msg.Pipe.dry_run_ = true;
};


/**
 * @enum {string}
 */
ydn.msg.ChannelName = {
  DEV: 'dev', // development channel for testing
  CONTENT_SCRIPT: 'content-script',
  WEB_APP: 'web-app',
  HOST_PERMISSION: 'hostper',
  POPUP: 'popup',
  OPTIONS: 'options',
  SETUP: 'setup'
};


/**
 * @enum {string}
 */
ydn.msg.Group = {
  MAIN: 'default',
  DEV: ydn.msg.ChannelName.DEV,
  CONTENT_SCRIPT: ydn.msg.ChannelName.CONTENT_SCRIPT,
  HOST_PERMISSION: ydn.msg.ChannelName.HOST_PERMISSION,
  POPUP: ydn.msg.ChannelName.POPUP,
  OPTIONS: ydn.msg.ChannelName.OPTIONS,
  SUGAR: 'sugarcrm'
};


/**
 * List of supported group.
 * @type {Array.<ydn.msg.Group>}
 */
ydn.msg.Channels = [ydn.msg.ChannelName.DEV, ydn.msg.ChannelName.CONTENT_SCRIPT,
  ydn.msg.ChannelName.HOST_PERMISSION,
  ydn.msg.ChannelName.POPUP, ydn.msg.ChannelName.OPTIONS, ydn.msg.ChannelName.SETUP];


/**
 * @param {ydn.msg.Group=} opt_group
 * @param {string=} opt_name
 * @return {!ydn.msg.Channel}
 */
ydn.msg.getChannel = function(opt_group, opt_name) {
  return ydn.msg.getMain().getChannel(opt_group, opt_name);
};





