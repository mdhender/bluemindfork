/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.pop3.driver;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import net.bluemind.authentication.api.AuthUser;
import net.bluemind.authentication.api.IAuthenticationPromise;
import net.bluemind.backend.mail.api.IMailboxFoldersPromise;
import net.bluemind.backend.mail.api.IMailboxItemsPromise;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecordsPromise;
import net.bluemind.core.api.Stream;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.lib.vertx.utils.MmapWriteStream;
import net.bluemind.pop3.endpoint.MailItemData;
import net.bluemind.pop3.endpoint.MailboxConnection;
import net.bluemind.pop3.endpoint.Pop3Context;
import net.bluemind.pop3.endpoint.Retr;
import net.bluemind.pop3.endpoint.Stat;
import net.bluemind.pop3.endpoint.TargetStream;
import net.bluemind.pop3.endpoint.TopItemStream;

public class CoreConnection implements MailboxConnection {

	private static final Logger logger = LoggerFactory.getLogger(CoreConnection.class);
	private static final Path TMP = Paths.get(System.getProperty("java.io.tmpdir"));

	private IServiceProvider prov;
	private AuthUser me;

	private Supplier<CompletableFuture<ItemValue<MailboxFolder>>> inboxRef = Suppliers
			.memoize(CoreConnection.this::inbox);

	public CoreConnection(IServiceProvider prov, AuthUser authUser) {
		this.prov = prov;
		this.me = authUser;
	}

	@Override
	public void close() {
		prov.instance(IAuthenticationPromise.class).logout()
				.thenAccept(logout -> logger.info("{} disconnected", me.value.defaultEmailAddress()));
	}

	@Override
	public CompletableFuture<Stat> stat() {
		CompletableFuture<Stat> completableFutureStat = new CompletableFuture<>();
		Stat stat = new Stat();
		inboxRef.get().thenAccept(inbx -> {
			IDbMailboxRecordsPromise recApi = prov.instance(IDbMailboxRecordsPromise.class, inbx.uid);
			recApi.count(ItemFlagFilter.create().mustNot(ItemFlag.Deleted)).thenCompose(count -> {
				stat.setMsgCount(count.total);
				return recApi.weight();
			}).thenAccept(weight -> {
				stat.setSizeInBytes(weight.total);
				completableFutureStat.complete(stat);
			});
		}).exceptionally(ex -> {
			completableFutureStat.completeExceptionally(ex);
			return null;
		});

		return completableFutureStat;
	}

	private CompletableFuture<ItemValue<MailboxFolder>> inbox() {
		IMailboxFoldersPromise foldersApi = prov.instance(IMailboxFoldersPromise.class, me.domainUid,
				"user." + me.value.login.replace('.', '^'));
		return foldersApi.byName("INBOX");
	}

	@Override
	public CompletableFuture<Void> list(Pop3Context ctx, WriteStream<ListItem> output) {
		CompletableFuture<Void> ret = new CompletableFuture<>();
		return ctx.getMap().thenCompose(map -> {
			Iterator<Entry<Integer, MailItemData>> recordIds = map.entrySet().iterator();
			fetch(ret, recordIds, output);
			return ret;
		}).exceptionally(ex -> {
			logger.error(ex.getMessage());
			ret.completeExceptionally(ex);
			return null;
		});
	}

	@Override
	public CompletableFuture<Void> listUnique(Pop3Context ctx, Integer id) {
		CompletableFuture<Void> ret = new CompletableFuture<>();
		return ctx.getMap().thenCompose(map -> {
			if (map.get(id) == null) {
				ctx.write("-ERR no such message, only " + map.size() + " messages in maildrop\r\n");
				ret.completeExceptionally(new Exception("Cannot find mail with id " + id + " in maildrop"));
				return ret;
			} else {
				ctx.write("+OK " + id + " " + map.get(id).getMsgSize() + "\r\n");
				ret.complete(null);
				return ret;
			}
		}).exceptionally(ex -> {
			ret.completeExceptionally(ex);
			return null;
		});
	}

	@Override
	public CompletableFuture<Void> uidl(Pop3Context ctx, WriteStream<UidlItem> output) {
		CompletableFuture<Void> ret = new CompletableFuture<>();
		return ctx.getMap().thenCompose(map -> {
			Iterator<Entry<Integer, MailItemData>> iterator = map.entrySet().iterator();
			fetchUidl(ret, iterator, output);
			return ret;
		}).exceptionally(ex -> {
			ret.completeExceptionally(ex);
			return null;
		});
	}

	@Override
	public CompletableFuture<Void> uidlUnique(Pop3Context ctx, Integer id) {
		CompletableFuture<Void> ret = new CompletableFuture<>();
		return ctx.getMap().thenCompose(map -> {
			if (map.get(id) == null) {
				ctx.write("-ERR no such message, only " + map.size() + " messages in maildrop\r\n");
				ret.completeExceptionally(new Exception("Cannot find mail with id " + id + " in maildrop"));
				return ret;
			} else {
				ctx.write("+OK " + id + " " + map.get(id).getBodyMsgId() + "\r\n");
				ret.complete(null);
				return ret;
			}
		}).exceptionally(ex -> {
			ret.completeExceptionally(ex);
			return null;
		});
	}

	public CompletableFuture<ConcurrentMap<Integer, MailItemData>> mapPopIdtoMailId() {

		CompletableFuture<ConcurrentMap<Integer, MailItemData>> result = new CompletableFuture<>();

		inboxRef.get().thenCompose(ibx -> prov.instance(IMailboxItemsPromise.class, ibx.uid)
				.filteredChangesetById(0L, ItemFlagFilter.create().mustNot(ItemFlag.Deleted)).thenCompose(cs -> {
					IMailboxItemsPromise recApi = prov.instance(IMailboxItemsPromise.class, ibx.uid);
					Iterator<ItemVersion> recordIds = Iterables.concat(cs.created, cs.updated).iterator();

					List<CompletableFuture<ItemValue<MailboxItem>>> list = ImmutableList.copyOf(recordIds).stream()
							.map(item -> recApi.getCompleteById(item.id)).collect(Collectors.toList());

					return CompletableFuture.allOf(list.stream().toArray(CompletableFuture[]::new)).thenAccept(v -> {
						List<MailItemData> listMails = list.stream().map(CompletableFuture::join)
								.map(i -> new MailItemData(i.internalId, i.value.body.guid, i.value.body.size))
								.collect(Collectors.toList());
						ConcurrentMap<Integer, MailItemData> map = IntStream.range(0, listMails.size()).boxed()
								.collect(Collectors.toConcurrentMap(it -> it + 1, listMails::get));
						result.complete(map);
					}).exceptionally(ex -> {
						result.completeExceptionally(ex);
						return null;
					});
				}));
		return result;
	}

	private void fetch(CompletableFuture<Void> ret, Iterator<Entry<Integer, MailItemData>> recordIds,
			WriteStream<ListItem> output) {
		while (recordIds.hasNext()) {
			Entry<Integer, MailItemData> entry = recordIds.next();
			ListItem li = new ListItem(entry.getKey(), entry.getValue().getMsgSize());
			output.write(li);
			if (output.writeQueueFull()) {
				output.drainHandler(v -> fetch(ret, recordIds, output));
				return;
			}
		}
		output.end();
		ret.complete(null);
	}

	private void fetchUidl(CompletableFuture<Void> ret, Iterator<Entry<Integer, MailItemData>> recordIds,
			WriteStream<UidlItem> output) {

		while (recordIds.hasNext()) {
			Entry<Integer, MailItemData> next = recordIds.next();
			UidlItem ui = new UidlItem(next.getValue().getBodyMsgId(), next.getKey());
			output.write(ui);
			if (output.writeQueueFull()) {
				output.drainHandler(v -> fetchUidl(ret, recordIds, output));
				return;
			}
		}
		output.end();
		ret.complete(null);
	}

	public CompletableFuture<Retr> retr(Pop3Context ctx, String param) {
		int msgId;
		try {
			msgId = Integer.parseInt(param);
		} catch (NumberFormatException e) {
			return null;
		}
		CompletableFuture<Retr> cf = new CompletableFuture<>();

		inboxRef.get().thenCompose(inbox -> ctx.getMap().thenCompose(map -> {
			MailItemData details = map.get(msgId);
			if (details == null) {
				cf.complete(null);
				return null;
			}
			return prov.instance(IMailboxItemsPromise.class, inbox.uid).getCompleteById(details.getItemId())
					.thenCompose(mail -> {
						if (mail == null) {
							cf.complete(null);
							return null;
						}
						return prov.instance(IMailboxItemsPromise.class, inbox.uid).fetchComplete(mail.value.imapUid)
								.thenAccept(s -> {
									CompletableFuture<ByteBuf> completableFuture = readMmap(s, mail.value.body.size);
									cf.complete(new Retr(mail.value.body.size, completableFuture));
								});
					});
		}));
		return cf;
	}

	@Override
	public CompletableFuture<Boolean> delete(Pop3Context ctx, List<Long> ids) {

		return inboxRef.get().thenCompose(inbox -> {
			List<List<Long>> partition = Lists.partition(ids, 1_000);
			List<CompletableFuture<Void>> completableFutures = new ArrayList<>();

			partition.stream().forEach(part -> {
				logger.debug("{} - try to delete {} mails", ctx.getLogin(), part.size());
				completableFutures.add(prov.instance(IMailboxItemsPromise.class, inbox.uid).multipleDeleteById(part));
			});
			return CompletableFuture.allOf(completableFutures.stream().toArray(CompletableFuture[]::new))
					.thenApply(res -> {
						logger.debug("{} - {} mails succesfully deleted", me.value.defaultEmailAddress(), ids.size());
						return true;
					}).exceptionally(ex -> {
						ex.printStackTrace();
						logger.error(ex.getMessage());
						return false;
					});
		});
	}

	@Override
	public CompletableFuture<Void> top(TopItemStream stream, String strMessageId, final String strBodyLines,
			Pop3Context ctx) {
		int messageId;
		int bodyLines;
		CompletableFuture<Void> cf = new CompletableFuture<>();
		try {
			messageId = Integer.parseInt(strMessageId);
			bodyLines = Integer.parseInt(strBodyLines);
		} catch (NumberFormatException e) {
			logger.warn("{} - wrong arguments for TOP command {} {}", me.value.defaultEmailAddress(), strMessageId,
					strBodyLines);
			stream.write("-ERR no such message");
			cf.complete(null);
			return cf;
		}
		return ctx.getMap().thenCompose(map -> {
			MailItemData detail = map.get(messageId);
			if (detail == null) {
				stream.write("-ERR no such message");
				return null;
			} else {
				return inboxRef.get().thenCompose(inbox -> prov.instance(IMailboxItemsPromise.class, inbox.uid)
						.getCompleteById(detail.getItemId())
						.thenCompose(mail -> read(
								prov.instance(IMailboxItemsPromise.class, inbox.uid).fetchComplete(mail.value.imapUid))
								.thenCompose(buf -> {
									stream.write("+OK");
									buf.markReaderIndex();
									int headerEndIndex = ByteBufUtil
											.indexOf(Unpooled.wrappedBuffer("\r\n\r\n".getBytes()), buf);
									buf.readerIndex(headerEndIndex + 4);
									int nextLine = 0;
									for (int i = 0; i < bodyLines; i++) {
										nextLine = ByteBufUtil.indexOf(Unpooled.wrappedBuffer("\r\n".getBytes()), buf);
										if (nextLine < 0) {
											break;
										}
										buf.readerIndex(nextLine + 2);
									}
									Integer finalOffset = buf.readerIndex();
									buf.resetReaderIndex();
									return ctx.write(buf.slice(0, finalOffset));
								})));
			}
		});
	}

	private static CompletableFuture<ByteBuf> readMmap(Stream s, int sizeHint) {
		try {
			MmapWriteStream out = new MmapWriteStream(TMP, sizeHint);
			ReadStream<Buffer> toRead = VertxStream.read(s);
			toRead.pipeTo(out);
			toRead.resume();
			return out.mmap();
		} catch (IOException e) {
			CompletableFuture<ByteBuf> ex = new CompletableFuture<>();
			ex.completeExceptionally(e);
			return ex;
		}
	}

	private static CompletableFuture<ByteBuf> read(CompletableFuture<Stream> stream) {
		return stream.thenCompose(s -> {
			CompletableFuture<ByteBuf> ret = new CompletableFuture<>();
			TargetStream out = new TargetStream();
			ReadStream<Buffer> toRead = VertxStream.read(s);
			toRead.pipeTo(out, ar -> {
				if (ar.succeeded()) {
					ret.complete(out.out);
				} else {
					ret.completeExceptionally(ar.cause());
				}
			});
			toRead.resume();
			return ret;
		});
	}
}
