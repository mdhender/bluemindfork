/**
 * Created by kyawtun on 16/1/14.
 */

goog.require('ydn.async.Deferred');

goog.exportSymbol('ydn.msg.Pipe', ydn.msg.Pipe);
goog.exportSymbol('ydn.msg.initPipe', ydn.msg.initPipe);
goog.exportSymbol('ydn.msg.getChannel', ydn.msg.getChannel);
goog.exportProperty(ydn.msg.Pipe.prototype, 'listen',
    ydn.msg.Pipe.prototype.listen);
goog.exportProperty(ydn.msg.Pipe.prototype, 'getChannel',
    ydn.msg.Pipe.prototype.getChannel);
goog.exportProperty(ydn.msg.Channel.prototype, 'send',
    ydn.msg.Channel.prototype.send);


goog.exportSymbol('goog.async.Deferred', goog.async.Deferred); // no need ?
goog.exportProperty(goog.async.Deferred.prototype, 'addCallback',
    goog.async.Deferred.prototype.addCallback);
goog.exportProperty(goog.async.Deferred.prototype, 'addErrback',
    goog.async.Deferred.prototype.addErrback);
goog.exportProperty(goog.async.Deferred.prototype, 'addBoth',
    goog.async.Deferred.prototype.addBoth);
goog.exportProperty(goog.async.Deferred.prototype, 'addCallbacks',
    goog.async.Deferred.prototype.addCallbacks);
goog.exportProperty(ydn.async.Deferred.prototype, 'addProgback',
    ydn.async.Deferred.prototype.addProgback);
