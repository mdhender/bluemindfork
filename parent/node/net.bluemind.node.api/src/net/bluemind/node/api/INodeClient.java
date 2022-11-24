/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.node.api;

import java.io.InputStream;
import java.util.List;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.node.shared.ActiveExecQuery;
import net.bluemind.node.shared.ExecDescriptor;
import net.bluemind.node.shared.ExecRequest;

/**
 * local or remote BM {@link Host} manipulation API.
 * 
 * 
 */
public interface INodeClient {

	/**
	 * Checks we can connect to a node.
	 * 
	 * @throws ServerFault if the connection does not work
	 */
	void ping() throws ServerFault;

	/**
	 * Fetches a file content into ram.
	 * 
	 * If the file does not exist, a zero-length byte array is returned.
	 * 
	 * @param path
	 * @return
	 * @throws ServerFault
	 */
	byte[] read(String path) throws ServerFault;

	/**
	 * Check if path exists
	 * 
	 * @param path
	 * @return
	 */
	boolean exists(String path);

	/**
	 * Opens a stream to a (maybe) remote file. Use this one when you can't predict
	 * the size of the file you want to read.
	 * 
	 * WARNING: If you forget to close this stream, your BM Core will die from out
	 * of memory, too many open files, etc sooner or later.
	 * 
	 * @param path
	 * @return
	 * @throws ServerFault
	 */
	InputStream openStream(String path) throws ServerFault;

	/**
	 * (Over)writes the file at path with content.
	 * 
	 * Content is closed by this method. You don't need to do it in your code.
	 * 
	 * @param path
	 * @param content
	 * @throws ServerFault
	 */
	void writeFile(String path, InputStream content) throws ServerFault;

	/**
	 * Starts a command in backend and returns a ref to track its progress. Task
	 * return a String list as output.
	 * 
	 * @param cmd
	 * @return
	 * @throws ServerFault
	 */
	TaskRef executeCommand(String cmd) throws ServerFault;

	/**
	 * Runs a command. The optional {@link ExecRequest#group} and
	 * {@link ExecRequest#name} can be used to provide a stable name to tasks
	 * 
	 * 
	 * 
	 * @param tsk
	 * @return
	 * @throws ServerFault
	 */
	TaskRef executeCommand(ExecRequest tsk) throws ServerFault;

	void asyncExecute(ExecRequest req, ProcessHandler ph);

	/**
	 * Returns a list of running process matching the given query object.
	 * {@link ActiveExecQuery#group} and {@link ActiveExecQuery#name} might be null
	 * for wider matches.
	 * 
	 * @return
	 */
	List<ExecDescriptor> getActiveExecutions(ActiveExecQuery query);

	void interrupt(ExecDescriptor runningTask);

	/**
	 * Starts a command in backend and returns a ref to track its progress No
	 * output, receive "Done" when command finish.
	 * 
	 * @param cmd
	 * @return
	 * @throws ServerFault
	 */
	TaskRef executeCommandNoOut(String cmd) throws ServerFault;

	/**
	 * Tracks the progress of a task
	 * 
	 * @param task
	 * @return
	 * @throws ServerFault
	 */
	TaskStatus getExecutionStatus(TaskRef task) throws ServerFault;

	/**
	 * List files from path matching extension. Task return a list of
	 * FileDescription as output
	 * 
	 * @param path
	 * @param extensionPattern
	 * @return empty list when the path does not exist
	 * @throws ServerFault
	 */
	List<FileDescription> listFiles(String path, String extensionPattern) throws ServerFault;

	/**
	 * List files from path. Task return a FileDescription list as output.
	 * 
	 * @param path
	 * @param extensionPattern
	 * @return empty list when the path does not exist
	 * @throws ServerFault
	 */
	List<FileDescription> listFiles(String path) throws ServerFault;

	/**
	 * Delete a file
	 * 
	 * @param path
	 * @throws ServerFault
	 */
	void deleteFile(String path) throws ServerFault;

	/*
	 * Moves a file from a path, to another
	 * 
	 * @param origin: Original path
	 * 
	 * @param destination: Destination path
	 * 
	 * @throws ServerFault
	 */
	void moveFile(String origin, String destination) throws ServerFault;
}
