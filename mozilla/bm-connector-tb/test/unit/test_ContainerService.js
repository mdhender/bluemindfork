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

Components.utils.import("resource://bm/bmUtils.jsm");
Components.utils.import("resource://gre/modules/Promise.jsm");

const baseUrl = "http://mocked";

const Cc = Components.classes;
const Ci = Components.interfaces;

var container;

add_test(function _testSync() {

    //override funcs to return test datas
    
    UserSubscriptionClient.prototype.listSubscriptions = function() {
        return Promise.resolve([ {
            "containerUid" : "t1",
            "name" : "test1",
            "owner" : "test",
            "ownerDisplayname" : "test",
            "offlineSync": true,
            "containerType": "addressbook"
        }, {
            "containerUid" : "t2",
            "name" : "test2",
            "owner" : "test",
            "ownerDisplayname" : "test",
            "offlineSync": true,
            "containerType": "addressbook"
        } ]);
    }

    ContainersClient.prototype.get = function(uid) {
        switch (uid) {
            case "t1":
                return Promise.resolve({
                    "uid" : "t1",
                    "name" : "test1",
                    "owner" : "test",
                    "ownerDisplayname" : "test",
                    "offlineSync": true,
                    "verbs": ["Manage","Invitation","Read","Write","All","Freebusy"]
                });
            case "t2":
                return Promise.resolve({
                    "uid" : "t2",
                    "name" : "test2",
                    "owner" : "test",
                    "ownerDisplayname" : "test",
                    "offlineSync": true,
                    "verbs": ["Manage","Invitation","Read","Write","All","Freebusy"]
                });
        }
    }

    ContainersClient.prototype.all = function() {
        return Promise.resolve([ {
            "uid" : "t1",
            "name" : "test1",
            "owner" : "test",
            "ownerDisplayname" : "test",
            "offlineSync": true,
            "verbs": ["Manage","Invitation","Read","Write","All","Freebusy"]
        }, {
            "uid" : "t2",
            "name" : "test2",
            "owner" : "test",
            "ownerDisplayname" : "test",
            "offlineSync": true,
            "verbs": ["Manage","Invitation","Read","Write","All","Freebusy"]
        } ]);
    }
    
    AddressBookClient.prototype.updates = function(localChanges) {
        return Promise.resolve();
    }
    
    AddressBookClient.prototype.changeset = function(version) {
        return Promise.resolve({
            version : 5,
            created : [ 'c1', 'c2' ],
            updated : [],
            deleted : []
        });
    }
    
    AddressBookClient.prototype.getComplete = function(uid) {
        return Promise.resolve({
            'uid' : uid,
            'version' : 5,
            'displayName' : 'test',
            'value' : {
            'kind' : 'individual',
            'identification' : {'formatedName':{'value':'toto'+uid},
                                'gender':{'value':'male'}},
            'deliveryAddressing' : []
            }
        });
    }
    
    AddressBookClient.prototype.multipleGet = function(uids) {
        return Promise.resolve([{
            'uid' : uids[0],
            'version' : 5,
            'displayName' : 'test',
            'value' : {
            'kind' : 'individual',
            'identification' : {'formatedName':{'value':'toto'+uids[0]},
                                'gender':{'value':'male'}},
            'deliveryAddressing' : []
            }
        }]);
    }

    // test

    let localFolders;
    let result = container.sync();
    result.then(function(aSuccessReason) {
        dump("aSuccessReason: " + aSuccessReason + "\n");
        do_check_eq(true, aSuccessReason[0].success);
        do_check_eq(true, aSuccessReason[1].success);
        
    }).then(function() {
        return BMFolderHome.getFolders();
    
    }).then(function(folders) {
        do_check_eq(2, folders.length);
        localFolders = folders;
        return BMFolderHome.getFolderSyncVersion(folders[0]);
    
    }).then(function(version) {
        do_check_eq(5, version);
        return BMFolderHome.getEntry(localFolders[0], 'c1');
    
    }).then(function(entry) {
        do_check_eq('totoc1', entry.value.identification.formatedName.value);
        run_next_test();
    
    }, function(aRejectReason) {
        do_throw(aRejectReason);
    
    });

});

function run_test() {

    dump("test started" + "\n");
    
    var loader = Cc["@mozilla.org/moz/jssubscript-loader;1"].getService(Ci.mozIJSSubScriptLoader);
    loader.loadSubScript("chrome://bm/content/core2/ContainerService.js");
    
    container = new BMContainerService(baseUrl, null, {uid:"test"});
    
    do_check_neq(null, container);
    
    run_next_test();
}
