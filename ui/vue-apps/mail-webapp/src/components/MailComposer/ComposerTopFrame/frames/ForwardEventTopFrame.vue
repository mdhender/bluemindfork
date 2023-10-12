<template>
    <chain-of-responsibility :is-responsible="hasEvent">
        <event-detail v-if="hasEvent" class="event-details m-4" :event="event" :message="message" />
    </chain-of-responsibility>
</template>

<script>
import { inject } from "@bluemind/inject";
import { messageUtils, attachmentUtils } from "@bluemind/mail";
import ChainOfResponsibility from "../../../ChainOfResponsibility";
import EventDetail from "../../../MailViewer/EventViewer/EventDetail";
import EventHelper from "~/store/helpers/EventHelper";

import MimeType from "@bluemind/email/src/MimeType";
const { hasCalendarPart, computeParts } = messageUtils;

export default {
    name: "ForwardEventTopFrame",
    components: { ChainOfResponsibility, EventDetail },
    props: {
        message: { type: Object, required: true },
        attachments: { type: Array, required: true }
    },
    data() {
        return {
            event: {}
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
            const ics = await this.fetchIcsToText();
            const event = await this.retreiveCalendarEvent(this.message, this.retreiveUid(ics));
            this.event = EventHelper.adapt(
                event,
                this.message.eventInfo.resourceUid,
                this.message.from.address,
                this.message.eventInfo.recuridIsoDate
            );
        }
    },
    methods: {
        async retreiveCalendarEvent(message, icsUid) {
            if (message.eventInfo.isResourceBooking) {
                return inject("CalendarPersistence", "calendar:" + message.eventInfo.resourceUid).getComplete(icsUid);
            }
            const events = await inject("CalendarPersistence").getByIcsUid(icsUid);
            return EventHelper.findEvent(events, message.eventInfo.recuridIsoDate);
        },

        retreiveUid(ics) {
            const UUID_REGEX = /UID:(\S+)/i;
            return ics.match(UUID_REGEX)?.[1];
        },

        async fetchIcsToText() {
            const res = await fetch(encodeURI(this.icsFileUrl));
            return res.text();
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.event-details {
    padding: $sp-5 0 $sp-4 0;
    background-color: $neutral-bg-lo1;
}
</style>
