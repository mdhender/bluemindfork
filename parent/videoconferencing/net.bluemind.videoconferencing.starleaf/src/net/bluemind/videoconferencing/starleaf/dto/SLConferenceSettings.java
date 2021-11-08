/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.videoconferencing.starleaf.dto;

import com.google.common.base.Strings;

import io.vertx.core.json.JsonObject;

/**
 * https://support.starleaf.com/integrating/cloud-api/request-objects/#conf_set
 */
public class SLConferenceSettings {

	public enum Layout {
		speaker_with_strip, equal_panes, speaker_only, large_speaker
	}

	public final String ownerId;

	// The title of the conference. Must be between 2 and 256 characters in length.
	public final String title;

	// A description of the conference, maximum length 2048 characters.
	public final String description;

	// The layout to be used for the conference. Valid values are
	// “speaker_with_strip”, “equal_panes”, “speaker_only”, and “large_speaker”. The
	// default value is “speaker_with_strip”.
	// “speaker_only” is new in Cloud 4.6, API minor version 9.
	// The layouts are described in more detail below.
	public final Layout layout;

	// If true, the conference is silently deleted after 30 days. Use this feature
	// if the client must create a placeholder conference (in order to be allocated
	// a dial in number, for example) before the details can be finalized.
	public final boolean dummy;

	// If true, the conference is recorded.
	public final boolean recording;

	// If true, meeting will attempt RTMP streaming (correct stream URL and key will
	// be required)
	public final boolean rtmpEnable;

	// If rtmp_enable is true, this field is required. String will be filled into
	// the Stream URL field
	public final String rtmpUri;

	// This field is optional, regardless of rtmp_enable. String will be filled into
	// the Stream key field
	public final String rtmpToken;

	// The start date and time of the conference in ISO 8601 format (without time
	// zone). Must be null if the conference is permanent.
	public final String start;

	// The end date and time of the conference in ISO 8601 format (without time
	// zone). Must be null if the conference is permanent.
	public final String end;

	// The name of the conference time zone, as per the IANA Time Zone Database.
	// This must match one of the StarLeaf supported time zone names – see Timezone
	// for more details.
	public final String timezone;

	public final SLConferenceRepetition repetition;

	public SLConferenceSettings(String ownerId, String title, String description, boolean dummy, String start,
			String end, String timezone, SLConferenceRepetition repetition) {
		this(ownerId, title, description, Layout.speaker_with_strip, dummy, false, false, "", "", start, end, timezone,
				repetition);
	}

	public SLConferenceSettings(String ownerId, String title, String description, Layout layout, boolean dummy,
			boolean recording, boolean rtmpEnable, String rtmpUri, String rtmpToken, String start, String end,
			String timezone, SLConferenceRepetition repetition) {
		this.ownerId = ownerId;
		this.title = title;
		this.description = description;
		this.layout = layout;
		this.dummy = dummy;
		this.recording = recording;
		this.rtmpEnable = rtmpEnable;
		this.rtmpUri = rtmpUri;
		this.rtmpToken = rtmpToken;
		this.start = start;
		this.end = end;
		this.timezone = timezone;

		this.repetition = repetition;
	}

	public JsonObject asJson() {
		JsonObject ret = new JsonObject();
		ret.put("title", title);
		if (!Strings.isNullOrEmpty(description)) {
			ret.put("description", description);
		}

		if (layout != null) {
			ret.put("layout", layout.name());
		}

		ret.put("dummy", dummy);
		ret.put("recording", recording);
		ret.put("rtmp_enable", rtmpEnable);
		ret.put("rtmp_uri", rtmpUri);
		ret.put("rtmp_token", rtmpToken);

		ret.put("timezone", timezone);
		ret.put("hide_dir_entry", true);
		ret.put("require_owner", false);
		ret.put("permanent", false);

		ret.put("start", start);
		ret.put("end", end);

		if (repetition != null) {
			ret.put("repetition", repetition.asJson());
		}

		return ret;
	}

	public static final SLConferenceSettings fromJson(JsonObject json) {
		JsonObject settings = json.getJsonObject("settings");

		return new SLConferenceSettings(json.getString("owner_id"), settings.getString("title"),
				settings.getString("description"), Layout.valueOf(settings.getString("layout")),
				settings.getBoolean("dummy"), settings.getBoolean("recording"), settings.getBoolean("rtmp_enable"),
				settings.getString("rtmp_uri"), settings.getString("rtmp_token"), settings.getString("start"),
				settings.getString("end"), settings.getString("timezone"), SLConferenceRepetition.fromJson(json));
	}

	@Override
	public String toString() {
		return "SLConference [ownerId=" + ownerId + ", title=" + title + ", description=" + description + ", layout="
				+ layout + ", dummy=" + dummy + ", recording=" + recording + ", rtmpEnable=" + rtmpEnable + ", rtmpUri="
				+ rtmpUri + ", rtmpToken=" + rtmpToken + ", start=" + start + ", end=" + end + ", timezone=" + timezone
				+ "]";
	}

}
