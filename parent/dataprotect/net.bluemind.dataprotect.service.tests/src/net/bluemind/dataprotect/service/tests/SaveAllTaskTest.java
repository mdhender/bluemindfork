package net.bluemind.dataprotect.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.GenerationStatus;
import net.bluemind.dataprotect.api.PartGeneration;
import net.bluemind.dataprotect.service.internal.SaveAllTask;
import net.bluemind.dataprotect.service.internal.SaveAllTask.PartGenerationIndex;

public class SaveAllTaskTest {
	private static final DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

	@Test
	public void getLatestPartsGenerations_getLatest() throws ParseException {
		List<DataProtectGeneration> dpgs = new ArrayList<>();
		dpgs.add(getDataProtectGeneration(GenerationStatus.VALID, "2000-01-01"));
		dpgs.add(getDataProtectGeneration(GenerationStatus.VALID, "2020-01-01"));
		dpgs.add(getDataProtectGeneration(GenerationStatus.VALID, "2010-01-01"));

		PartGenerationIndex latest = new SaveAllTask.PartGenerationIndex(dpgs);

		assertEquals(3, latest.getKeys().size());

		checkIndexKey(latest, "tag1", "srv1", "2020-01-01");
		checkIndexKey(latest, "tag1", "srv2", "2020-01-01");
		checkIndexKey(latest, "tag2", "srv1", "2020-01-01");
	}

	@Test
	public void getLatestPartsGenerations_getLatest_invalidGeneration() throws ParseException {
		List<DataProtectGeneration> dpgs = new ArrayList<>();
		dpgs.add(getDataProtectGeneration(GenerationStatus.VALID, "2000-01-01"));
		dpgs.add(getDataProtectGeneration(GenerationStatus.INVALID, "2020-01-01"));
		dpgs.add(getDataProtectGeneration(GenerationStatus.VALID, "2010-01-01"));

		PartGenerationIndex latest = new SaveAllTask.PartGenerationIndex(dpgs);

		assertEquals(3, latest.getKeys().size());

		checkIndexKey(latest, "tag1", "srv1", "2010-01-01");
		checkIndexKey(latest, "tag1", "srv2", "2010-01-01");
		checkIndexKey(latest, "tag2", "srv1", "2010-01-01");
	}

	private void checkIndexKey(PartGenerationIndex latest, String tag, String server, String expectedDate)
			throws ParseException {
		PartGeneration part = latest.get(PartGeneration.create(0, server, tag));
		assertNotNull(part);
		assertEquals(GenerationStatus.VALID, part.valid);
		assertTrue(format.parse(expectedDate).equals(part.begin));
	}

	private DataProtectGeneration getDataProtectGeneration(GenerationStatus status, String date) throws ParseException {
		DataProtectGeneration dpg = new DataProtectGeneration();

		PartGeneration pg = new PartGeneration();
		pg.valid = status;
		pg.tag = "tag1";
		pg.server = "srv1";
		pg.begin = format.parse(date);
		dpg.parts.add(pg);

		pg = new PartGeneration();
		pg.valid = status;
		pg.tag = "tag1";
		pg.server = "srv2";
		pg.begin = format.parse(date);
		dpg.parts.add(pg);

		pg = new PartGeneration();
		pg.valid = status;
		pg.tag = "tag2";
		pg.server = "srv1";
		pg.begin = format.parse(date);
		dpg.parts.add(pg);

		return dpg;
	}
}