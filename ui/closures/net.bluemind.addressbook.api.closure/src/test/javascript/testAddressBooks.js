goog.provide("net.bluemind.addressbook.api.AddressBooksClientTest");


goog.require('goog.testing.PseudoRandom');
goog.require('relief.rpc.Command');
goog.require('relief.rpc.RPCService');
goog.require('relief.cache.Cache');
goog.require("net.bluemind.addressbook.api.AddressBooksClient");


goog.require('goog.async.Deferred');
goog.require('goog.testing.AsyncTestCase');
goog.require('goog.testing.jsunit');

var asyncTestCase = goog.testing.AsyncTestCase.createAndInstall();
var cache = null;
var rpc = null;
var book = null;

function setUp() {
	cache = new relief.cache.Cache();
	rpc = new relief.rpc.RPCService(cache);
	book = new net.bluemind.addressbook.api.AddressBooksClient(rpc, "http://localhost:8090");
}

function testCreate () {
	// FIXME
	return;
	
	var id = new goog.testing.PseudoRandom().random();
	asyncTestCase.waitForAsync('addressbook creation');

	var res = book.create("1234test5"+id, {"uid":"test","name":"test","owner":"anonymous","type":"addressbook",
		"defaultContainer":true});
	
	res.addCallback(function(e) {
		asyncTestCase.continueTesting();
	}, null);
		
	res.addErrback(function(e) {
		fail("error during containers creation");
		asyncTestCase.continueTesting();
	});
};

function testList () {
	// FIXME
	return;
	
	asyncTestCase.waitForAsync('list addressbooks');

	var res = book.all();
	
	res.addCallback(function(e) {
		asyncTestCase.continueTesting();
	}, null);
		
	res.addErrback(function(e) {
		fail("error during containers listing");
		asyncTestCase.continueTesting();
	});
};

function testSubscribe() {
	// FIXME
	return;
	
	asyncTestCase.waitForAsync('list addressbooks');

	var res = book.subscribe("1234test5");
	
	res.addCallback(function(e) {
		asyncTestCase.continueTesting();
	}, null);
		
	res.addErrback(function(e) {
		fail("error during containers subscription");
		asyncTestCase.continueTesting();
	});
	
}

function testSubxListSubscription() {
	// FIXME
	return;
	
	asyncTestCase.waitForAsync('list addressbooks subscriptions');

	var res = book.listSubscriptions();
	
	res.addCallback(function(e) {
		asyncTestCase.continueTesting();
	}, null);
		
	res.addErrback(function(e) {
		fail("error during containers subscription");
		asyncTestCase.continueTesting();
	});
}

function testUnsubscribe() {
	// FIXME
	return;
	
	asyncTestCase.waitForAsync('list addressbooks');

	var res = book.unsubscribe("1234test5");
	
	res.addCallback(function(e) {
		asyncTestCase.continueTesting();
	}, null);
		
	res.addErrback(function(e) {
		fail("error during containers subscription");
		asyncTestCase.continueTesting();
	});
	
}
