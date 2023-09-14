<script setup>
import { computed } from "vue";
import EventHeader from "./EventHeader";
import EventDetail from "./EventDetail";
import EventFooter from "./EventFooter";
import { REPLY_ACTIONS } from "./replyActions";

const props = defineProps({
    message: { type: Object, required: true },
    event: { type: Object, required: true }
});

const fromAttendee = computed(() =>
    props.event.attendees?.find(attendee => attendee.mail === props.message.from.address)
);
const isOccurrence = computed(() => !!props.message.eventInfo.recuridIsoDate);

const statusKeyForOccurrence = {
    [REPLY_ACTIONS.ACCEPTED]: "mail.viewer.invitation.reply.status.occurrence.accepted",
    [REPLY_ACTIONS.TENTATIVE]: "mail.viewer.invitation.reply.status.occurrence.tentative",
    [REPLY_ACTIONS.DECLINED]: "mail.viewer.invitation.reply.status.occurrence.declined"
};
const statusKeyForEvent = {
    [REPLY_ACTIONS.ACCEPTED]: "mail.viewer.invitation.reply.status.event.accepted",
    [REPLY_ACTIONS.TENTATIVE]: "mail.viewer.invitation.reply.status.event.tentative",
    [REPLY_ACTIONS.DECLINED]: "mail.viewer.invitation.reply.status.event.declined"
};
const eventKey = computed(() =>
    isOccurrence.value ? "mail.viewer.invitation.reply.occurrence" : "mail.viewer.invitation.reply.event"
);
const statusKey = computed(
    () => (isOccurrence.value ? statusKeyForOccurrence : statusKeyForEvent)[fromAttendee.value.status]
);
</script>

<template>
    <div class="event-replied">
        <event-header v-if="fromAttendee && statusKey">
            <i18n :path="eventKey" tag="span" class="font-weight-bold event-replied-header">
                <template #name>{{ fromAttendee.name }}</template>
                <template #status>
                    <span :class="`event-replied-status event-replied-status-${fromAttendee.status.toLowerCase()}`">{{
                        $t(statusKey)
                    }}</span>
                </template>
            </i18n>
        </event-header>
        <event-detail :event="event" :message="message" />
        <event-footer :event="event" />
    </div>
</template>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.event-replied {
    display: flex;
    flex-direction: column;
    gap: $sp-4;

    .event-replied-header {
        .event-replied-status-accepted {
            color: $success-fg-hi1;
        }
        .event-replied-status-tentative {
            color: $warning-fg-hi1;
        }
        .event-replied-status-declined {
            color: $danger-fg-hi1;
        }
    }
}
</style>
