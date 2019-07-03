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
 * @fileoverview Contact synchronization service.
 */

goog.provide('bluemind.container.persistance.ItemHomeTests');

goog.require('bluemind.container.persistance.testdata');
goog.require('bluemind.container.persistance.schema');
goog.require('goog.debug.Console');
goog.require('goog.testing.jsunit');
goog.require('goog.testing.PropertyReplacer');
goog.require('ydn.db.con.IndexedDb');
goog.require('ydn.debug');
goog.require('ydn.async');
goog.require('ydn.db.Storage');
goog.require('goog.testing.PropertyReplacer');

goog.require('goog.storage.mechanism.IEUserData');
goog.require('goog.storage.mechanism.IterableMechanism');

goog.require('bluemind.container.persistance.WSItemHome');
goog.require('bluemind.container.persistance.DBItemHome');
goog.require('bluemind.container.persistance.IItemHome');

goog.require('relief.rpc.RPCService');
goog.require('relief.cache.Cache');
goog.provide('bluemind.db');

goog.require('bluemind.tests');

goog.require('bluemind.storage.StorageHelper');


goog.require('goog.testing.AsyncTestCase');
goog.require('goog.testing.jsunit');

var asyncTestCase = goog.testing.AsyncTestCase.createAndInstall();
var cache = null;
var rpc = null;
var book = null;
var auth = null;
var sp = null;
var stubs;
var home = null;

function setUp() {
	cache = new relief.cache.Cache();
	
	bluemind.privacy = true;
	bluemind.application = 'contact';
	bluemind.db.schema = bluemind.container.persistance.schema;

	ydn.db.base.NO_IDB = false;
	stubs = new goog.testing.PropertyReplacer();

	stubs.setPath('bluemind.user.getId', function() {
		return "test";
	});
}

function testStoreEntry_DB() {
	doTestStoreEntry(withDBStorage, withDBHome);
}

function testGetEntry_DB() {
	doTestGetEntry(withDBStorage, withDBHome);
}

function testDeleteEntry_DB() {
	doTestDeleteEntry(withDBStorage, withDBHome);
}

function testFindEntriesByFolder_DB() {
	doTestFindEntriesByFolder(withDBStorage, withDBHome);
}

function testGetLocalChangeSet_DB() {
	doTestGetLocalChangeSet(withDBStorage, withDBHome);
}

function testSyncFolderEntries_DB() {
	doTestSyncFolderEntries(withDBStorage, withDBHome);
}

function testSearchEntriesByFolder_DB() {
	doTestSearchEntriesByFolder(withDBStorage, withDBHome);
}


function testStoreEntry_WS() {
	doTestStoreEntry(withWSStorage, withWSHome);
}

function testGetEntry_WS() {
	doTestGetEntry(withWSStorage, withWSHome);
}

function testDeleteEntry_WS() {
	doTestDeleteEntry(withWSStorage, withWSHome);
}

function testFindEntriesByFolder_WS() {
	doTestFindEntriesByFolder(withWSStorage, withWSHome);
}

function testSearchEntriesByFolder_WS() {
	doTestSearchEntriesByFolder(withWSStorage, withWSHome);
}

function testGetLocalChangeSet_WS() {
	doTestGetLocalChangeSet(withWSStorage, withWSHome);
}


function testSyncFolderEntries_WS() {
	doTestSyncFolderEntries(withWSStorage, withWSHome);
}

function doTestStoreEntry(storageFunction, homeFunction) {

	asyncTestCase.waitForAsync('test store entry');

	var entry = {
		'id' : 'test',
		'folderId' : 'f1',
		'name' : 'check'
	};
	var folder = 'f1';

	bluemind.tests.chain([ //
	storageFunction, // storage
	homeFunction, //
	function() {
		return home.storeItem(folder, entry);
	}, function() {
		asyncTestCase.continueTesting();
	} ]);
};

function doTestGetEntry(storageFunction, homeFunction) {

	asyncTestCase.waitForAsync('test store entry');

	var entry = {
		'id' : 'test',
		'name' : 'check'
	};

	var folder = 'f1';

	bluemind.tests.chain([ //
	storageFunction, // storage
	homeFunction, //
	function() {
		return home.storeItem(folder, entry);
	}, function() {
		return home.getItem(folder, entry['id']);
	}, function(e) {
		assertNotNull(e);
	}, function() {
		return home.getItem(folder, 'fakeId');
	}, function(e) {
		assertTrue(e == undefined);
		asyncTestCase.continueTesting();
	} ]);
};

function doTestDeleteEntry(storageFunction, homeFunction) {
	asyncTestCase.waitForAsync('test delete entry');

	var entry = {
		'id' : 'test',
		'name' : 'check'
	};

	var folder = 'f1';

	bluemind.tests.chain([ //
	storageFunction, // storage
	homeFunction, //
	function() {
		return home.storeItem(folder, entry);
	}, function() {
		return home.deleteItem(folder, entry['id']);
	}, function() {
		asyncTestCase.continueTesting();
	} ]);
};

function doTestFindEntriesByFolder(storageFunction, homeFunction) {
	asyncTestCase.waitForAsync('test store entry');
	var entry = {
		'id' : 'test',
		'name' : 'a'
	};

	var entry2 = {
		'id' : 'test2',
		'name' : 'b'
	};

	var entry3 = {
		'id' : 'test3',
		'name' : 'check'
	};

	var folder = 'ff1';

	var folder2 = 'ff2';
	bluemind.tests.chain([ //
	storageFunction, // storage
	homeFunction, //
	function() {
		var r1 = home.storeItem(folder, entry);
		var r2 = home.storeItem(folder, entry2);
		var r3 = home.storeItem(folder2, entry3);
		new goog.async.DeferredList([ r1, r2, r3 ]);
	}, function() {
		return home.getItems(folder, 0);
	}, function(res) {
		assertNotNull(res);
		assertEquals(2, res.getCount());
		assertEquals('test', res.getItems()[0].id);
		assertEquals('test2', res.getItems()[1].id);
		asyncTestCase.continueTesting();
	} ]);
};

function doTestSearchEntriesByFolder(storageFunction, homeFunction) {
	asyncTestCase.waitForAsync('test search entries');

	var entry = {
		'id' : 'test',
		'name' : 'joel'
	};

	var entry2 = {
		'id' : 'test2',
		'name' : 'joey'
	};

	var entry3 = {
		'id' : 'test3',
		'name' : 'starr'
	};

	var folder = 'f1';
	var folder2 = 'f2';

	bluemind.tests.chain([ //
	storageFunction, // storage
	homeFunction, //
	function() {
		var r1 = home.storeItem(folder, entry);
		var r2 = home.storeItem(folder, entry2);
		var r3 = home.storeItem(folder2, entry3);
		return new goog.async.DeferredList([ r1, r2, r3 ]);
	}, function() {
		return home.searchItems('jo', true, 0);
	}, function(res) {
		assertNotNull(res);
		assertEquals(2, res.getCount());
		assertEquals('test', res.getItems()[0].id);
		assertEquals('test2', res.getItems()[1].id);
		asyncTestCase.continueTesting();
	} ]);

};

function doTestGetLocalChangeSet(storageFunction, homeFunction) {
	asyncTestCase.waitForAsync('test search entries');

	var entry = {
		'id' : 'test',
		'name' : 'joel'
	};

	var entry2 = {
		'id' : 'test2',
		'name' : 'joey'
	};

	var entry3 = {
		'id' : 'test3',
		'name' : 'starr'
	};

	var folder = 'f1';
	var folder2 = 'f2';

	bluemind.tests.chain([ //
	storageFunction, // storage
	homeFunction, //
	function() {
		var r1 = home.storeItem(folder, entry);
		var r2 = home.storeItem(folder, entry2);
		var r3 = home.storeItem(folder2, entry3);
		return new goog.async.DeferredList([ r1, r2, r3 ]);
	}, function() {
		return home.getLocalChangeSet('f1');
	}, function(changes) {
		assertNotNull(changes);
		assertEquals(2, changes.length);
		var change1 = changes[0];
		var change2 = changes[1];

		assertEquals('test', change1.itemId);
		assertEquals('modified', change1.type);
		assertEquals('test2', change2.itemId);
		assertEquals('modified', change2.type);
	}, function() {
		return home.deleteItem(folder, entry2.id);
	}, function() {
		return home.getLocalChangeSet('f1');
	}, function(changes) {
		assertNotNull(changes);
		assertEquals(2, changes.length);
		var change1 = changes[0];
		var change2 = changes[1];

		assertEquals('test', change1.itemId);
		assertEquals('modified', change1.type);
		assertEquals('test2', change2.itemId);
		assertEquals('deleted', change2.type);
	}, function() {
		return home.syncFolderEntries(folder, [], [], 1);
	}, function() {
		return home.getLocalChangeSet('f1');
	}, function(changes) {
		assertNotNull(changes);
		assertEquals(2, changes.length);
		asyncTestCase.continueTesting();
	} ]);

};

function doTestSyncFolderEntries(storageFunction, homeFunction) {
	asyncTestCase.waitForAsync('test sync folder entries');

	var entry = {
		'id' : 'test',
		'name' : 'joel'
	};

	var entry2 = {
		'id' : 'test2',
		'name' : 'joey'
	};

	var entry3 = {
		'id' : 'test3',
		'name' : 'starr'
	};

	var folder = 'f1';
	var folder2 = 'f2';

	bluemind.tests.chain([ //
	storageFunction, // storage
	homeFunction, //
	function() {
		var r1 = home.storeItem(folder, entry);
		var r2 = home.storeItem(folder, entry2);
		var r3 = home.storeItem(folder2, entry3);
		return new goog.async.DeferredList([ r1, r2, r3 ]);
	}, function() {
		return home.getLocalChangeSet('f1');
	}, function(changes) {
		assertNotNull(changes);
		assertEquals(2, changes.length);
	}, function() {
		return home.syncItems('f1', [], [], 5);
	}, function() {
		return home.getLocalChangeSet('f1');
	}, function(changes) {
		assertEquals(2, changes.length);
	}, function() {
		entry.name = 'jojo';
		entry2.name = 'koko';
		return home.syncItems('f1', [ entry, entry2 ], [], 6);
	}, function() {
		return home.getLocalChangeSet('f1');
	}, function(changes) {
		assertEquals(0, changes.length);
		return home.getItem(folder, 'test');
	}, function(res) {
		assertEquals('jojo', res['name']);
		asyncTestCase.continueTesting();
	} ]);
};

var withDBStorage = function() {
	return bluemind.storage.StorageHelper.initStorage();
};

var withDBHome = function() {
	home = new bluemind.container.persistance.DBItemHome();
	return;
};


var withWSStorage = function() {
	return bluemind.storage.StorageHelper.initStorage();
};

var withWSHome = function() {
	home =  new bluemind.container.persistance.WSItemHome();
	return;
};