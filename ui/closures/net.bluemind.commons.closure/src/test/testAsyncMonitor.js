goog.provide("net.bluemind.concurrent.CrossWindowLockTest");


goog.require('goog.testing.PseudoRandom');
goog.require('net.bluemind.concurrent.AsyncMonitor');

goog.require('goog.async.Deferred');
goog.require('goog.testing.AsyncTestCase');
goog.require('goog.testing.jsunit');
goog.require('goog.async.Delay');
var asyncTestCase = goog.testing.AsyncTestCase.createAndInstall();


function testHeartbeat() {
  var m = new net.bluemind.concurrent.AsyncMonitor('testHeartbeat');
  m.start();
  asyncTestCase.waitForAsync('HeartBeat');
  new goog.async.Delay(function() {
    m.heartbeat();
  }, 2000);
  new goog.async.Delay(function() {
    asyncTestCase.continueTesting();        
    assertTrue(m.isAlive());
  }, 4000);

}

function testNaturalDeath() {
  var m = new net.bluemind.concurrent.AsyncMonitor('testNaturalDeath');
  m.start();
  asyncTestCase.waitForAsync('Death');
  new goog.async.Delay(function() {
    asyncTestCase.continueTesting();        
    assertFalse(m.isAlive());
  }, 4000);

}


function testKill() {
  var m = new net.bluemind.concurrent.AsyncMonitor('testKill');
  m.start();
  var killed = false;
  m.kill();
  try {
    m.heartbeat();
  } catch (e) {
    killed = true;
  }
  assertTrue(killed);
}
