<script setup>
import { computed } from "vue";
import EventHeader from "./EventHeader";
import EventDetail from "./EventDetail";
import EventFooter from "./EventFooter";
import { STATUS_KEY_FOR_OCCURRENCE, STATUS_KEY_FOR_EVENT, REPLY_ACTIONS } from "./replyActions";

const props = defineProps({
    message: { type: Object, required: true },
    event: { type: Object, required: true }
});

const fromAttendee = computed(() =>
    props.event.attendees?.find(attendee => attendee.mail === props.message.from.address)
);
const isOccurrence = computed(() => !!props.message.eventInfo.recuridIsoDate);

const status = computed(() => fromAttendee.value.status);

const eventKey = computed(() => {
    const key = isOccurrence.value ? "mail.viewer.invitation.reply.occurrence" : "mail.viewer.invitation.reply.event";
    return status.value === REPLY_ACTIONS.NEEDS_ACTION ? `${key}.not` : key;
});
const statusKey = computed(() => (isOccurrence.value ? STATUS_KEY_FOR_OCCURRENCE : STATUS_KEY_FOR_EVENT)[status.value]);
</script>

<template>
    <div class="event-replied">
        <event-header v-if="fromAttendee && statusKey">
            <i18n :path="eventKey" tag="div" class="bold event-replied-header">
                <template #name>{{ fromAttendee.name }}</template>
                <template #status>
                    <span :class="`event-replied-status event-replied-status-${status.toLowerCase()}`">
                        {{ $t(statusKey) }}
                    </span>
                </template>
            </i18n>
        </event-header>
        <event-detail :event="event" :message="message" />
    </div>
</template>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.event-replied {
    display: flex;
    flex-direction: column;

    .event-replied-header {
        .event-replied-status-accepted {
            color: $success-fg-hi1;
        }
        .event-replied-status-needsaction {
            color: $info-fg-hi1;
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
