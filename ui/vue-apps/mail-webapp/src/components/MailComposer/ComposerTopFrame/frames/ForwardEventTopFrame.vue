<template>
    <chain-of-responsibility :is-responsible="hasEvent">
        <event-not-found v-if="error" class="event-details m-4" />
        <event-detail v-else-if="hasEvent && event" class="event-details m-4" :event="event" :message="message" />
    </chain-of-responsibility>
</template>

<script>
import { inject } from "@bluemind/inject";
import { messageUtils, attachmentUtils, partUtils } from "@bluemind/mail";
import ChainOfResponsibility from "../../../ChainOfResponsibility";
import EventDetail from "../../../MailViewer/EventViewer/EventDetail";
import EventNotFound from "../../../MailViewer/EventViewer/EventNotFound";
import EventHelper from "~/store/helpers/EventHelper";

import MimeType from "@bluemind/email/src/MimeType";
const { getCalendarParts } = messageUtils;
const { getPartsFromCapabilities, hasCalendarPart } = partUtils;
export default {
    name: "ForwardEventTopFrame",
    components: { ChainOfResponsibility, EventDetail, EventNotFound },
    props: {
        message: { type: Object, required: true },
        attachments: { type: Array, required: true }
    },
    data() {
        return {
            event: {},
            error: false
        };
    },
    computed: {
        hasEvent() {
            return hasCalendarPart(this.message.structure);
        },
        icsAttachments() {
            return this.attachments.filter(a => a.mime === MimeType.ICS);
        },
        extractedFiles() {
            return attachmentUtils.AttachmentAdaptor.extractFiles(this.icsAttachments, this.message);
        },
        icsFileUrl() {
            return this.extractedFiles?.find(f => f.url)?.url ?? "";
        }
    },
    async created() {
        if (this.hasEvent) {
            try {
                const event = await this.retreiveEventFromCalendarPart(this.message);
                this.event = EventHelper.adapt(
                    event,
                    this.message.eventInfo.resourceUid,
                    this.message.from.address,
                    this.message.eventInfo.recuridIsoDate
                );

                this.error = false;
            } catch {
                this.error = true;
            }
        }
    },
    methods: {
        async retreiveEventFromCalendarPart(message) {
            const part = getCalendarParts(message.structure).pop();
            const blob = await inject("MailboxItemsPersistence", message.folderRef.uid).fetch(
                message.remoteRef.imapUid,
                part.address,
                part.encoding,
                part.mime,
                part.charset
            );
            const text = await blob.text();
            return this.retreiveCalendarEvent(message, this.retreiveUid(text));
        },

        async retreiveCalendarEvent(message, icsUid) {
            if (message.eventInfo.isResourceBooking) {
                return inject("CalendarPersistence", "calendar:" + message.eventInfo.resourceUid).getComplete(icsUid);
            }
            const events = await inject("CalendarPersistence").getByIcsUid(icsUid);
            return EventHelper.findEvent(events, message.eventInfo.recuridIsoDate);
        },

        retreiveUid(textContent) {
            return readTextPropertyValue(textContent, "UID");
        }
    }
};

function readTextPropertyValue(input, property) {
    let len = input.length;
    let begin = input.search(new RegExp(`^${property}\\S*:`, "mi"));
    begin = input.indexOf(":", begin);
    let end,
        line = "";
    do {
        end = input.indexOf("\n", begin) + 1 || len;
        line += input.slice(begin + 1, end).replace(/\r?\n/g, "");
        if (input.charAt(end) !== " " && input.charAt(end) !== "\t") {
            return line.trim();
        }
        begin = end;
    } while (end < len && end > 0);
    return line.trim();
}
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.event-details {
    padding: $sp-5 0 $sp-4 0;
    background-color: $neutral-bg-lo1;
}
</style>
