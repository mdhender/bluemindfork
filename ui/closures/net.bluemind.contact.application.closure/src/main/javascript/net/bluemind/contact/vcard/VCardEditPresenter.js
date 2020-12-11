/* BEGIN LICENSE 
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
goog.provide("net.bluemind.contact.vcard.VCardEditPresenter");

goog.require("goog.Promise");
goog.require("goog.array");
goog.require("net.bluemind.contact.vcard.VCardConsultPresenter");
goog.require("net.bluemind.mvp.UID");
goog.require("net.bluemind.contact.vcard.VCardModelAdapter");
goog.require("net.bluemind.events.MailToWebmailHandler.RCubeHelper");

/**
 * @constructor
 * @param {net.bluemind.mvp.ApplicationContext} ctx
 * @param {goog.ui.Component} view
 * @extends {net.bluemind.contact.vcard.VCardConsultPresenter}
 */
net.bluemind.contact.vcard.VCardEditPresenter = function(ctx, view) {
  net.bluemind.contact.vcard.VCardConsultPresenter.call(this, ctx, view);
  this.handler.listen(this.view, 'save', this.handleSave);
  this.handler.listen(this.view, 'delete', this.handleDelete);
  this.handler.listen(this.view, 'move', this.handleMove);
  this.handler.listen(this.view, 'mailto', this.handleMailto);
  this.handler.listen(this.view, 'tel', this.handleCallto);
  this.handler.listen(this.view, 'copy', this.handleCopy);
  this.handler.listen(this.view, 'create-tag', this.handleCreateTag);
  this.handler.listen(this.view, 'history', this.handleLoadHistory);
  this.logger = goog.log.getLogger('net.bluemind.contact.vcard.VCardEditPresenter');
  this.ctx_ = ctx;
}
goog.inherits(net.bluemind.contact.vcard.VCardEditPresenter, net.bluemind.contact.vcard.VCardConsultPresenter);

/** @override */
net.bluemind.contact.vcard.VCardEditPresenter.prototype.setup = function() {
  var container = this.ctx.params.get('container');
  var promise, uid = this.ctx.params.get('uid');
  var addressbook = goog.array.find(this.ctx.session.get('addressbooks'), function(adb) {
    return (adb['uid'] == container);
  });
  if (uid) {
    promise = this.ctx.service('addressbook').getItem(container, uid);
  } else {
    promise = goog.Promise.resolve(this.getEmptyVCard(container, addressbook));
  }
  var vcard;
  return promise.then(function(item) {
    vcard = item;
    return this.ctx.service('addressbook').getLocalChangeSet(container);
  }, null, this).then(function(changes) {
    if (goog.isDefAndNotNull(vcard)) {
      this.view.setModel(this.toModelView(vcard, addressbook, changes));
      var addressbooks = goog.array.clone(this.ctx.session.get('addressbooks'));
      this.view.setFolders(addressbooks);
    } else {
      throw 'VCard ' + uid + ' not found';
    }
  }, null, this).then(function() {
    return this.ctx.service('tags').getTags();
  }, null, this).then(function(tags) {
    var vtags = goog.array.map(tags, function(tag) {
      return {
        id : tag['itemUid'],
        container : tag['containerUid'],
        label : tag['label'],
        color : tag['color']
      };
    });
    this.view.setTags(vtags);
  }, null, this).thenCatch(function(error) {
    goog.log.error(this.logger, 'error during loading', error);
    this.ctx.notifyError(net.bluemind.contact.Messages.errorLoading(), error);
  }, this);
};

/**
 * @param {Object} vcard VCard object
 * @param {Object} addressbook Container object
 * @param {Array} changes Changes list
 * @override
 */
net.bluemind.contact.vcard.VCardEditPresenter.prototype.toModelView = function(vcard, addressbook, changes) {
  var mv = goog.base(this, 'toModelView', vcard, addressbook);
  var change = goog.array.find(changes, function(change) {
    return change['itemId'] == vcard['uid'];
  });
  mv.states = {};
  mv.states.synced = !goog.isDefAndNotNull(change);
  mv.states.error = !mv.states.synced && change['type'] == 'error';
  mv.error = mv.states.error && {
    code : change['errorCode'],
    message : change['errorMessage']
  };
  return mv;
};

/**
 * Create an empty vcard
 * 
 * @protected
 * @return {Object}
 */
net.bluemind.contact.vcard.VCardEditPresenter.prototype.getEmptyVCard = function(container) {
  return {
    'container' : container,
    'uid' : null,
    'value' : {
      'identification' : {},
      'communications' : {},
      'organizational' : {},
      'related' : {},
      'explanatory' : {},
      'deliveryAddressing' : {},
      'security' : {}
    }
  }
};

/**
 * Convert from model view to storage model
 * 
 * @protected
 * @param {Object} vcard VCard object
 * @return {Object}
 */
net.bluemind.contact.vcard.VCardEditPresenter.prototype.fromModelView = function(mv) {
  return new net.bluemind.contact.vcard.VCardModelAdapter(this.ctx_).fromModelView(mv);
};

/**
 * Save contact
 * 
 * @protected
 * @param {goog.event.Event} event
 */
net.bluemind.contact.vcard.VCardEditPresenter.prototype.handleSave = function(event) {
  var model = this.view.getModel();
  var errors = this.validateModelView_(model);
  if (errors.length > 0) {
    model.errors = errors;
    this.view.setModel(model);
    return;
  }

  var vcard = this.fromModelView(model);

  if (model.uploadPhoto) {
    vcard['value']['identification']['photo'] = true;
  }

  if (model.deletePhoto) {
    vcard['value']['identification']['photo'] = false;
  }

  var toCreate = goog.array.filter(vcard['value']['explanatory']['categories'], function(c) {
    return c['itemUid'] == null;
  });


  var alreadyExists = !!vcard['uid'] && vcard['uid'] != 'undefinied';
  return this.ctx.service('tags').createTags(toCreate).then(function() {
    if (model.deletePhoto) {
      return this.ctx.service('addressbook').deletePhoto(vcard['container'], vcard['uid']);
    }
  }, null, this).then(function() {
    if (vcard['value']['kind'] == 'individual') {
      vcard['value']['identification']['formatedName'] = null;
    }

    if (alreadyExists) {
      return this.ctx.service('addressbook').update(vcard);
    } else {
      vcard['uid'] = net.bluemind.mvp.UID.generate();
      return this.ctx.service('addressbook').create(vcard);
    }
  }, null, this).then(function(item) {
    if (model.uploadPhoto) {
      return this.ctx.service('addressbook').setPhoto(vcard['container'], vcard['uid'], model.uploadPhoto);
    }
  }, null, this).then(function() {
    if (alreadyExists) {
      this.ctx.notifyInfo(net.bluemind.contact.Messages.successUpdate());
    } else {
      this.ctx.notifyInfo(net.bluemind.contact.Messages.successCreate());
    }
    // FIXME: location
    this.ctx.helper('url').goTo('/vcard/edit?uid=' + vcard['uid'] + '&container=' + vcard['container'], 'vcontainer');
  }, function(error) {

    if (alreadyExists) {
      goog.log.error(this.logger, 'error during card ' + vcard['uid'] + ' update', error);
      this.ctx.notifyError(net.bluemind.contact.Messages.errorUpdate(error), error);
    } else {
      goog.log.error(this.logger, 'error during card creation', error);
      this.ctx.notifyError(net.bluemind.contact.Messages.errorCreate(error), error);
    }
  }, this);
};

/**
 * Move contact
 * 
 * @protected
 * @param {goog.event.Event} event
 */
net.bluemind.contact.vcard.VCardEditPresenter.prototype.handleMove = function(event) {
  var model = this.view.getModel();
  this.ctx.service('addressbook').moveItem(model.container.id, model.id, event.target.getId()).then(function(m) {
    this.ctx.notifyInfo(net.bluemind.contact.Messages.successMove());
    this.ctx.helper('url').goTo('/vcard/?container=' + event.target.getId() + '&uid=' + model.id, 'vcontainer');

  }, function(error) {
    goog.log.error(this.logger, 'error during card ' + model.id + ' move', error);
    this.ctx.notifyError(net.bluemind.contact.Messages.errorMove(error), error);
  }, this);
};

/**
 * mailto contact
 * 
 * @protected
 * @param {goog.event.Event} event
 */
net.bluemind.contact.vcard.VCardEditPresenter.prototype.handleMailto = function(event) {
  var model = this.view.getModel();
  var email = model.emails && (model.emails.length > 0) && model.emails[0];
  if (goog.array.contains(this.ctx.user['roles'], 'hasMail')) {
    net.bluemind.events.MailToWebmailHandler.RCubeHelper.mailTo(email.value);
  } else {
    window.location.href = 'mailto:' + email.value;
  }
};

/**
 * tel contact
 * 
 * @protected
 * @param {goog.event.Event} event
 */
net.bluemind.contact.vcard.VCardEditPresenter.prototype.handleCallto = function(event) {
  var model = this.view.getModel();
  var tel = model.tels && (model.tels.length > 0) && goog.array.find(model.tels, function(tel) {
    return goog.string.contains(tel.label, 'voice');
  });
  window.location.href = 'tel:' + tel.value;
};

/**
 * Delete contact
 * 
 * @protected
 * @param {goog.event.Event} event
 */
net.bluemind.contact.vcard.VCardEditPresenter.prototype.handleDelete = function(event) {
  var model = this.view.getModel();
  if (model.id && model.container) {
    this.ctx.service('addressbook').deleteItem(model.container.id, model.id).then(function() {
      this.ctx.notifyInfo(net.bluemind.contact.Messages.successDelete());
      // FIXME: location
      this.ctx.helper('url').goTo('/', [ 'container', 'vcontainer' ]);
    }, function(error) {
      goog.log.error(this.logger, 'error during card ' + model.id + ' delete', error);
      this.ctx.notifyError(net.bluemind.contact.Messages.errorDelete(error), error);
    }, this);
  } else {
    this.ctx.helper('url').goTo('/', [ 'container', 'vcontainer' ]);
  }
};

/**
 * Create tag
 * 
 * @protected
 * @param {goog.event.Event} event
 */
net.bluemind.contact.vcard.VCardEditPresenter.prototype.handleCreateTag = function(event) {
  var tag = event.tag;
  var m = {
    'itemUid' : tag.id,
    'label' : tag.label,
    'color' : tag.color
  };
  this.ctx.service('tags').createTag(m).then(function() {
    // FIXME ugly hack
    tag.container = m['containerUid'];
  });
};

/**
 * Load item history
 * 
 * @protected
 * @param {goog.event.Event} event
 */
net.bluemind.contact.vcard.VCardEditPresenter.prototype.handleLoadHistory = function(event) {
  var model = this.view.getModel();
  var that = this;
  var history = this.ctx.service('addressbook').getItemHistory(model.container.id, model.id).then(function(history) {
    that.view.showHistory(history['entries']);
  });
}

net.bluemind.contact.vcard.VCardEditPresenter.prototype.validateModelView_ = function(mv) {
  var ret = [];
  // validate email
  var inError = false;
  if (mv.emails) {
    var regexp = "^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@([A-Za-z0-9-]+\\.)+[A-Za-z]{2,}$";
    goog.array.forEach(mv.emails, function(email) {
      if (email && email.value != null && email.value.length > 0 && email.value.match(regexp) == null) {
        /** @meaning contact.vcard.email.invalid */
        var MSG_EMAIL_INVALID = goog.getMsg('Email is not valid');
        ret.push({
          property : 'email.value',
          msg : MSG_EMAIL_INVALID
        });
      }

    });

    if (!mv.name && (!mv.fullname || !mv.fullname.value)) {
      if (!mv.emails || mv.emails.length == 0) {
        if (!mv.company) {
          /** @meaning contact.vcard.name.isEmpty */
          var MSG_NAME_IS_EMPTY = goog.getMsg('Name is empty');
          ret.push({
            property : 'name',
            msg : MSG_NAME_IS_EMPTY
          });
        }
      }
    }
    return ret;
  }
}
