package net.bluemind.core.backup.continuous.restore.domains;

import org.slf4j.event.Level;
import org.slf4j.helpers.MessageFormatter;

import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.task.service.IServerTaskMonitor;

public class RestoreLogger {

	public enum Operation {
		CREATE, UPDATE, SET, DELETE, CREATE_PARENT, DELETE_CHILD, APPLY_MAILBOX, FILTER, SKIP, SEPPUKU
	}

	private final IServerTaskMonitor monitor;

	public RestoreLogger(IServerTaskMonitor monitor) {
		this.monitor = monitor;
	}

	public IServerTaskMonitor monitor() {
		return monitor;
	}

	public RestoreLogger subWork(double work) {
		return new RestoreLogger(monitor.subWork(work));
	}

	public void create(String type, RecordKey key) {
		create(type, null, key);
	}

	public void create(String type, String kind, RecordKey key) {
		log(Operation.CREATE, type, kind, key, Level.INFO);
	}

	public void update(String type, RecordKey key) {
		update(type, null, key);
	}

	public void update(String type, String kind, RecordKey key) {
		log(Operation.UPDATE, type, kind, key, Level.INFO);
	}

	public void set(String type, RecordKey key) {
		set(type, null, key);
	}

	public void set(String type, String kind, RecordKey key) {
		log(Operation.SET, type, kind, key, Level.INFO);
	}

	public void delete(String type, RecordKey key) {
		delete(type, null, key);
	}

	public void delete(String type, String kind, RecordKey key) {
		log(Operation.DELETE, type, kind, key, Level.INFO);
	}

	public void createParent(String type, RecordKey key, String uid) {
		monitor.log("op:" + Operation.CREATE_PARENT + ", type:" + type + ",  key:" + key + ", uid: " + uid);
	}

	public void deleteChild(String type, RecordKey key, String uid) {
		monitor.log("op:" + Operation.DELETE_CHILD + ", type:" + type + ",  key:" + key + ", uid: " + uid);
	}

	public void applyMailbox(String type, RecordKey key) {
		log(Operation.APPLY_MAILBOX, type, null, key, Level.INFO);
	}

	public void filter(String type, RecordKey key) {
		delete(type, null, key);
	}

	public void filter(String type, String kind, RecordKey key) {
		log(Operation.FILTER, type, kind, key, Level.INFO);
	}

	public void skip(String type, RecordKey key, String payload) {
		skip(type, null, key, payload);
	}

	public void skip(String type, String kind, RecordKey key, String payload) {
		String t = (kind == null) ? type : type + "." + kind;
		monitor.log("op:skip, type:" + t + ",  key:" + key + ", payload:" + payload, Level.WARN);
	}

	public void failure(String type, RecordKey key, String payload, Throwable t) {
		monitor.log("op:failure, type:" + type + ",  key:" + key + ", payload:" + payload, t);
	}

	public void seppuku(String type, RecordKey key) {
		log(Operation.SEPPUKU, type, null, key, Level.INFO);
	}

	public void debug(String s, Object... args) {
		monitor.log(
				"[" + Thread.currentThread().getName() + "] - " + MessageFormatter.arrayFormat(s, args).getMessage());
	}

	private void log(Operation op, String type, String kind, RecordKey key, Level level) {
		String t = (kind == null) ? type : type + "." + kind;
		monitor.log("op:" + op + ", type:" + t + ",  key:" + key, level);
	}
}
