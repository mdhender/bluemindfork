goog.provide("net.bluemind.concurrent.CrossWindowLockTest");


goog.require('goog.testing.PseudoRandom');
goog.require('net.bluemind.concurrent.CrossWindowLock');

goog.require('goog.async.Deferred');
goog.require('goog.testing.AsyncTestCase');
goog.require('goog.testing.jsunit');
goog.require('goog.async.Delay');
var asyncTestCase = goog.testing.AsyncTestCase.createAndInstall();


function testLock () {
  var l1 = new net.bluemind.concurrent.CrossWindowLock('testLock');
  var l2 = new net.bluemind.concurrent.CrossWindowLock('testLock');
  assertTrue(l1.tryLock());
  assertFalse(l2.tryLock()); 
  l1.unlock();
}

function testUnlock() {
  var l1 = new net.bluemind.concurrent.CrossWindowLock('testUnlock');
  var l2 = new net.bluemind.concurrent.CrossWindowLock('testUnlock');
  assertTrue(l1.tryLock());
  l1.unlock();
  assertTrue(l2.tryLock()); 
  l2.unlock();
}

function testExpire() {
  var l1 = new net.bluemind.concurrent.CrossWindowLock('testExpire');
  var l2 = new net.bluemind.concurrent.CrossWindowLock('testExpire');
  assertTrue(l1.tryLock());
  var d = new goog.async.Delay(function() {
    asyncTestCase.continueTesting();
    assertTrue(l2.tryLock());
  }, 350); 
  asyncTestCase.waitForAsync('Async lock');
  d.start();
}
