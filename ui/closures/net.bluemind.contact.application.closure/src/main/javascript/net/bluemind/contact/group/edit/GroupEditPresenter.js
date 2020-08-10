/*
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
goog.provide("net.bluemind.contact.group.edit.GroupEditPresenter");
goog.provide("net.bluemind.contact.group.edit.GroupEditPresenter.MemberMatcher");

goog.require("goog.array");
goog.require("goog.string");
goog.require("net.bluemind.contact.group.edit.GroupEditView");
goog.require("net.bluemind.contact.group.edit.ui.MemberField.EventType");
goog.require("net.bluemind.contact.group.ui.MemberDetails.EventType");
goog.require("net.bluemind.contact.vcard.VCardEditPresenter");

/**
 * @constructor
 * @param {net.bluemind.mvp.ApplicationContext} ctx
 * @extends {net.bluemind.contact.vcard.VCardEditPresenter}
 */
net.bluemind.contact.group.edit.GroupEditPresenter = function(ctx) {
  var view = new net.bluemind.contact.group.edit.GroupEditView(ctx);

  net.bluemind.contact.vcard.VCardEditPresenter.call(this, ctx, view);
  var matcher = new net.bluemind.contact.group.edit.GroupEditPresenter.MemberMatcher(ctx, view.getChild('form')
      .getChild('members'));
  view.getChild('form').getChild('members').setMatcher(matcher);
  this.handler.listen(view, net.bluemind.contact.group.edit.ui.MemberField.EventType.CREATE, this.handleCreateMember_);
  this.handler.listen(view, net.bluemind.contact.group.edit.ui.MemberField.EventType.AC_ADD,
      this.handleAutoCompleteEntryAdded_);
  this.handler.listen(view, net.bluemind.contact.group.ui.MemberDetails.EventType.GOTO, this.handleGoToMember_);
  this.handler.listen(view, 'validate', this.handleValidateContact_);
}
goog.inherits(net.bluemind.contact.group.edit.GroupEditPresenter, net.bluemind.contact.vcard.VCardEditPresenter);

/** @override */
net.bluemind.contact.group.edit.GroupEditPresenter.prototype.fromModelView = function(mv) {
  var vcard = goog.base(this, 'fromModelView', mv)
  vcard['name'] = mv.name;
  vcard['value']['kind'] = 'group';

  return vcard;
};

/**
 * Convert a member to vcard
 * 
 * @private
 * @param {Object} vcard VCard object
 * @return {Object}
 */
net.bluemind.contact.group.edit.GroupEditPresenter.prototype.fromMemberModelView_ = function(mv) {
  var fullname = mv.name.split(' ');
  mv.lastnames = fullname.pop() || '';
  mv.firstnames = fullname.join(' ');
  mv.emails = mv.email ? [ mv.email ] : null;
  var vcard = {
    'container' : mv.container,
    'uid' : mv.id,
    'name' : mv.name,
    'value' : {
      'kind' : 'individual',
      'identification' : {
        'name' : {
          'givenNames' : mv.firstnames,
          'familyNames' : mv.lastnames
        }
      }
    }
  };
  if (mv.email) {
    vcard['value']['communications'] = {
      'emails' : [ {
        'parameters' : [ {
          'label' : 'TYPE',
          'value' : 'work'
        } ],
        'value' : mv.email
      } ]
    };
  }
  return vcard;

};

/**
 * Create a contact
 * 
 * @param {goog.event.Event} event
 */
net.bluemind.contact.group.edit.GroupEditPresenter.prototype.handleCreateMember_ = function(event) {
  var model = event.model;
  var vcard = this.fromMemberModelView_(model);
  this.ctx.service('addressbook').create(vcard).then(function(item) {
    this.ctx.notifyInfo(net.bluemind.contact.Messages.successCreate());
  }, function(error) {
    this.ctx.notifyError(net.bluemind.contact.Messages.errorCreate(error), error);
  }, this);
};

/**
 * Go to a contact
 * 
 * @param {goog.event.Event} event
 */
net.bluemind.contact.group.edit.GroupEditPresenter.prototype.handleGoToMember_ = function(event) {
  var model = event.model;

  var cont = model.container;
  if (!model.container) {
    cont = this.view.getModel().container.id;
  }

  this.ctx.service('addressbook').getItem(cont, model.id).then(function(card) {
    if (card) {
      this.ctx.helper('url').goTo('/vcard/?uid=' + model.id + '&container=' + cont);
    } else {
      this.ctx.notifyError(net.bluemind.contact.Messages.errorNotAccessible());
    }
  }, null, this);
};

/**
 * @constructor
 */
net.bluemind.contact.group.edit.GroupEditPresenter.MemberMatcher = function(ctx, field) {
  this.ctx = ctx;
  this.field = field;
};

/**
 * @type {net.bluemind.mvp.AppicationContext}
 */
net.bluemind.contact.group.edit.GroupEditPresenter.MemberMatcher.prototype.ctx;

/**
 * @type {net.bluemind.contact.group.edit.ui.MemberField}
 */
net.bluemind.contact.group.edit.GroupEditPresenter.MemberMatcher.prototype.view;

/**
 * Validate contact dlist members
 * 
 * @protected
 * @param {goog.event.Event} event
 */
net.bluemind.contact.group.edit.GroupEditPresenter.prototype.handleValidateContact_ = function(event) {
  var that = this;
  var model = this.view.getModel();
  var promises = [];
  for ( var index in model.members) {
    var member = model.members[index];
      promises.push({
        "uid" : member.id,
        "promise" : this.ctx.service('addressbook').getItem(member.container ? member.container : model.container.id,
            member.id)
      });
  }
  
  goog.Promise.all(promises.map(function (p) {return p.promise})).then(function(ret) {
    for (var index in promises) {
      
      var resolved = promises[index]; 
      var resolvedValue = ret[index];
      
      var i = model.members.length;
      while (i--) {
        var member = model.members[i];
        if (member.id == resolved['uid']) {
          if (!resolvedValue || !resolvedValue['value'] || resolvedValue['value']['kind'] == 'group') {
            model.members.splice(i, 1);
          } else {
            member.name = resolvedValue['displayName'];
            var communications = resolvedValue['value']['communications'];
            if (communications && communications['emails'] && communications['emails'].length > 0) {
              member.email = communications['emails'][0]['value'];
            } else {
              member.email = '';
            }
          }
          break;
        }
      }
    }
  }).then(function(e) {
    that.view.setModel(model);
  });
};

net.bluemind.contact.group.edit.GroupEditPresenter.prototype.handleAutoCompleteEntryAdded_ = function(event) {
  var model = event.model;
  if (model.container != this.ctx.params.get('container')) {
    this.ctx.notifyInfo(net.bluemind.contact.Messages.infoExternalContact());
  }
}

/**
 * Matcher
 * 
 * @param {*} token
 * @param {*} max
 * @param {*} handler
 * @param {*} opt_fullString
 */
net.bluemind.contact.group.edit.GroupEditPresenter.MemberMatcher.prototype.requestMatchingRows = function(token, max,
    handler, opt_fullString) {
  if (goog.string.trim(token) != '') {
    this.ctx.service('addressbooks').search(token, 0, max, 'Pertinance').then(function(results) {
      var values = this.field.getValue();
      values = goog.array.map(values, function(value) {
        return value.id;
      });

      var thisGroup = {
        container : this.ctx.params.get('container'),
        uid : this.ctx.params.get('uid')
      };

      results = goog.array.filter(results, function(match) {
        if (match['uid'] == thisGroup.uid && match['container'] == thisGroup.container) {
          return false;
        }
        if (match['container'] != thisGroup.container) {
          if (match['value']['kind'] == 'group') {
            // FEATBL-716: filter dlists from other addressbooks
            return false;
          }
        }
        return !goog.array.contains(values, match['uid']);
      }, this);

      var members = goog.array.map(results || [], function(match) {
        var addressbook = goog.array.find(this.ctx.session.get('addressbooks'), function(adb) {
          return (adb['uid'] == match['container']);
        });

        var member = {
          name : match['name'],
          id : match['uid'],
          container : match['container'],
          containerName : addressbook['name']
        };

        var communications = match['value']['communications'];
        if (communications && communications['emails'] && communications['emails'].length > 0) {
          member.email = communications['emails'][0]['value'];
        }

        if (match['value']['identification']['photo']) {
          member.photo = '/api/addressbooks/' + match['container'] + '/' + match['id'] + '/icon';
        } else {
          if (match['value']['kind'] == 'group') {
            member.photo = 'images/no-dlist-picture_small.png';
          } else {
            member.photo = 'images/nopicture_small.png';
          }

        }
        return member;
      }, this);

      members.push({
        name : token,
        container : this.ctx.params.get('container')
      });
      handler(token, members);
    }, null, this).thenCatch(function(e) {
      this.ctx.notifyError(net.bluemind.contact.Messages.errorLoading(e), e);
    }, this);
  }
};
