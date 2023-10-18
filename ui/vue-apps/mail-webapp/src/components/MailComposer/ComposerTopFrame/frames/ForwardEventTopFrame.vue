<template>
    <chain-of-responsibility :is-responsible="hasEvent">
        <event-not-found v-if="error" class="event-details m-4" />
        <event-detail v-else-if="hasEvent && event" class="event-details m-4" :event="event" :message="message" />
    </chain-of-responsibility>
</template>

<script>
import { mapState } from "vuex";
import { inject } from "@bluemind/inject";
import { messageUtils, attachmentUtils } from "@bluemind/mail";
import MimeType from "@bluemind/email/src/MimeType";
import EventHelper from "~/store/helpers/EventHelper";
import { SET_CURRENT_EVENT } from "~/mutations";
import ChainOfResponsibility from "../../../ChainOfResponsibility";
import EventDetail from "../../../MailViewer/EventViewer/base/EventDetail";
import EventNotFound from "../../../MailViewer/EventViewer/EventNotFound";

const { hasCalendarPart, computeParts, getCalendarParts } = messageUtils;

export default {
    name: "ForwardEventTopFrame",
    components: { ChainOfResponsibility, EventDetail, EventNotFound },
    props: {
        message: { type: Object, required: true },
        attachments: { type: Array, required: true }
    },
    data() {
        return {
            error: false
        };
    },
    computed: {
        ...mapState("mail", {
            event: state => state.consultPanel.currentEvent
        }),
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
                const [event, recurid] = await this.retreiveEventFromCalendarPart(this.message);
                this.$store.commit(
                    `mail/${SET_CURRENT_EVENT}`,
                    EventHelper.adapt(
                        event,
                        this.message.eventInfo.resourceUid,
                        this.message.from.address,
                        recurid ?? this.message.eventInfo.recuridIsoDate
                    )
                );
                this.error = false;
            } catch {
                this.$store.commit(`mail/${SET_CURRENT_EVENT}`, {});
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

            const event = await this.retreiveCalendarEvent(message, this.retreiveUid(text));
            const textRecurid = this.retreiveRecurid(text);

            return [
                event,
                textRecurid
                    ? event.value.occurrences.find(occ => occ.recurid.iso8601.startsWith(textRecurid)).recurid.iso8601
                    : null
            ];
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
        },
        retreiveRecurid(textContent) {
            const textRecurid = readTextPropertyValue(textContent, "RECURRENCE-ID");
            return textRecurid
                ? textRecurid.replace(/(\d{4})(\d{2})(\d{2})T(\d{2})(\d{2})(\d{2})(\d*)?/, "$1-$2-$3T$4:$5:$6")
                : null;
        }
    }
};

function readTextPropertyValue(input, property) {
    let len = input.length;
    let begin = input.search(new RegExp(`^${property}\\S*:`, "mi"));
    if (begin === -1) {
        return;
    }
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
