goog.provide("net.bluemind.addressbook.api.AddressBookClientTest");

goog.require('goog.testing.PseudoRandom');
goog.require('relief.rpc.Command');
goog.require('relief.rpc.RPCService');
goog.require('relief.cache.Cache');
goog.require("net.bluemind.addressbook.api.AddressBookClient");
goog.require('goog.string');
goog.require('goog.testing.AsyncTestCase');
goog.require('goog.testing.jsunit');

var asyncTestCase = goog.testing.AsyncTestCase.createAndInstall();

var cache = null;
var rpc = null;
var book = null;

function setUp() {
	cache = new relief.cache.Cache();
	rpc = new relief.rpc.RPCService(cache, {
		'X-BM-ApiKey' : 'MAGIC'
	});
	book = new net.bluemind.addressbook.api.AddressBookClient(rpc,
			"http://localhost:8090", "1234test5");

}
function testChangeset() {
	// FIXME
	// return;

	asyncTestCase.waitForAsync('addressbook changset');

	var res = book.changeset();

	res.addCallback(function(e) {
		assertTrue(true);
		asyncTestCase.continueTesting();
	}, null);

	res.addErrback(function(e) {
		fail("error during call " + e);
		asyncTestCase.continueTesting();
	});
};

function testCreate() {
	asyncTestCase.waitForAsync('addressbook create');

	var res = book.create('tes' + goog.string.getRandomString(), {
		"kind" : "individual",
		'identification' : {
			"formatedName" : {
				"value" : "jo"
			}
		},
		"deliveryAddressing" : [],
		"explanatory" : {}
	});

	res.addCallback(function(e) {
		assertTrue(true);
		asyncTestCase.continueTesting();
	}, null);

	res.addErrback(function(e) {
		fail("error during call " + e);
		asyncTestCase.continueTesting();
	});
}

function testMultipleGet() {
	asyncTestCase.waitForAsync('addressbook multipleget');

	var res = book.multipleGet([ 'test1234', 'tesiztrxm544tx3' ]);
	res.addCallback(function(e) {
		assertTrue(true);
		asyncTestCase.continueTesting();
	}, null);

	res.addErrback(function(e) {
		fail("error during call " + e);
		asyncTestCase.continueTesting();
	});
}

function testUpdates() {
	asyncTestCase.waitForAsync('addressbook updates');

	var changes = {
		'add' : [ {'uid': 'tes' + goog.string.getRandomString(), 'value':{
			"kind" : "individual",
			'identification' : {
				"formatedName" : {
					"value" : "jo"
				}
			},
			"deliveryAddressing" : [],
			"explanatory" : {}
		}} ],
		'modify' : [],
		'delete' : []
	};

	var res = book.updates(changes);
	res.addCallback(function(e) {
		assertTrue(true);
		asyncTestCase.continueTesting();
	}, null);

	res.addErrback(function(e) {
		fail("error during call " + e);
		asyncTestCase.continueTesting();
	});
}
