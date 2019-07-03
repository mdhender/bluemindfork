package net.bluemind.lib.jackson.tests;

import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;
import net.bluemind.bo.api.lic.License;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonTests extends TestCase {

	public void testSerializeLicense() {
		License lic = new License();
		lic.setContent("toto");

		ObjectMapper mapper = new ObjectMapper();
		try {
			byte[] mapped = mapper.writeValueAsBytes(lic);
			System.out.println(new String(mapped));

			License reparsed = mapper.readValue(mapped, License.class);
			assertEquals(reparsed, lic);
			System.out.println("reparsed.content: " + reparsed.getContent());
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	public void testLicList() {
		License lic = new License();
		lic.setContent("toto");
		List<License> lics = new LinkedList<License>();
		lics.add(lic);

		ObjectMapper mapper = new ObjectMapper();
		try {
			byte[] mapped = mapper.writeValueAsBytes(lics);
			System.out.println(new String(mapped));

			List<License> reparsed = mapper.readValue(mapped,
					new TypeReference<List<License>>() {
					});
			System.out.println("reparsed.size: " + reparsed.size() + " "
					+ reparsed.getClass());
			for (License l : reparsed) {
				System.out.println(" - l.content " + l.getContent());
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
}
