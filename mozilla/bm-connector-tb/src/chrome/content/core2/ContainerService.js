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
 * Contact synchronization service.
 */

Components.utils.import("resource://bm/core2/BMFolderHome.jsm");

BMContainerService = function(aBaseUrl, aAuthKey, aUser) {
    this._logger = Components.classes["@blue-mind.net/logger;1"].getService()
                                .wrappedJSObject.getLogger("BMContainerService: ");
    this._baseUrl = aBaseUrl;
    this._authKey = aAuthKey;
    this._user = aUser;
    let loader = Cc["@mozilla.org/moz/jssubscript-loader;1"]
                  .getService(Ci.mozIJSSubScriptLoader);
    loader.loadSubScript("chrome://bm/content/core2/client/AddressBookClient.js");
    loader.loadSubScript("chrome://bm/content/core2/client/ContainersClient.js");
    loader.loadSubScript("chrome://bm/content/core2/client/UserSubscriptionClient.js");
};

BMContainerService.prototype.sync = function() {
    let books = new UserSubscriptionClient(this._baseUrl, this._authKey, this._user.domainUid);
    let containers = new ContainersClient(this._baseUrl, this._authKey);
  
    let self = this;
    let descriptors = [];
    let folders;
    let result = new Promise(function (resolve, reject) {
    
        try {
            let r = books.listSubscriptions(self._user.uid, "addressbook");
            r.then(function(subs) {
                //get container descriptors one by one
                return subs.reduce(function(sequence, sub) {
                    return sequence.then(function() {
                        return containers.get(sub.containerUid);
                    }).then(function(descriptor) {
                        descriptors.push(descriptor);
                    }, function(rej) {
                        self._logger.error(rej);
                        //skip not found subscribed container
                    });
                }, Promise.resolve());

            }).then(function() {
                folders = descriptors.map(_adaptContainer);
                return BMFolderHome.syncFolders(folders, self._user);
        
            }).then(function() {
                let syncFolders = [];
                for (let f of folders) {
                    if (f.isSync) {
                        syncFolders.push(f);
                    }
                }
                folders = syncFolders;
                return self.syncFolders(folders);
                
            }).then(function(results) {
                resolve(results);
        
            }, function(rej) {
                reject(rej);
        
            });
        } catch(err) {
            reject(err);
        }
    });
    return result;
};

BMContainerService.prototype.syncFolders = function(folders) {
    let promises = folders.map(this.syncFolder, this);
    return Promise.all(promises);
};

BMContainerService.prototype.syncFolder = function(folder) {
    let book = new AddressBookClient(this._baseUrl, this._authKey, folder.id);
    
    let changeset;
    let deletedItems;
    let applyChanges;
    
    let self = this;
    let result = new Promise(function(resolve, reject) {
        try {
            let localChanges = BMFolderHome.getLocalChangeSet(folder);
            localChanges.then(function(changes) {
                applyChanges = [];
                // get changes to apply one by one
                return changes.reduce(function(sequence, change) {
                    return sequence.then(function() {
                        if (change.type == "added") {
                            //get item and add to applyChange.add
                            let entry = BMFolderHome.getEntry(folder, change.itemId, change.kind);
                            return entry.then(function(entry) {
                                applyChanges.push({
                                    "type": "add",
                                    "change" : {
                                        "uid": change.itemId,
                                        "value": entry.value
                                    }
                                });
                            });
                        } 
                        if (change.type == "updated") {
                            //get item and add to applyChange.modify
                            let entry = BMFolderHome.getEntry(folder, change.itemId, change.kind);
                            return entry.then(function(entry) {
                                applyChanges.push({
                                    "type": "modify",
                                    "change" : {
                                        "uid": change.itemId,
                                        "value": entry.value
                                    }
                                });
                            });
                        } else {
                            applyChanges.push({
                                "type": "delete",
                                "change": {
                                    "uid": change.itemId
                                }
                            });
                            return Promise.resolve();
                        }
                    });
                }, Promise.resolve());
            
            }).then(function() {
                if (applyChanges.length > 0) {
                    //send changes 50 by 50
                    let batch = function(next, localChgs) {
                        return next.then(function() {
                            let containerChanges = {
                                "add" : [],
                                "modify" : [],
                                "delete" : []
                            };
                            for (let chg of localChgs) {
                                containerChanges[chg.type].push(chg.change);
                            }
                            self._logger.info("send " + localChgs.length +  " changes");
                            return book.updates(containerChanges);
                        }).then(function(updatesResult) {
                            let toAck = updatesResult.added.concat(updatesResult.updated);
                            return BMFolderHome.ackChanges(folder, toAck).then(function() {
                                BMFolderHome.setErrors(folder, updatesResult.errors);
                            });
                        });
                    };

                    let next = Promise.resolve();
                    let start = 0;
                    while (start < applyChanges.length) {
                        let end = start + 50;
                        let changes = applyChanges.slice(start, end);
                        next = batch(next, changes);
                        start = end;
                    }
                    
                    return next.then(function() {
                        return Promise.resolve();
                    });
                } else {
                    return Promise.resolve();
                }
                
            }).then(function() {
                return BMFolderHome.getLocalPhotos(folder, applyChanges);
            
            }).then(function(photos) {
                //upload photos one by one
                return photos.reduce(function(sequence, photo) {
                    return sequence.then(function() {
                        return book.deletePhoto(photo.contactId);
                    }).then(function() {
                        if (photo.data) {
                            return book.setPhoto(photo.contactId, photo.data);
                        } else {
                            return Promise.resolve();
                        }
                    }).then(function() {
                        return Promise.resolve();
                    });
                }, Promise.resolve());
                
            }).then(function() {
                return BMFolderHome.getFolderSyncVersion(folder);
            
            }).then(function(version) {
                return book.changeset(version);
            
            }).then(function(res) {
                changeset = res;
                deletedItems = changeset.deleted;
                
                let changed = changeset.created.concat(changeset.updated);
                let changedValues = [];
                //get items in changed 100 by 100
                
                let batch = function(next, uids) {
                    return next.then(function() {
                        return book.multipleGet(uids);
                    }).then(function(items) {
                        for (let item of items) {
                            changedValues.push(_adaptCardAsEntry(item));
                        }
                    });
                };
                
                let next = Promise.resolve();
                let start = 0;
                while (start < changed.length) {
                    let end = start + 100;
                    let uids = changed.slice(start, end);
                    next = batch(next, uids);
                    start = end;
                }
                
                return next.then(function() {
                    return changedValues;
                });
            
            }).then(function(changedValues) {
                return BMFolderHome.syncFolderEntries(folder, changedValues, deletedItems, changeset.version);
            
            }).then(function(res) {
                resolve({
                    fname: folder.name,
                    success: true,
                    value: res
                });
            }, function(rej) {
                self._logger.error(rej);
                resolve({
                    fname: folder.name,
                    success: false,
                    value: rej
                });
            });
        } catch(err) {
            self._logger.error(err);
            resolve({
                fname: folder.name,
                success: false,
                value: err
            });
        }
    });
    return result;
};

var _adaptContainer = function(container) {
    var folder = {};
    folder.id = container.uid;
    folder.name = container.name;
    folder.isDefault = container.defaultContainer;
    folder.isWritable = container.writable;
    folder.owner = container.owner;
    folder.ownerDisplayname = container.ownerDisplayname;
    folder.isSync = container.offlineSync;
    folder.isReadable = BMFolderHome.isReadable(container.verbs);
    return folder;
};

var _adaptCardAsEntry = function(itemValue) {
    var entry = {};
    entry.id = itemValue.uid;
    entry.name = itemValue.displayName;
    entry.value = itemValue.value;
    entry.externalId = itemValue.externalId;
    return entry;
};
