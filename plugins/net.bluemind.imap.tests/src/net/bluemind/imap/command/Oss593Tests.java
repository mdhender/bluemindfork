package net.bluemind.imap.command;

import junit.framework.TestCase;
import net.bluemind.imap.EncodedWord;
import net.bluemind.imap.command.parser.BodyStructureParser;
import net.bluemind.imap.mime.MimeTree;

public class Oss593Tests extends TestCase {

	public void testParseOss593() {
		String envel = "((\"TEXT\" \"HTML\" (\"CHARSET\" \"utf-8\") NIL NIL \"BASE64\" 334 5 NIL NIL NIL NIL)(\"TEXT\" \"PLAIN\" (\"CHARSET\" \"utf-8\") NIL NIL \"BASE64\" 50 1 NIL NIL NIL NIL)(\"TEXT\" \"PLAIN\" (\"CHARSET\" \"utf-8\") NIL NIL \"BASE64\" 756 10 NIL NIL NIL NIL)(\"TEXT\" \"HTML\" (\"CHARSET\" \"utf-8\") NIL NIL \"BASE64\" 24726 317 NIL NIL NIL NIL)(\"TEXT\" \"PLAIN\" (\"CHARSET\" \"utf-8\") NIL NIL \"BASE64\" 4406 57 NIL NIL NIL NIL)(\"APPLICATION\" \"PDF\" (\"NAME\" \"=?ISO-8859-1?Q?MADINAT_Nora_courrier_de_l=27=E9poux=2Epdf?=\") NIL NIL \"BASE64\" 0 NIL (\"ATTACHMENT\" (\"FILENAME*\" \"=?ISO-8859-1?Q?MADINAT_Nora_courrier_de_l=27=E9poux=2Epdf?=\" \"SIZE\" \"184376\")) NIL NIL) \"MIXED\" (\"BOUNDARY\" \"----E1LIY290YJYNLSMV7U2GVXHLXLMTI3\") NIL NIL NIL)";

		String dec = EncodedWord.decode("=?ISO-8859-1?Q?MADINAT_Nora_courrier_de_l=27=E9poux=2Epdf?=").toString();
		System.out.println(dec);

		MimeTree bs = new BodyStructureParser().parse(envel.getBytes());
		assertNotNull(bs);
		System.out.println("bs: " + bs);
	}

}
