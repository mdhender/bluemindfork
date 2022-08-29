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
package net.bluemind.dataprotect.persistence;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import net.bluemind.core.api.VersionInfo;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.GenerationStatus;
import net.bluemind.dataprotect.api.PartGeneration;
import net.bluemind.dataprotect.persistence.internal.PartGenerationColumns;

public class DataProtectGenerationStore extends JdbcAbstractStore {

	private static final String DP_PARTGEN_SELECT = "SELECT " + PartGenerationColumns.COLUMNS.names()
			+ " FROM t_dp_partgeneration ";

	private static final String DP_PARTGEN_INSERT = "INSERT INTO t_dp_partgeneration ("
			+ PartGenerationColumns.COLUMNS.names() + ") values (" + PartGenerationColumns.COLUMNS.values() + ")";

	private static final EntityPopulator<PartGeneration> COMPLETE_POPULATOR = (rs, index, value) -> {

		value.id = rs.getInt(index++);
		value.generationId = rs.getInt(index++);
		value.begin = Date.from(rs.getTimestamp(index++).toInstant());
		Timestamp ts = rs.getTimestamp(index++);
		value.end = ts != null ? Date.from(ts.toInstant()) : null;
		value.size = rs.getLong(index++);
		value.valid = GenerationStatus.valueOf(rs.getString(index++));
		value.tag = rs.getString(index++);
		value.server = rs.getString(index++);
		value.datatype = rs.getString(index++);
		return index;
	};

	public static Creator<PartGeneration> CREATOR = con -> new PartGeneration();

	public DataProtectGenerationStore(DataSource dataSource) {
		super(dataSource);
	}

	public DataProtectGeneration newGeneration(VersionInfo version) throws SQLException {
		Date now = new Date(System.currentTimeMillis());
		String q = "INSERT INTO t_dp_backup (starttime, version) values (?, ?)";
		int id = insertWithSerial(q, new Object[] { new java.sql.Timestamp(now.getTime()), version.toString() });
		DataProtectGeneration dpg = new DataProtectGeneration();
		dpg.blueMind = version;
		dpg.id = id;
		dpg.protectionTime = now;
		new GenerationWriter(dpg.id).write(dpg);
		return dpg;
	}

	public DataProtectGeneration newGeneration(int id, Date startTime, VersionInfo vi) throws SQLException {
		String q = "INSERT INTO t_dp_backup (id, starttime, version) values (?, ?, ?)";
		insert(q, new Object[] { id, new java.sql.Timestamp(startTime.getTime()), vi.toString() });
		DataProtectGeneration dpg = new DataProtectGeneration();
		dpg.blueMind = vi;
		dpg.id = id;
		dpg.protectionTime = startTime;
		new GenerationWriter(dpg.id).write(dpg);
		return dpg;
	}

	public int newPart(final int genId, final String tag, final String serverAddress, String datatype)
			throws SQLException {

		final long nextId = nextSeqValue();

		insert(DP_PARTGEN_INSERT, null, (con, statement, index, currentRow, value) -> {

			statement.setLong(index++, nextId);
			statement.setLong(index++, genId);
			statement.setTimestamp(index++, Timestamp.from(Instant.now()));
			statement.setNull(index++, Types.TIMESTAMP);
			statement.setNull(index++, Types.INTEGER);
			statement.setString(index++, GenerationStatus.UNKNOWN.name());
			statement.setString(index++, tag);
			statement.setString(index++, serverAddress);
			statement.setString(index++, datatype);
			return index;
		});
		new GenerationWriter(genId).addPart(getPart(nextId));
		return (int) nextId;
	}

	private PartGeneration getPart(long nextId) throws SQLException {
		String query = DP_PARTGEN_SELECT + " WHERE id = ?";

		return unique(query, CREATOR, COMPLETE_POPULATOR, new Object[] { nextId });

	}

	public void updatePart(final PartGeneration part) throws SQLException {
		update("UPDATE t_dp_partgeneration set endtime=?, size_mb=?, valid=?::t_generation_status where id=?", null,
				(con, statement, index, currentRow, value) -> {
					part.validate();
					statement.setTimestamp(index++, Timestamp.from(part.end.toInstant()));
					statement.setLong(index++, part.size);
					statement.setString(index++, part.valid.name());
					statement.setInt(index++, part.id);
					return index;
				});
		new GenerationWriter(part.generationId).updatePart(part);
	}

	public void rewriteGenerations(List<DataProtectGeneration> generations) throws ServerFault {
		GenerationWriter.deleteOtherGenerations(generations);
		List<DataProtectGeneration> filteredGenerations = generations.stream() //
				.map(this::verifyGeneration) //
				.filter(gen -> {
					return gen.protectionTime != null;
				}) //
				.collect(Collectors.toList());
		doOrFail(() -> {
			delete("DELETE FROM t_dp_partgeneration", new Object[] {});
			delete("DELETE FROM t_dp_backup", new Object[] {});

			int seqMax = 1;
			List<PartGeneration> parts = new ArrayList<>();
			for (DataProtectGeneration gen : filteredGenerations) {
				try {
					GenerationWriter.deleteGenerationFile(gen.id);
					DataProtectGeneration ng = newGeneration(gen.id, gen.protectionTime, gen.blueMind);
					gen.id = ng.id;
					for (PartGeneration part : gen.parts) {
						part.generationId = ng.id;
						parts.add(part);
						new GenerationWriter(gen.id).addPart(part);
						seqMax = Math.max(seqMax, part.id);
					}
				} catch (Exception e) {
					logger.warn("Cannot write generation: {} ", gen.id, e);
				}

			}

			batchInsert(DP_PARTGEN_INSERT, parts, (con, statement, index, currentRow, part) -> {
				PartGeneration partGen = part;
				statement.setInt(index++, partGen.id);
				statement.setInt(index++, partGen.generationId);
				if (partGen.begin != null) {
					statement.setTimestamp(index++, Timestamp.from(partGen.begin.toInstant()));
				} else {
					statement.setNull(index++, Types.TIMESTAMP);
				}

				if (partGen.end != null) {
					statement.setTimestamp(index++, Timestamp.from(partGen.end.toInstant()));
				} else {
					statement.setNull(index++, Types.TIMESTAMP);
				}
				partGen.validate();
				statement.setLong(index++, partGen.size);
				statement.setString(index++, partGen.valid.name());
				statement.setString(index++, partGen.tag);
				statement.setString(index++, partGen.server);
				statement.setString(index++, partGen.datatype);
				return index;
			});

			String resetSeqQuery = "SELECT setval('partgeneration_id_seq', ? )";
			unique(resetSeqQuery, rs -> null, (rs, index, value) -> 0, new Object[] { seqMax });
			return null;
		});
	}

	private DataProtectGeneration verifyGeneration(DataProtectGeneration gen) {
		if (gen.protectionTime == null) {
			try {
				List<DataProtectGeneration> storedGen = getGenerations().stream().filter(g -> {
					return g.id == gen.id;
				}).collect(Collectors.toList());
				if (!storedGen.isEmpty()) {
					gen.protectionTime = storedGen.get(0).protectionTime;
				}
			} catch (SQLException e) {
				logger.warn("Cannot read protection time from generation {}", gen.id, e);
			}
		}
		return gen;
	}

	public List<PartGeneration> listParts(final String tag, final String serverAddress) throws SQLException {
		String query = DP_PARTGEN_SELECT + " WHERE server_adr = ? and tag = ? order by id asc";

		return select(query, CREATOR, COMPLETE_POPULATOR, new Object[] { serverAddress, tag });
	}

	public PartGeneration last(final String tag, final String serverAddress) throws SQLException {
		String query = DP_PARTGEN_SELECT
				+ " WHERE server_adr = ? and tag = ? and endtime is not null order by id desc limit 1";

		return unique(query, CREATOR, COMPLETE_POPULATOR, new Object[] { serverAddress, tag });
	}

	protected long nextSeqValue() throws SQLException {
		return unique("SELECT nextval('partgeneration_id_seq')", rs -> rs.getLong(1),
				Arrays.<EntityPopulator<Long>>asList(), new Object[] {});
	}

	public List<DataProtectGeneration> getGenerations() throws SQLException {
		String dpQ = "SELECT id, starttime, version FROM t_dp_backup";
		List<DataProtectGeneration> storedGens = select(dpQ, (Creator<DataProtectGeneration>) con -> {
			DataProtectGeneration ret = new DataProtectGeneration();
			ret.parts = new LinkedList<>();
			return ret;
		}, (rs, index, value) -> {
			value.id = rs.getInt(index++);
			value.protectionTime = Date.from(rs.getTimestamp(index++).toInstant());
			value.blueMind = VersionInfo.create(rs.getString(index++));
			return index;
		}, new Object[0]);
		Map<Integer, DataProtectGeneration> idx = new HashMap<Integer, DataProtectGeneration>();
		for (DataProtectGeneration dp : storedGens) {
			idx.put(dp.id, dp);
		}

		String query = DP_PARTGEN_SELECT + " order by backup_id, id asc";
		List<PartGeneration> parts = select(query, CREATOR, COMPLETE_POPULATOR, new Object[0]);
		List<DataProtectGeneration> dpg = new LinkedList<>();
		int lastId = 0;
		DataProtectGeneration cur = null;
		for (PartGeneration part : parts) {
			if (lastId == 0 || part.generationId != lastId) {
				cur = idx.get(part.generationId);
				dpg.add(cur);
				lastId = cur.id;
			}
			cur.parts.add(part);
		}
		return dpg;
	}
}
