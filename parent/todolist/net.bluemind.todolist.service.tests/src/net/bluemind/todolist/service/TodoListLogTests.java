/* BEGIN LICENSE
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
package net.bluemind.todolist.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.Test;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.auditlogs.AuditLogEntry;
import net.bluemind.core.container.model.ChangeLogEntry.Type;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.vertx.VertxEventChecker;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.VTodo;
import net.bluemind.todolist.hook.TodoListHookAddress;

public class TodoListLogTests extends AbstractServiceTests {

	private static final String AUDIT_LOG_DATASTREAM_PREFIX = "audit_log_";
	private final String DATASTREAM_NAME = AUDIT_LOG_DATASTREAM_PREFIX + domainUid;

	@Test
	public void testCreate() throws ServerFault, ElasticsearchException, IOException {

		VertxEventChecker<JsonObject> createdMessageChecker = new VertxEventChecker<>(TodoListHookAddress.CREATED);

		VTodo todo = defaultVTodo();
		String uid = "test_" + System.nanoTime();

		getService(defaultSecurityContext).create(uid, todo);

		ESearchActivator.refreshIndex(DATASTREAM_NAME);
		ElasticsearchClient esClient = ESearchActivator.getClient();

		Message<JsonObject> message = createdMessageChecker.shouldSuccess();
		assertNotNull(message);

		Awaitility.await().atMost(3, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> response = esClient.search(
					s -> s.index(DATASTREAM_NAME).query(q -> q.bool(b -> b
							.must(TermQuery.of(t -> t.field("container.uid").value(container.uid))._toQuery())
							.must(TermQuery.of(t -> t.field("logtype").value(container.type))._toQuery())
							.must(TermQuery.of(t -> t.field("action").value(Type.Created.toString()))._toQuery()))),
					AuditLogEntry.class);
			return 1L == response.hits().total().value();
		});
	}

	@Test
	public void testUpdate() throws ServerFault, ElasticsearchException, IOException {

		VTodo todo = defaultVTodo();
		String uid = "test_" + System.nanoTime();
		getService(defaultSecurityContext).create(uid, todo);

		todo.summary = "Coucou";

		getService(defaultSecurityContext).update(uid, todo);
		ESearchActivator.refreshIndex(DATASTREAM_NAME);

		ElasticsearchClient esClient = ESearchActivator.getClient();

		Awaitility.await().atMost(3, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> response = esClient.search(
					s -> s.index(DATASTREAM_NAME).query(q -> q.bool(b -> b
							.must(TermQuery.of(t -> t.field("container.uid").value(container.uid))._toQuery())
							.must(TermQuery.of(t -> t.field("logtype").value(container.type))._toQuery())
							.must(TermQuery.of(t -> t.field("action").value(Type.Created.toString()))._toQuery()))),
					AuditLogEntry.class);
			return 1L == response.hits().total().value();
		});

		SearchResponse<AuditLogEntry> response = esClient.search(
				s -> s.index(DATASTREAM_NAME)
						.query(q -> q.bool(b -> b
								.must(TermQuery.of(t -> t.field("container.uid").value(container.uid))._toQuery())
								.must(TermQuery.of(t -> t.field("logtype").value(container.type))._toQuery())
								.must(TermQuery.of(t -> t.field("action").value(Type.Created.toString()))._toQuery()))),
				AuditLogEntry.class);

		assertEquals(1L, response.hits().total().value());

		response = esClient.search(
				s -> s.index(DATASTREAM_NAME)
						.query(q -> q.bool(b -> b
								.must(TermQuery.of(t -> t.field("container.uid").value(container.uid))._toQuery())
								.must(TermQuery.of(t -> t.field("logtype").value(container.type))._toQuery())
								.must(TermQuery.of(t -> t.field("action").value(Type.Updated.toString()))._toQuery()))),
				AuditLogEntry.class);
		assertEquals(1L, response.hits().total().value());
	}

	@Test
	public void testDelete() throws ServerFault, ElasticsearchException, IOException {

		VertxEventChecker<JsonObject> deletedMessageChecker = new VertxEventChecker<>(TodoListHookAddress.DELETED);

		VTodo todo = defaultVTodo();

		String uid = "test_" + System.nanoTime();
		getService(defaultSecurityContext).create(uid, todo);

		getService(defaultSecurityContext).delete(uid);

		ItemValue<VTodo> vtodo = getService(defaultSecurityContext).getComplete(uid);
		assertNull(vtodo);

		Message<JsonObject> message = deletedMessageChecker.shouldSuccess();
		assertNotNull(message);
		ESearchActivator.refreshIndex(DATASTREAM_NAME);

		ElasticsearchClient esClient = ESearchActivator.getClient();
		Awaitility.await().atMost(3, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
					.index(DATASTREAM_NAME) //
					.query(q -> q.bool(b -> b
							.must(TermQuery.of(t -> t.field("container.uid").value(container.uid))._toQuery())
							.must(TermQuery.of(t -> t.field("logtype").value(container.type))._toQuery())
							.must(TermQuery.of(t -> t.field("action").value(Type.Created.toString()))._toQuery()))),
					AuditLogEntry.class);
			return 1L == response.hits().total().value();
		});

		Awaitility.await().atMost(3, TimeUnit.SECONDS).until(() -> {
			SearchResponse<AuditLogEntry> response = esClient.search(s -> s //
					.index(DATASTREAM_NAME) //
					.query(q -> q.bool(b -> b
							.must(TermQuery.of(t -> t.field("container.uid").value(container.uid))._toQuery())
							.must(TermQuery.of(t -> t.field("logtype").value(container.type))._toQuery())
							.must(TermQuery.of(t -> t.field("action").value(Type.Deleted.toString()))._toQuery()))),
					AuditLogEntry.class);
			return 1L == response.hits().total().value();
		});

		SearchResponse<AuditLogEntry> response = esClient.search(
				s -> s.index(DATASTREAM_NAME)
						.query(q -> q.bool(b -> b
								.must(TermQuery.of(t -> t.field("container.uid").value(container.uid))._toQuery())
								.must(TermQuery.of(t -> t.field("logtype").value(container.type))._toQuery())
								.must(TermQuery.of(t -> t.field("action").value(Type.Created.toString()))._toQuery()))),
				AuditLogEntry.class);
		assertEquals(1L, response.hits().total().value());

		response = esClient.search(
				s -> s.index(DATASTREAM_NAME)
						.query(q -> q.bool(b -> b
								.must(TermQuery.of(t -> t.field("container.uid").value(container.uid))._toQuery())
								.must(TermQuery.of(t -> t.field("logtype").value(container.type))._toQuery())
								.must(TermQuery.of(t -> t.field("action").value(Type.Deleted.toString()))._toQuery()))),
				AuditLogEntry.class);
		assertEquals(1L, response.hits().total().value());
	}

	@Override
	protected ITodoList getService(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(ITodoList.class, container.uid);
	}

}
