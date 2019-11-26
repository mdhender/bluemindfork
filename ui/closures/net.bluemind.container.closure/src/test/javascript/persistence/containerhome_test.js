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

goog.provide('bluemind.container.persistence.ContainerHomeHomeTests');
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

goog.require('bluemind.container.persistence.IContainerHome');
goog.require('bluemind.container.persistence.DBContainerHome');
goog.require('bluemind.container.persistence.WSContainerHome');

goog.require('bluemind.container.persistence.schema');
goog.provide('bluemind.db');
goog.require('bluemind.tests');

goog.require('bluemind.storage.StorageHelper');

var asyncTestCase = goog.testing.AsyncTestCase.createAndInstall();
var cache = null;
var rpc = null;
var book = null;
var auth = null;
var sp = null;
var stubs;
var home = null;

function setUp() {
	
	
	var iframe = /** @type {!HTMLIFrameElement} */
	(goog.dom.getElement('history_frame'));
	var input = /** @type {!HTMLInputElement} */
	(goog.dom.getElement('history_input'));
	var content = /** @type {!Element} */
	(goog.dom.getElement('content-body'));

	bluemind.privacy = true;
	bluemind.application = 'contact';
	bluemind.db.schema = bluemind.container.persistence.schema;

	ydn.db.base.NO_IDB = false;
	stubs = new goog.testing.PropertyReplacer();

	stubs.setPath('bluemind.user.getId', function() {
		return "test";
	});

}

function testSyncFolders_WS() {
	doTestSyncFolders(withWSStorage, withWSHome);
};

function testGetSetSyncVersion_WS() {
	doTestGetSetSyncVersion(withWSStorage, withWSHome);
}

function testSyncFoldersMultipleCall_WS() {
	doTestSyncFoldersMultipleCall(withWSStorage, withWSHome);
}

function testSyncFolders_DB() {
	doTestSyncFolders(withDBStorage, withDBHome);
};
function testGetSetSyncVersion_DB() {
	doTestGetSetSyncVersion(withDBStorage, withDBHome);
};

function testSyncFoldersMultipleCall_DB() {
	doTestSyncFoldersMultipleCall(withDBStorage, withDBHome);
};

function doTestSyncFolders(storageFunction, homeFunction) {

	var folder = { 'id':'test1', 'name':'test1'};

	
	var folder2 = { 'id':'test2', 'name':'test2'};

	asyncTestCase.waitForAsync('folders sync');

	bluemind.tests.chain([ //
	storageFunction, // storage
	homeFunction, //
	function() {
		result = home.updates([ folder, folder2 ]);
		assertNotNull(result);
		return result;
	}, function() {
		return home.getContainers();
	}, function(res) {
		assertNotNull(res);
		assertEquals(2, res.length);
		assertEquals('test1', res[0].id);
		assertEquals('test2', res[1].id);
		asyncTestCase.continueTesting();
	} ]);

};

function doTestSyncFoldersMultipleCall(storageFunction, homeFunction) {
	var folder = {'id':'test1', 'name':'test1'};
	var folder2 = {'id':'test2', 'name':'test2'};
	var folder3 = {'id':'test3', 'name':'test3'};
	
	asyncTestCase.waitForAsync('folders sync');

	bluemind.tests.chain([ //
	storageFunction, // storage
	homeFunction, //
	function() {
		return home.updates([ folder, folder2 ]);
	}, function() {
		return home.updates([ folder2, folder3 ]);
	},

	function() {
		return home.getContainers();
	}, function(res) {
		assertNotNull(res);
		assertEquals(2, res.length);
		assertEquals('test2', res[0].id);
		assertEquals('test3', res[1].id);
		asyncTestCase.continueTesting();
	} ]);

};

function doTestGetSetSyncVersion(storageFunction, homeFunction) {
	bluemind.tests.chain([ //
	storageFunction, // storage
	homeFunction, //
	function() {
		return home.setSyncVersion('test1', 1);
	}, function() {
		return home.getSyncVersion('test1');
	}, function(res) {
		assertNotNull(res);
		assertEquals(1, res);
	}, function() {
		return home.setSyncVersion('test1', undefined);
	}, function() {
		return home.getSyncVersion('test1');
	}, function(res) {
		assertEquals(undefined, res);
		asyncTestCase.continueTesting();
	} ]);
};

var withWSStorage = function() {
	return bluemind.storage.StorageHelper.initStorage();
};

var withWSHome = function() {
	home = new bluemind.container.persistence.WSContainerHome();
	return;
};

var withDBStorage = function() {
	return bluemind.storage.StorageHelper.initStorage();
};

var withDBHome = function() {
	home = new bluemind.container.persistence.DBContainerHome();
	return;
};
