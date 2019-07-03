/**
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

/**
 * @fileoverview Manage folder storage.
 */

goog.provide("net.bluemind.tag.service.TagService");

goog.require("goog.Promise");
goog.require("goog.array");
goog.require("goog.events.EventTarget");
goog.require("goog.events.EventHandler");
goog.require("net.bluemind.container.service.ContainerService");
goog.require("net.bluemind.container.service.ContainerService.EventType");
goog.require("net.bluemind.container.service.ContainersService");
goog.require("net.bluemind.core.container.api.ContainersClient");
goog.require("net.bluemind.mvp.UID");
goog.require("net.bluemind.mvp.helper.ServiceHelper");
goog.require("net.bluemind.tag.api.TagsClient");
/**
 * @constructor
 * @param {net.bluemind.mvp.ApplicationContext} ctx Context
 * @extends {goog.events.EventTarget}
 */
net.bluemind.tag.service.TagService = function(ctx) {
  goog.base(this);
  this.ctx = ctx;
  this.cs_ = new net.bluemind.container.service.ContainerService(ctx, 'tag');
  this.css_ = new net.bluemind.container.service.ContainersService(ctx, 'tag');
  this.handler_ = new goog.events.EventHandler(this);
  this.handler_.listen(this.cs_, net.bluemind.container.service.ContainerService.EventType.CHANGE, function(e) {
    this.dispatchEvent(e);
  });
};
goog.inherits(net.bluemind.tag.service.TagService, goog.events.EventTarget);

/**
 * @type {net.bluemind.mvp.ApplicationContext}
 */
net.bluemind.tag.service.TagService.prototype.ctx;

net.bluemind.tag.service.TagService.prototype.isLocal = function() {
  return this.css_.available();
};

/**
 * @param {Object.<string, function():T>} states
 * @param {Array.<*>} params
 * @return {T}
 * @template T
 */
net.bluemind.tag.service.TagService.prototype.handleByState = function(states, params) {
  return net.bluemind.mvp.helper.ServiceHelper.handleByState(this.ctx, this, states, params);
};

/**
 * @return {goog.Promise}
 */
net.bluemind.tag.service.TagService.prototype.getPersonalTagsContainer = function() {
  // personal tags
  return goog.Promise.resolve({"uid":"tags_"+this.ctx.user['uid']});
};

/**
 * @param {Object} tag
 * @return {goog.Promise}
 */
net.bluemind.tag.service.TagService.prototype.createTag = function(tag) {
  var item = {
    'uid' : tag['itemUid'],
    'container' : null,
    'name' : tag['label'],
    'value' : {
      'label' : tag['label'],
      'color' : tag['color']
    }
  };

  return this.getPersonalTagsContainer().then(function(tagContainer) {
    item['container'] = tagContainer['uid'];
    tag['containerUid'] = tagContainer['uid'];
    return this.handleByState({
      'local,remote' : this.createTagsLocalRemote, //
      'local' : this.createTagsLocal, //
      'remote' : this.createTagsRemote
    }, [ [ item ], tagContainer['uid'] ]);
  }, null, this);
}

/**
 * @param {Array.<Object>} tags
 * @return {goog.Promise}
 */
net.bluemind.tag.service.TagService.prototype.createTags = function(tags) {

  return this.getPersonalTagsContainer().then(function(tagContainer) {

    var items = goog.array.map(tags, function(tag) {
      var item = {
        'uid' : net.bluemind.mvp.UID.generate(),
        'value' : {
          'label' : tag['label'],
          'color' : tag['color']
        }
      };
      tag['containerUid'] = tagContainer['uid'];
      tag['itemUid'] = item['uid'];
      return item;
    });
    return this.handleByState({
      'local,remote' : this.createTagsLocalRemote, //
      'local' : this.createTagsLocal, //
      'remote' : this.createTagsRemote
    }, [ items, tagContainer['uid'] ]);
  }, null, this);
}

/**
 * @param {Array.<Object>} entries
 * @param {string} containerUid
 * @return {goog.Promise}
 */
net.bluemind.tag.service.TagService.prototype.createTagsLocalRemote = function(entries, containerUid) {
  var client = new net.bluemind.tag.api.TagsClient(this.ctx.rpc, '', containerUid);
  return client.updates({
    'add' : entries,
    'modify' : [],
    'delete' : []
  }).then(function() {
    goog.array.forEach(entries, function(entry) {
      entry['name'] = entry['value']['label'];
    });
    return this.cs_.storeItemsWithoutChangeLog(containerUid, entries);
  }, null, this);
}

/**
 * @param {Array.<Object>} entries
 * @param {string} containerUid
 * @return {goog.Promise}
 */
net.bluemind.tag.service.TagService.prototype.createTagsLocal = function(entries, containerUid) {
  var p = goog.array.map(entries, function(entry) {
    entry['containerUid'] = containerUid;
    entry['name'] = entry['value']['label'];
    return this.cs_.storeItem(entry);
  }, this);
  return goog.Promise.all(p);
}

/**
 * @param {Array.<Object>} entries
 * @param {string} containerUid
 * @return {goog.Promise}
 */
net.bluemind.tag.service.TagService.prototype.createTagsRemote = function(entries, containerUid) {
  var client = new net.bluemind.tag.api.TagsClient(this.ctx.rpc, '', containerUid);
  return (/** @type {goog.Promise} */
  (client.updates({
    'add' : entries,
    'modify' : [],
    'delete' : []
  })));
}

/**
 * @return {goog.Promise}
 */
net.bluemind.tag.service.TagService.prototype.getTags = function() {

  return this.handleByState({
    'local' : this.getTagsLocal,
    'remote' : this.getTagsRemote
  }, []);
}

/**
 * @return {goog.Promise}
 */
net.bluemind.tag.service.TagService.prototype.getTagsLocal = function() {
  return this.css_.getItems().then(function(tags) {
    var ret = goog.array.map(tags, function(tag) {

      var tagRef = {
        'containerUid' : tag['container'],
        'itemUid' : tag['uid'],
        'label' : tag['value']['label'],
        'color' : tag['value']['color']
      };
      return tagRef;
    });
    return ret;
  });
}

/**
 * @return {goog.Promise}
 */
net.bluemind.tag.service.TagService.prototype.getTagsRemote = function() {
  var containersClient = new net.bluemind.core.container.api.ContainersClient(this.ctx.rpc, '');
  return containersClient.all({
    'type' : 'tags'
  }).then(function(containers) {
    var ret = goog.array.map(containers, function(container) {
      var client = new net.bluemind.tag.api.TagsClient(this.ctx.rpc, '', container['uid']);
      return client.all().then(function(tags) {
        return goog.array.map(tags, function(tag) {
          var tagRef = {
            'containerUid' : container['uid'],
            'itemUid' : tag['uid'],
            'label' : tag['value']['label'],
            'color' : tag['value']['color']
          };
          return tagRef;
        });
      }, null, this);
    }, this);
    return goog.Promise.all(ret);
  }, null, this).then(function(tags) {
    return goog.array.flatten(tags);
  });
}