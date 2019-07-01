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
package net.bluemind.core.api.date;

import java.util.HashMap;
import java.util.Map;

public class TimezoneExtensions {
	private static Map<String, String> msdnTimezone = new HashMap<>();
	private static Map<String, String> lotusTimezone = new HashMap<>();

	// from
	// https://msdn.microsoft.com/en-us/library/ms912391(v=winembedded.11).aspx
	static {
		put("International Date Line West", "Etc/GMT+12", msdnTimezone);
		put("Dateline Standard Time", "Etc/GMT-12", msdnTimezone);

		put("Coordinated Universal Time-11", "Etc/GMT+11", msdnTimezone);
		put("Hawaii", "Pacific/Honolulu", msdnTimezone);
		put("Hawaiian Standard Time", "Pacific/Honolulu", msdnTimezone);

		put("Alaska", "America/Anchorage", msdnTimezone);
		put("Alaskan Standard Time", "America/Anchorage", msdnTimezone);

		// <!-- (UTC-08:00) Baja California -->

		msdnTimezone.put("Pacific Time (US & Canada)", "America/Los_Angeles");
		put("Pacific Standard Time", "America/Los_Angeles", msdnTimezone);

		put("Arizona", "America/Phoenix", msdnTimezone);
		put("US Mountain Standard Time", "America/Phoenix", msdnTimezone);
		put("Chihuahua, La Paz, Mazatlan", "America/Chihuahua", msdnTimezone);
		msdnTimezone.put("Mountain Standard Time (Mexico)", "America/Chihuahua");

		msdnTimezone.put("Mountain Time (US & Canada)", "America/Denver");
		put("Mountain Standard Time", "America/Denver", msdnTimezone);

		put("Central America", "America/Guatemala", msdnTimezone);
		put("Central America Standard Time", "America/Guatemala", msdnTimezone);

		msdnTimezone.put("Central Time (US & Canada)", "America/Chicago");
		put("Central Standard Time", "America/Chicago", msdnTimezone);

		put("Guadalajara, Mexico City, Monterrey", "America/Mexico_City", msdnTimezone);
		msdnTimezone.put("Central Standard Time (Mexico)", "America/Mexico_City");

		put("Saskatchewan", "America/Regina", msdnTimezone);
		put("Canada Central Standard Time", "America/Regina", msdnTimezone);

		put("Bogota, Lima, Quito, Rio Branco", "America/Bogota", msdnTimezone);

		put("SA Pacific Standard Time", "America/Bogota", msdnTimezone);

		put("Chetumal", "America/Cancun", msdnTimezone);
		msdnTimezone.put("Eastern Standard Time (Mexico)", "America/Cancun");

		msdnTimezone.put("Eastern Time (US & Canada)", "America/New_York");
		put("Eastern Standard Time", "America/New_York", msdnTimezone);

		msdnTimezone.put("Indiana (East)", "America/Indianapolis");
		put("US Eastern Standard Time", "America/Indianapolis", msdnTimezone);

		put("Caracas", "America/Caracas", msdnTimezone);
		put("Venezuela Standard Time", "America/Caracas", msdnTimezone);

		put("Asuncion", "America/Asuncion", msdnTimezone);
		put("Paraguay Standard Time", "America/Asuncion", msdnTimezone);

		msdnTimezone.put("Atlantic Time (Canada)", "America/Halifax");
		put("Atlantic Standard Time", "America/Halifax", msdnTimezone);

		put("Cuiaba", "America/Cuiaba", msdnTimezone);
		put("Central Brazilian Standard Time", "America/Cuiaba", msdnTimezone);

		put("Georgetown, La Paz, Manaus, San Juan", "America/La_Paz", msdnTimezone);
		put("SA Western Standard Time", "America/La_Paz", msdnTimezone);

		put("Newfoundland", "America/St_Johns", msdnTimezone);
		put("Newfoundland Standard Time", "America/St_Johns", msdnTimezone);

		put("Brasilia", "America/Sao_Paulo", msdnTimezone);
		put("E. South America Standard Time", "America/Sao_Paulo", msdnTimezone);

		put("Cayenne, Fortaleza", "America/Cayenne", msdnTimezone);
		put("SA Eastern Standard Time", "America/Cayenne", msdnTimezone);

		put("City of Buenos Aires", "America/Buenos_Aires", msdnTimezone);
		put("Argentina Standard Time", "America/Buenos_Aires", msdnTimezone);

		put("Greenland", "America/Godthab", msdnTimezone);
		put("Greenland Standard Time", "America/Godthab", msdnTimezone);

		put("Montevideo", "America/Montevideo", msdnTimezone);
		put("Montevideo Standard Time", "America/Montevideo", msdnTimezone);

		put("Salvador", "America/Bahia", msdnTimezone);
		put("Bahia Standard Time", "America/Montevideo", msdnTimezone);

		put("Santiago", "America/Santiago", msdnTimezone);
		put("Pacific SA Standard Time", "America/Santiago", msdnTimezone);

		put("Coordinated Universal Time-02", "Etc/GMT+2", msdnTimezone);
		put("UTC-02", "Etc/GMT+2", msdnTimezone);

		put("Azores", "Atlantic/Azores", msdnTimezone);
		put("Azores Standard Time", "Atlantic/Azores", msdnTimezone);

		put("Cabo Verde Is.", "Atlantic/Cape_Verde", msdnTimezone);
		put("Cape Verde Standard Time", "Atlantic/Cape_Verde", msdnTimezone);

		put("Casablanca", "Africa/Casablanca", msdnTimezone);
		put("Morocco Standard Time", "Africa/Casablanca", msdnTimezone);

		put("Coordinated Universal Time", "Etc/GMT", msdnTimezone);

		put("Casablanca", "Africa/Casablanca", msdnTimezone);
		put("Dublin, Edinburgh, Lisbon, London", "Europe/London", msdnTimezone);
		put("GMT Standard Time", "Europe/London", msdnTimezone);

		put("Monrovia, Reykjavik", "Atlantic/Reykjavik", msdnTimezone);
		put("Greenwich Standard Time", "Atlantic/Reykjavik", msdnTimezone);

		put("Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna", "Europe/Berlin", msdnTimezone);
		put("W. Europe Standard Time", "Europe/Berlin", msdnTimezone);

		put("Belgrade, Bratislava, Budapest, Ljubljana, Prague", "Europe/Budapest", msdnTimezone);
		put("Central Europe Standard Time", "Europe/Budapest", msdnTimezone);

		put("Brussels, Copenhagen, Madrid, Paris", "Europe/Paris", msdnTimezone);
		put("Romance Standard Time", "Europe/Paris", msdnTimezone);

		put("Sarajevo, Skopje, Warsaw, Zagreb", "Europe/Warsaw", msdnTimezone);
		put("Central European Standard Time", "Europe/Warsaw", msdnTimezone);

		put("West Central Africa", "Africa/Lagos", msdnTimezone);
		put("W. Central Africa Standard Time", "Africa/Lagos", msdnTimezone);

		put("Windhoek", "Africa/Windhoek", msdnTimezone);
		put("Namibia Standard Time", "Africa/Windhoek", msdnTimezone);

		put("Amman", "Asia/Amman", msdnTimezone);
		put("Jordan Standard Time", "Asia/Amman", msdnTimezone);

		put("Athens, Bucharest", "Europe/Bucharest", msdnTimezone);
		put("GTB Standard Time", "Europe/Bucharest", msdnTimezone);

		put("Beirut", "Asia/Beirut", msdnTimezone);
		put("Middle East Standard Time", "Asia/Beirut", msdnTimezone);

		put("Cairo", "Africa/Cairo", msdnTimezone);
		put("Egypt Standard Time", "Africa/Cairo", msdnTimezone);

		put("Damascus", "Asia/Damascus", msdnTimezone);
		put("Syria Standard Time", "Asia/Damascus", msdnTimezone);

		put("E. Europe", "Europe/Chisinau", msdnTimezone);
		put("E. Europe Standard Time", "Europe/Chisinau", msdnTimezone);

		put("Harare, Pretoria", "Africa/Johannesburg", msdnTimezone);
		put("South Africa Standard Time", "Africa/Johannesburg", msdnTimezone);

		put("Helsinki, Kyiv, Riga, Sofia, Tallinn, Vilnius", "Europe/Kiev", msdnTimezone);
		put("FLE Standard Time", "Europe/Kiev", msdnTimezone);

		put("Istanbul", "Europe/Istanbul", msdnTimezone);
		put("Turkey Standard Time", "Europe/Istanbul", msdnTimezone);

		put("Jerusalem", "Asia/Jerusalem", msdnTimezone);
		put("Israel Standard Time", "Asia/Jerusalem", msdnTimezone);

		msdnTimezone.put("Kaliningrad (RTZ 1)", "Europe/Kaliningrad");
		put("Kaliningrad Standard Time", "Europe/Kaliningrad", msdnTimezone);

		put("Tripoli", "Africa/Tripoli", msdnTimezone);
		put("Libya Standard Time", "Africa/Tripoli", msdnTimezone);

		put("Baghdad", "Asia/Baghdad", msdnTimezone);
		put("Arabic Standard Time", "Asia/Baghdad", msdnTimezone);

		put("Kuwait, Riyadh", "Asia/Riyadh", msdnTimezone);
		put("Arab Standard Time", "Asia/Riyadh", msdnTimezone);

		put("Minsk", "Europe/Minsk", msdnTimezone);
		put("Belarus Standard Time", "Europe/Minsk", msdnTimezone);

		msdnTimezone.put("Moscow, St. Petersburg, Volgograd (RTZ 2)", "Europe/Moscow");
		put("Russian Standard Time", "Europe/Moscow", msdnTimezone);

		put("Nairobi", "Africa/Nairobi", msdnTimezone);
		put("E. Africa Standard Time", "Africa/Nairobi", msdnTimezone);

		put("Tehran", "Asia/Tehran", msdnTimezone);
		put("Iran Standard Time", "Asia/Tehran", msdnTimezone);

		put("Abu Dhabi, Muscat", "Asia/Dubai", msdnTimezone);
		put("Arabian Standard Time", "Asia/Dubai", msdnTimezone);

		put("Baku", "Asia/Baku", msdnTimezone);
		put("Azerbaijan Standard Time", "Asia/Baku", msdnTimezone);

		msdnTimezone.put("Izhevsk, Samara (RTZ 3)", "Europe/Samara");
		put("Russia Time Zone 3", "Europe/Samara", msdnTimezone);

		put("Port Louis", "Indian/Mauritius", msdnTimezone);
		put("Mauritius Standard Time", "Indian/Mauritius", msdnTimezone);

		put("Tbilisi", "Asia/Tbilisi", msdnTimezone);
		put("Georgian Standard Time", "Asia/Tbilisi", msdnTimezone);

		put("Yerevan", "Asia/Yerevan", msdnTimezone);
		put("Caucasus Standard Time", "Asia/Yerevan", msdnTimezone);

		put("Kabul", "Asia/Kabul", msdnTimezone);
		put("Afghanistan Standard Time", "Asia/Kabul", msdnTimezone);

		put("Ashgabat, Tashkent", "Asia/Tashkent", msdnTimezone);
		put("West Asia Standard Time", "Asia/Tashkent", msdnTimezone);

		msdnTimezone.put("Ekaterinburg (RTZ 4)", "Asia/Yekaterinburg");
		put("Ekaterinburg Standard Time", "Asia/Yekaterinburg", msdnTimezone);

		put("Islamabad, Karachi", "Asia/Karachi", msdnTimezone);
		put("Pakistan Standard Time", "Asia/Karachi", msdnTimezone);

		put("Chennai, Kolkata, Mumbai, New Delhi", "Asia/Calcutta", msdnTimezone);
		put("India Standard Time", "Asia/Calcutta", msdnTimezone);

		put("Sri Jayawardenepura", "Asia/Colombo", msdnTimezone);
		put("Sri Lanka Standard Time", "Asia/Colombo", msdnTimezone);

		put("Kathmandu", "Asia/Katmandu", msdnTimezone);
		put("Nepal Standard Time", "Asia/Katmandu", msdnTimezone);

		put("Astana", "Asia/Almaty", msdnTimezone);
		put("Central Asia Standard Time", "Asia/Almaty", msdnTimezone);

		put("Dhaka", "Asia/Dhaka", msdnTimezone);
		put("Bangladesh Standard Time", "Asia/Dhaka", msdnTimezone);

		msdnTimezone.put("Novosibirsk (RTZ 5)", "Asia/Novosibirsk");
		put("Novosibirsk", "Asia/Novosibirsk", msdnTimezone);

		msdnTimezone.put("Yangon (Rangoon)", "Asia/Rangoon");
		put("Myanmar Standard Time", "Asia/Rangoon", msdnTimezone);

		put("Bangkok, Hanoi, Jakarta", "Asia/Bangkok", msdnTimezone);
		put("SE Asia Standard Time", "Asia/Bangkok", msdnTimezone);

		msdnTimezone.put("Krasnoyarsk (RTZ 6)", "Asia/Krasnoyarsk");
		put("North Asia Standard Time", "Asia/Krasnoyarsk", msdnTimezone);

		put("Beijing, Chongqing, Hong Kong, Urumqi", "Asia/Shanghai", msdnTimezone);
		put("China Standard Time", "Asia/Shanghai", msdnTimezone);

		msdnTimezone.put("Irkutsk (RTZ 7)", "Asia/Irkutsk");
		put("North Asia East Standard Time", "Asia/Irkutsk", msdnTimezone);

		put("Kuala Lumpur, Singapore", "Asia/Singapore", msdnTimezone);
		put("Singapore Standard Time", "Asia/Singapore", msdnTimezone);

		put("Perth", "Australia/Perth", msdnTimezone);
		put("W. Australia Standard Time", "Australia/Perth", msdnTimezone);

		put("Taipei", "Asia/Taipei", msdnTimezone);
		put("Taipei Standard Time", "Asia/Taipei", msdnTimezone);

		put("Ulaanbaatar", "Asia/Ulaanbaatar", msdnTimezone);
		put("Ulaanbaatar Standard Time", "Asia/Ulaanbaatar", msdnTimezone);

		put("Pyongyang", "Asia/Pyongyang", msdnTimezone);
		put("North Korea Standard Time", "Asia/Pyongyang", msdnTimezone);

		put("Osaka, Sapporo, Tokyo", "Asia/Tokyo", msdnTimezone);
		put("Tokyo Standard Time", "Asia/Tokyo", msdnTimezone);

		put("Seoul", "Asia/Seoul", msdnTimezone);
		put("Korea Standard Time", "Asia/Seoul", msdnTimezone);

		msdnTimezone.put("Yakutsk (RTZ 8)", "Asia/Yakutsk");
		put("Yakutsk Standard Time", "Asia/Yakutsk", msdnTimezone);

		put("Adelaide", "Australia/Adelaide", msdnTimezone);
		put("Cen. Australia Standard Time", "Australia/Adelaide", msdnTimezone);

		put("Darwin", "Australia/Darwin", msdnTimezone);
		put("AUS Central Standard Time", "Australia/Darwin", msdnTimezone);

		put("Brisbane", "Australia/Brisbane", msdnTimezone);
		put("E. Australia Standard Time", "Australia/Brisbane", msdnTimezone);

		put("Canberra, Melbourne, Sydney", "Australia/Sydney", msdnTimezone);
		put("AUS Eastern Standard Time", "Australia/Sydney", msdnTimezone);

		put("Guam, Port Moresby", "Pacific/Port_Moresby", msdnTimezone);
		put("West Pacific Standard Time", "Pacific/Port_Moresby", msdnTimezone);

		put("Hobart", "Australia/Hobart", msdnTimezone);
		put("Tasmania Standard Time", "Australia/Hobart", msdnTimezone);

		put("Magadan", "Asia/Magadan", msdnTimezone);
		put("Magadan Standard Time", "Asia/Magadan", msdnTimezone);

		msdnTimezone.put("Vladivostok, Magadan (RTZ 9)", "Asia/Vladivostok");
		put("Vladivostok Standard Time", "Asia/Vladivostok", msdnTimezone);

		msdnTimezone.put("Chokurdakh (RTZ 10)", "Asia/Srednekolymsk");
		put("Russia Time Zone 10", "Asia/Srednekolymsk", msdnTimezone);

		put("Solomon Is., New Caledonia", "Pacific/Guadalcanal", msdnTimezone);
		put("Central Pacific Standard Time", "Pacific/Guadalcanal", msdnTimezone);

		msdnTimezone.put("Anadyr, Petropavlovsk-Kamchatsky (RTZ 11)", "Asia/Kamchatka");
		put("Russia Time Zone 11", "Asia/Kamchatka", msdnTimezone);

		put("Auckland, Wellington", "Pacific/Auckland", msdnTimezone);
		put("New Zealand Standard Time", "Pacific/Auckland", msdnTimezone);

		put("UTC+12", "Etc/GMT-12", msdnTimezone);
		put("Coordinated Universal Time+12", "Etc/GMT-12", msdnTimezone);

		put("Fiji", "Pacific/Fiji", msdnTimezone);
		put("Fiji Standard Time", "Pacific/Fiji", msdnTimezone);

		put("Nuku'alofa", "Pacific/Tongatapu", msdnTimezone);
		put("Tonga Standard Time", "Pacific/Tongatapu", msdnTimezone);

		put("Samoa", "Pacific/Apia", msdnTimezone);
		put("Samoa Standard Time", "Pacific/Apia", msdnTimezone);

		put("Kiritimati Island", "Pacific/Kiritimati", msdnTimezone);
		put("Line Islands Standard Time", "Pacific/Kiritimati", msdnTimezone);

	}

	// domino timezone ..
	// http://domingo.sourceforge.net/xref/de/bea/domingo/http/DominoTimeZone.html
	static {
		// lotus timezone
		put("Western/Central Europe", "CET", lotusTimezone);
		put("Eastern", "EST5EDT", lotusTimezone);
		put("Western", "WET", lotusTimezone);

	}

	public static String translate(String strangeTimeZoneId) {
		String unknown = strangeTimeZoneId.toUpperCase();
		String ret = msdnTimezone.get(unknown);
		if (ret != null) {
			return ret;
		}

		return lotusTimezone.get(unknown);
	}

	private static void put(String alias, String id, Map<String, String> timezoneRegistry) {
		timezoneRegistry.put(alias.toUpperCase(), id);
	}

}
