goog.provide('net.bluemind.sync.SyncServiceTest');
goog.provide('net.bluemind.test.sync.DummySyncService');

goog.require('net.bluemind.sync.SyncService');
goog.require('goog.testing.PseudoRandom');

goog.require('goog.async.Deferred');
goog.require('goog.testing.AsyncTestCase');
goog.require('goog.testing.jsunit');
goog.require('goog.async.Delay');
goog.require('goog.async.Deferred');

var asyncTestCase = goog.testing.AsyncTestCase.createAndInstall();


net.bluemind.test.sync.SuccessSyncService = function() {};
goog.inherits(net.bluemind.test.sync.SuccessSyncService, net.bluemind.sync.SyncService);
net.bluemind.test.sync.SuccessSyncService.prototype.getName = function() {return 'SuccessSyncService';};
net.bluemind.test.sync.SuccessSyncService.prototype.syncInternal = function() {return goog.async.succeed();};

net.bluemind.test.sync.FailSyncService = function() {};
goog.inherits(net.bluemind.test.sync.FailtSyncService, net.bluemind.sync.SyncService);
net.bluemind.test.sync.FailSyncService.prototype.getName = function() {return 'FailSyncService';};
net.bluemind.test.sync.FailSyncService.prototype.syncInternal = function() {return goog.async.fail();};

net.bluemind.test.sync.LongSyncService = function() {};
goog.inherits(net.bluemind.test.sync.LongSyncService, net.bluemind.sync.SyncService);
net.bluemind.test.sync.LongSyncService.prototype.getName = function() {return 'FailSyncService';};
net.bluemind.test.sync.LongSyncService.prototype.syncInternal = function() {
  var d = new goog.async.Deferred();
  new goog.async.Delay(d.callback, 3000, d).start();
  return d;
};

function testSync() {
  var ok = new net.bluemind.test.sync.SuccessSyncService();
  var error =  new net.bluemind.test.sync.FailSyncService();
  asyncTestCase.waitForAsync('success sync');
  ok.sync.addCallback(function() {
    asyncTestCase.continueTesting();    
    assertTrue(true);
  }).addErrback(function() {
    asyncTestCase.continueTesting();    
    assertTrue(false);
  });
  asyncTestCase.waitForAsync('error sync');  
  err.sync.addCallback(function() {
    asyncTestCase.continueTesting();    
    assertTrue(false);
  }).addErrback(function() {
    asyncTestCase.continueTesting();    
    assertTrue(true);
  });

};


function testSyncLock() {
  var s1 = new net.bluemind.test.sync.LongSyncService();
  asyncTestCase.waitForAsync('error sync');    
  s1.sync.addCallback(function() {
    asyncTestCase.continueTesting();    
    assertTrue(true);
  }).addErrback(function() {
    asyncTestCase.continueTesting();    
    assertTrue(false);
  });
  // Emulate lock from another window
  var s2 = new net.bluemind.test.sync.LongSyncService();
  asyncTestCase.waitForAsync('error sync');    
  s2.sync.addCallback(function(e) {
    asyncTestCase.continueTesting();    
    assertTrue(false);
  }).addErrback(function() {
    asyncTestCase.continueTesting(e);    
    assertTrue(true);
  });
  // Emulate a sync lasting more than interval  
  s1.sync.addCallback(function(e) {
    asyncTestCase.continueTesting();    
    assertTrue(false);
  }).addErrback(function() {
    asyncTestCase.continueTesting(e);    
    assertTrue(true);
  });

}

