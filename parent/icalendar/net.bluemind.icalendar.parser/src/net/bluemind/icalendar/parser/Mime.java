/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.icalendar.parser;

import java.util.HashMap;

public class Mime {

	private static final String APPLICATION_ANDREW_INSET = "application/andrew-inset";
	private static final String APPLICATION_JSON = "application/json";
	private static final String APPLICATION_ZIP = "application/zip";
	private static final String APPLICATION_X_GZIP = "application/x-gzip";
	private static final String APPLICATION_TGZ = "application/tgz";
	private static final String APPLICATION_MSWORD = "application/msword";
	private static final String APPLICATION_MSWORD_2007 = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
	private static final String APPLICATION_VND_TEXT = "application/vnd.oasis.opendocument.text";
	private static final String APPLICATION_POSTSCRIPT = "application/postscript";
	private static final String APPLICATION_PDF = "application/pdf";
	private static final String APPLICATION_JNLP = "application/jnlp";
	private static final String APPLICATION_MAC_BINHEX40 = "application/mac-binhex40";
	private static final String APPLICATION_MAC_COMPACTPRO = "application/mac-compactpro";
	private static final String APPLICATION_MATHML_XML = "application/mathml+xml";
	private static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
	private static final String APPLICATION_ODA = "application/oda";
	private static final String APPLICATION_RDF_XML = "application/rdf+xml";
	private static final String APPLICATION_JAVA_ARCHIVE = "application/java-archive";
	private static final String APPLICATION_RDF_SMIL = "application/smil";
	private static final String APPLICATION_SRGS = "application/srgs";
	private static final String APPLICATION_SRGS_XML = "application/srgs+xml";
	private static final String APPLICATION_VND_MIF = "application/vnd.mif";
	private static final String APPLICATION_VND_MSEXCEL = "application/vnd.ms-excel";
	private static final String APPLICATION_VND_SPREADSHEET = "application/vnd.oasis.opendocument.spreadsheet";
	private static final String APPLICATION_VND_MSPOWERPOINT = "application/vnd.ms-powerpoint";
	private static final String APPLICATION_VND_RNREALMEDIA = "application/vnd.rn-realmedia";
	private static final String APPLICATION_X_BCPIO = "application/x-bcpio";
	private static final String APPLICATION_X_CDLINK = "application/x-cdlink";
	private static final String APPLICATION_X_CHESS_PGN = "application/x-chess-pgn";
	private static final String APPLICATION_X_CPIO = "application/x-cpio";
	private static final String APPLICATION_X_CSH = "application/x-csh";
	private static final String APPLICATION_X_DIRECTOR = "application/x-director";
	private static final String APPLICATION_X_DVI = "application/x-dvi";
	private static final String APPLICATION_X_FUTURESPLASH = "application/x-futuresplash";
	private static final String APPLICATION_X_GTAR = "application/x-gtar";
	private static final String APPLICATION_X_HDF = "application/x-hdf";
	private static final String APPLICATION_X_JAVASCRIPT = "application/x-javascript";
	private static final String APPLICATION_X_KOAN = "application/x-koan";
	private static final String APPLICATION_X_LATEX = "application/x-latex";
	private static final String APPLICATION_X_NETCDF = "application/x-netcdf";
	private static final String APPLICATION_X_OGG = "application/x-ogg";
	private static final String APPLICATION_X_SH = "application/x-sh";
	private static final String APPLICATION_X_SHAR = "application/x-shar";
	private static final String APPLICATION_X_SHOCKWAVE_FLASH = "application/x-shockwave-flash";
	private static final String APPLICATION_X_STUFFIT = "application/x-stuffit";
	private static final String APPLICATION_X_SV4CPIO = "application/x-sv4cpio";
	private static final String APPLICATION_X_SV4CRC = "application/x-sv4crc";
	private static final String APPLICATION_X_TAR = "application/x-tar";
	private static final String APPLICATION_X_RAR_COMPRESSED = "application/x-rar-compressed";
	private static final String APPLICATION_X_TCL = "application/x-tcl";
	private static final String APPLICATION_X_TEX = "application/x-tex";
	private static final String APPLICATION_X_TEXINFO = "application/x-texinfo";
	private static final String APPLICATION_X_TROFF = "application/x-troff";
	private static final String APPLICATION_X_TROFF_MAN = "application/x-troff-man";
	private static final String APPLICATION_X_TROFF_ME = "application/x-troff-me";
	private static final String APPLICATION_X_TROFF_MS = "application/x-troff-ms";
	private static final String APPLICATION_X_USTAR = "application/x-ustar";
	private static final String APPLICATION_X_WAIS_SOURCE = "application/x-wais-source";
	private static final String APPLICATION_VND_MOZZILLA_XUL_XML = "application/vnd.mozilla.xul+xml";
	private static final String APPLICATION_XHTML_XML = "application/xhtml+xml";
	private static final String APPLICATION_XSLT_XML = "application/xslt+xml";
	private static final String APPLICATION_XML = "application/xml";
	private static final String APPLICATION_XML_DTD = "application/xml-dtd";
	private static final String IMAGE_BMP = "image/bmp";
	private static final String IMAGE_CGM = "image/cgm";
	private static final String IMAGE_GIF = "image/gif";
	private static final String IMAGE_IEF = "image/ief";
	private static final String IMAGE_JPEG = "image/jpeg";
	private static final String IMAGE_TIFF = "image/tiff";
	private static final String IMAGE_PNG = "image/png";
	private static final String IMAGE_SVG_XML = "image/svg+xml";
	private static final String IMAGE_VND_DJVU = "image/vnd.djvu";
	private static final String IMAGE_WAP_WBMP = "image/vnd.wap.wbmp";
	private static final String IMAGE_X_CMU_RASTER = "image/x-cmu-raster";
	private static final String IMAGE_X_ICON = "image/x-icon";
	private static final String IMAGE_X_PORTABLE_ANYMAP = "image/x-portable-anymap";
	private static final String IMAGE_X_PORTABLE_BITMAP = "image/x-portable-bitmap";
	private static final String IMAGE_X_PORTABLE_GRAYMAP = "image/x-portable-graymap";
	private static final String IMAGE_X_PORTABLE_PIXMAP = "image/x-portable-pixmap";
	private static final String IMAGE_X_RGB = "image/x-rgb";
	private static final String AUDIO_BASIC = "audio/basic";
	private static final String AUDIO_MIDI = "audio/midi";
	private static final String AUDIO_MPEG = "audio/mpeg";
	private static final String AUDIO_X_AIFF = "audio/x-aiff";
	private static final String AUDIO_X_MPEGURL = "audio/x-mpegurl";
	private static final String AUDIO_X_PN_REALAUDIO = "audio/x-pn-realaudio";
	private static final String AUDIO_X_WAV = "audio/x-wav";
	private static final String CHEMICAL_X_PDB = "chemical/x-pdb";
	private static final String CHEMICAL_X_XYZ = "chemical/x-xyz";
	private static final String MODEL_IGES = "model/iges";
	private static final String MODEL_MESH = "model/mesh";
	private static final String MODEL_VRLM = "model/vrml";
	private static final String TEXT_PLAIN = "text/plain";
	private static final String TEXT_RICHTEXT = "text/richtext";
	private static final String TEXT_RTF = "text/rtf";
	private static final String TEXT_HTML = "text/html";
	private static final String TEXT_CALENDAR = "text/calendar";
	private static final String TEXT_CSS = "text/css";
	private static final String TEXT_SGML = "text/sgml";
	private static final String TEXT_TAB_SEPARATED_VALUES = "text/tab-separated-values";
	private static final String TEXT_VND_WAP_XML = "text/vnd.wap.wml";
	private static final String TEXT_VND_WAP_WMLSCRIPT = "text/vnd.wap.wmlscript";
	private static final String TEXT_X_SETEXT = "text/x-setext";
	private static final String TEXT_X_COMPONENT = "text/x-component";
	private static final String VIDEO_QUICKTIME = "video/quicktime";
	private static final String VIDEO_MPEG = "video/mpeg";
	private static final String VIDEO_VND_MPEGURL = "video/vnd.mpegurl";
	private static final String VIDEO_X_MSVIDEO = "video/x-msvideo";
	private static final String VIDEO_X_MS_WMV = "video/x-ms-wmv";
	private static final String VIDEO_X_SGI_MOVIE = "video/x-sgi-movie";
	private static final String X_CONFERENCE_X_COOLTALK = "x-conference/x-cooltalk";

	private static HashMap<String, String> mimeTypeToExtension;
	private static HashMap<String, String> extensionToMimeType;

	static {
		mimeTypeToExtension = new HashMap<String, String>(200);
		extensionToMimeType = new HashMap<String, String>(200);

		mimeTypeToExtension.put(APPLICATION_VND_MOZZILLA_XUL_XML, "xul");
		mimeTypeToExtension.put(APPLICATION_JSON, "json");
		mimeTypeToExtension.put(X_CONFERENCE_X_COOLTALK, "ice");
		mimeTypeToExtension.put(VIDEO_X_SGI_MOVIE, "movie");
		mimeTypeToExtension.put(VIDEO_X_MSVIDEO, "avi");
		mimeTypeToExtension.put(VIDEO_X_MS_WMV, "wmv");
		mimeTypeToExtension.put(VIDEO_VND_MPEGURL, "m4u");
		mimeTypeToExtension.put(TEXT_X_COMPONENT, "htc");
		mimeTypeToExtension.put(TEXT_X_SETEXT, "etx");
		mimeTypeToExtension.put(TEXT_VND_WAP_WMLSCRIPT, "wmls");
		mimeTypeToExtension.put(TEXT_VND_WAP_XML, "wml");
		mimeTypeToExtension.put(TEXT_TAB_SEPARATED_VALUES, "tsv");
		mimeTypeToExtension.put(TEXT_SGML, "sgml");
		mimeTypeToExtension.put(TEXT_CSS, "css");
		mimeTypeToExtension.put(TEXT_CALENDAR, "ics");
		mimeTypeToExtension.put(MODEL_VRLM, "vrlm");
		mimeTypeToExtension.put(MODEL_MESH, "mesh");
		mimeTypeToExtension.put(MODEL_IGES, "iges");
		mimeTypeToExtension.put(IMAGE_X_RGB, "rgb");
		mimeTypeToExtension.put(IMAGE_X_PORTABLE_PIXMAP, "ppm");
		mimeTypeToExtension.put(IMAGE_X_PORTABLE_GRAYMAP, "pgm");
		mimeTypeToExtension.put(IMAGE_X_PORTABLE_BITMAP, "pbm");
		mimeTypeToExtension.put(IMAGE_X_PORTABLE_ANYMAP, "pnm");
		mimeTypeToExtension.put(IMAGE_X_ICON, "ico");
		mimeTypeToExtension.put(IMAGE_X_CMU_RASTER, "ras");
		mimeTypeToExtension.put(IMAGE_WAP_WBMP, "wbmp");
		mimeTypeToExtension.put(IMAGE_VND_DJVU, "djvu");
		mimeTypeToExtension.put(IMAGE_SVG_XML, "svg");
		mimeTypeToExtension.put(IMAGE_IEF, "ief");
		mimeTypeToExtension.put(IMAGE_CGM, "cgm");
		mimeTypeToExtension.put(IMAGE_BMP, "bmp");
		mimeTypeToExtension.put(CHEMICAL_X_XYZ, "xyz");
		mimeTypeToExtension.put(CHEMICAL_X_PDB, "pdb");
		mimeTypeToExtension.put(AUDIO_X_PN_REALAUDIO, "ra");
		mimeTypeToExtension.put(AUDIO_X_MPEGURL, "m3u");
		mimeTypeToExtension.put(AUDIO_X_AIFF, "aiff");
		mimeTypeToExtension.put(AUDIO_MPEG, "mp3");
		mimeTypeToExtension.put(AUDIO_MIDI, "midi");
		mimeTypeToExtension.put(APPLICATION_XML_DTD, "dtd");
		mimeTypeToExtension.put(APPLICATION_XML, "xml");
		mimeTypeToExtension.put(APPLICATION_XSLT_XML, "xslt");
		mimeTypeToExtension.put(APPLICATION_XHTML_XML, "xhtml");
		mimeTypeToExtension.put(APPLICATION_X_WAIS_SOURCE, "src");
		mimeTypeToExtension.put(APPLICATION_X_USTAR, "ustar");
		mimeTypeToExtension.put(APPLICATION_X_TROFF_MS, "ms");
		mimeTypeToExtension.put(APPLICATION_X_TROFF_ME, "me");
		mimeTypeToExtension.put(APPLICATION_X_TROFF_MAN, "man");
		mimeTypeToExtension.put(APPLICATION_X_TROFF, "roff");
		mimeTypeToExtension.put(APPLICATION_X_TEXINFO, "texi");
		mimeTypeToExtension.put(APPLICATION_X_TEX, "tex");
		mimeTypeToExtension.put(APPLICATION_X_TCL, "tcl");
		mimeTypeToExtension.put(APPLICATION_X_SV4CRC, "sv4crc");
		mimeTypeToExtension.put(APPLICATION_X_SV4CPIO, "sv4cpio");
		mimeTypeToExtension.put(APPLICATION_X_STUFFIT, "sit");
		mimeTypeToExtension.put(APPLICATION_X_SHOCKWAVE_FLASH, "swf");
		mimeTypeToExtension.put(APPLICATION_X_SHAR, "shar");
		mimeTypeToExtension.put(APPLICATION_X_SH, "sh");
		mimeTypeToExtension.put(APPLICATION_X_NETCDF, "cdf");
		mimeTypeToExtension.put(APPLICATION_X_LATEX, "latex");
		mimeTypeToExtension.put(APPLICATION_X_KOAN, "skm");
		mimeTypeToExtension.put(APPLICATION_X_JAVASCRIPT, "js");
		mimeTypeToExtension.put(APPLICATION_X_HDF, "hdf");
		mimeTypeToExtension.put(APPLICATION_X_GTAR, "gtar");
		mimeTypeToExtension.put(APPLICATION_X_FUTURESPLASH, "spl");
		mimeTypeToExtension.put(APPLICATION_X_DVI, "dvi");
		mimeTypeToExtension.put(APPLICATION_X_DIRECTOR, "dir");
		mimeTypeToExtension.put(APPLICATION_X_CSH, "csh");
		mimeTypeToExtension.put(APPLICATION_X_CPIO, "cpio");
		mimeTypeToExtension.put(APPLICATION_X_CHESS_PGN, "pgn");
		mimeTypeToExtension.put(APPLICATION_X_CDLINK, "vcd");
		mimeTypeToExtension.put(APPLICATION_X_BCPIO, "bcpio");
		mimeTypeToExtension.put(APPLICATION_VND_RNREALMEDIA, "rm");
		mimeTypeToExtension.put(APPLICATION_VND_MSPOWERPOINT, "ppt");
		mimeTypeToExtension.put(APPLICATION_VND_MIF, "mif");
		mimeTypeToExtension.put(APPLICATION_SRGS_XML, "grxml");
		mimeTypeToExtension.put(APPLICATION_SRGS, "gram");
		mimeTypeToExtension.put(APPLICATION_RDF_SMIL, "smil");
		mimeTypeToExtension.put(APPLICATION_RDF_XML, "rdf");
		mimeTypeToExtension.put(APPLICATION_X_OGG, "ogg");
		mimeTypeToExtension.put(APPLICATION_ODA, "oda");
		mimeTypeToExtension.put(APPLICATION_MATHML_XML, "mathml");
		mimeTypeToExtension.put(APPLICATION_MAC_COMPACTPRO, "cpt");
		mimeTypeToExtension.put(APPLICATION_MAC_BINHEX40, "hqx");
		mimeTypeToExtension.put(APPLICATION_JNLP, "jnlp");
		mimeTypeToExtension.put(APPLICATION_ANDREW_INSET, "ez");
		mimeTypeToExtension.put(TEXT_PLAIN, "txt");
		mimeTypeToExtension.put(TEXT_RTF, "rtf");
		mimeTypeToExtension.put(TEXT_RICHTEXT, "rtx");
		mimeTypeToExtension.put(TEXT_HTML, "html");
		mimeTypeToExtension.put(APPLICATION_ZIP, "zip");
		mimeTypeToExtension.put(APPLICATION_X_RAR_COMPRESSED, "rar");
		mimeTypeToExtension.put(APPLICATION_X_GZIP, "gzip");
		mimeTypeToExtension.put(APPLICATION_TGZ, "tgz");
		mimeTypeToExtension.put(APPLICATION_X_TAR, "tar");
		mimeTypeToExtension.put(IMAGE_GIF, "gif");
		mimeTypeToExtension.put(IMAGE_JPEG, "jpg");
		mimeTypeToExtension.put(IMAGE_TIFF, "tiff");
		mimeTypeToExtension.put(IMAGE_PNG, "png");
		mimeTypeToExtension.put(AUDIO_BASIC, "au");
		mimeTypeToExtension.put(AUDIO_X_WAV, "wav");
		mimeTypeToExtension.put(VIDEO_QUICKTIME, "mov");
		mimeTypeToExtension.put(VIDEO_MPEG, "mpg");
		mimeTypeToExtension.put(APPLICATION_MSWORD, "doc");
		mimeTypeToExtension.put(APPLICATION_MSWORD_2007, "docx");
		mimeTypeToExtension.put(APPLICATION_VND_TEXT, "odt");
		mimeTypeToExtension.put(APPLICATION_VND_MSEXCEL, "xls");
		mimeTypeToExtension.put(APPLICATION_VND_SPREADSHEET, "ods");
		mimeTypeToExtension.put(APPLICATION_POSTSCRIPT, "ps");
		mimeTypeToExtension.put(APPLICATION_PDF, "pdf");
		mimeTypeToExtension.put(APPLICATION_OCTET_STREAM, "exe");
		mimeTypeToExtension.put(APPLICATION_JAVA_ARCHIVE, "jar");

		mimeTypeToExtension.entrySet().forEach(entry -> {
			extensionToMimeType.put(entry.getValue(), entry.getKey());
		});
	}

	public static String getExtension(String mimeType) {
		return mimeTypeToExtension.getOrDefault(mimeType, "data");
	}

	public static String getMimeType(String name) {
		String extension = name.contains(".") ? name.substring(name.lastIndexOf(".") + 1) : name;
		return extensionToMimeType.getOrDefault(extension, "application/octet-stream");
	}
}
