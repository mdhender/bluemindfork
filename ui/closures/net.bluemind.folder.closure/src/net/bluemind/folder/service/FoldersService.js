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
 * @fileoverview Provide services for tasks
 */
goog.provide('net.bluemind.folder.service.FoldersService');
goog.require('goog.async.Deferred');
goog.require('net.bluemind.container.service.ContainerService');
goog.require('net.bluemind.mvp.helper.ServiceHelper');
goog.require('goog.events.EventTarget');
goog.require('net.bluemind.container.service.ContainerService');
goog.require('net.bluemind.container.service.ContainersService');
goog.require("net.bluemind.core.container.api.ContainersFlatHierarchyClient");
goog.require("net.bluemind.core.container.api.OwnerSubscriptionsClient");

/**
 * Service provdier object for Tasks
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context.
 * @constructor
 * @extends {goog.events.EventTarget}
 */
net.bluemind.folder.service.FoldersService = function(ctx) {
  goog.base(this);
  this.ctx = ctx;

  this.css_ = new net.bluemind.container.service.ContainersService(ctx, 'folder');
  this.cs_ = new net.bluemind.container.service.ContainerService(ctx, 'folder');
  this.handler_ = new goog.events.EventHandler(this);
  this.handler_.listen(this.cs_, net.bluemind.container.service.ContainerService.EventType.CHANGE, this.handleChange_);
  this.ctx.service('containersObserver').observerContainers('owner_subscriptions', [ this.getFolderContainerUid_() ]);
};
goog.inherits(net.bluemind.folder.service.FoldersService, goog.events.EventTarget);

var FILTER_CONTAINER = function(folder) {
  return folder['containerDescriptor'];
};

net.bluemind.folder.service.FoldersService.prototype.isLocal = function() {
  return this.css_.available();
};

net.bluemind.folder.service.FoldersService.prototype.getFolder = function(uid) {
  return this.handleByState({
    'local' : this.getFolderLocal, //
    'remote' : this.getFolderRemote
  }, [ uid ]);
}

net.bluemind.folder.service.FoldersService.prototype.getFolderContainerUid_ = function() {
  return 'owner_subscriptions_' + this.ctx.user['uid'] + '_at_' + this.ctx.user['domainUid'];
}
net.bluemind.folder.service.FoldersService.prototype.getFolderLocal = function(uid) {
  return this.cs_.searchItems(null, [ [ 'containerDescriptor.uid', '=', uid ] ]).then(function(folders) {
    if (folders.length > 0) {
      return FILTER_CONTAINER(folders[0]);
    }
  });
};

net.bluemind.folder.service.FoldersService.prototype.getFolderRemote = function(uid) {
  var client = new net.bluemind.core.container.api.ContainersClient(this.ctx.rpc, '');
  return client.get(uid);
};

net.bluemind.folder.service.FoldersService.prototype.handleByState = function(states, params) {
  return net.bluemind.mvp.helper.ServiceHelper.handleByState(this.ctx, this, states, params);
};

net.bluemind.folder.service.FoldersService.prototype.getFolders = function(type, opt_uids) {
  return this.handleByState({
    'local' : this.getFoldersLocal,
    'remote' : this.getFoldersRemote
  }, [ type, opt_uids ]);
}

net.bluemind.folder.service.FoldersService.prototype.getFoldersLocal = function(type, opt_uids) {
  if (type && !opt_uids) {
    // null because we do not need to filter on container uid because the is
    // only one container (for now ?)
    return this.cs_.searchItems(null, [ [ 'containerDescriptor.type', '=', type ] ]).then(function(folders) {
      return goog.array.map(folders, FILTER_CONTAINER);
    });
  } else {
    return this.css_.mget(opt_uids);
  }
};

net.bluemind.folder.service.FoldersService.prototype.refreshFolder = function(uid) {
  if (!this.isLocal()){
    return null;
  }
  var containersClient = new net.bluemind.core.container.api.ContainersClient(this.ctx.rpc, '');
  var dbFolder;
  return this.cs_.searchItems(null, [ [ 'containerDescriptor.uid', '=', uid ] ]).then(function(folders) {
    if (folders.length == 1) {
      dbFolder = folders[0];
      return containersClient.get(uid);
    } else {
      // console.log("folder not found !!!!", uid);
      return null;
    }
  }, null, this).then(function(cont) {
    if (cont == null) {
      return;
    }
    // only refresh writable flag
    if (cont['writable'] != dbFolder['containerDescriptor']['writable']) {
      dbFolder['containerDescriptor']['writable'] = cont['writable'];
      return this.cs_.storeItemWithoutChangeLog(dbFolder);
    }
  }, null, this);
}

net.bluemind.folder.service.FoldersService.prototype.getFoldersRemote = function(type, opt_uids) {
  if (type && !opt_uids) {
    var userSub = new net.bluemind.core.container.api.OwnerSubscriptionsClient(this.ctx.rpc, '', this.ctx.user['domainUid'], this.ctx.user['uid']);
    return userSub.list().then(function(result) {
      var uids = goog.array.map(goog.array.filter(result, function(node) {
        return type == node['value']['containerType'];
      }), function(node) {
        return node['value']['containerUid'];
      });;
      return this.getRemoteByUids_(uids);
    }, null, this);
  } else {
    return this.getRemoteByUids_(opt_uids);
  }
};

net.bluemind.folder.service.FoldersService.prototype.getRemoteByUids_ = function(uids) {
  var containersClient = new net.bluemind.core.container.api.ContainersClient(this.ctx.rpc, '');
  var directoryClient = new net.bluemind.directory.api.DirectoryClient(this.ctx.rpc, '', this.ctx.user['domainUid']);
  var folders = [];
  return containersClient.getContainers(uids).then(function(containers) {
    folders = containers;
    var owners = goog.array.map(folders, function(folder) {
      return folder['owner'];
    });
    return directoryClient.getMultiple(owners);
  }, null, this).then(function(owners) {
    goog.array.forEach(folders, function(folder) {
      folder['dir'] = goog.array.find(owners, function(owner) {
        return owner['uid'] == folder['owner'];
      })
      if (folder['dir'] != null) {
        folder['dir']= folder['dir']['value'];
      }
    })
    return folders;
  }, null, this);
}

net.bluemind.folder.service.FoldersService.prototype.getSettings = function(uid) {
  return this.getFolder(uid).then(function(folder) {
    if (folder && folder['settings']) {
      return folder['settings'];
    } else {
      return null;
    }
  });
}

net.bluemind.folder.service.FoldersService.prototype.setSettings = function(uid, settings) {
  return this.handleByState({
    'local,remote' : this.setSettingsLocalRemote, //
    'local' : this.setSettingsLocal, //
    'remote' : this.setSettingsRemote
  }, [ uid, settings ]);
}

net.bluemind.folder.service.FoldersService.prototype.setSettingsLocalRemote = function(uid, settings) {
  var cMgmt = new net.bluemind.core.container.api.ContainerManagementClient(this.ctx.rpc, '', uid);
  return cMgmt.setPersonalSettings(settings).then(function() {
    return this.css_.setSettingsWithoutChangeLog(uid, settings);
  }, null, this);
}

net.bluemind.folder.service.FoldersService.prototype.setSettingsLocal = function(uid, settings) {
  return this.css_.setSettings(uid, settings);
}

net.bluemind.folder.service.FoldersService.prototype.setSettingsRemote = function(uid, settings) {
  var cMgmt = new net.bluemind.core.container.api.ContainerManagementClient(this.ctx.rpc, '', uid);
  return cMgmt.setPersonalSettings(settings);
}

net.bluemind.folder.service.FoldersService.prototype.handleChange_ = function(e) {
  this.dispatchEvent(net.bluemind.container.service.ContainersService.EventType.CHANGE);
}