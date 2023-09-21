<script setup>
import { computed } from "vue";
import store from "@bluemind/store";
import { BmToggleableButton } from "@bluemind/ui-components";
import { messageUtils } from "@bluemind/mail";
import { ACCEPT_COUNTER_EVENT, DECLINE_COUNTER_EVENT } from "~/actions";
import EventHeader from "./EventHeader";
import EventDetail from "./EventDetail";
import EventFooter from "./EventFooter";
import { STATUS_KEY_FOR_OCCURRENCE, STATUS_KEY_FOR_EVENT } from "./replyActions";
const { MessageHeader } = messageUtils;

const props = defineProps({
    message: { type: Object, required: true },
    event: { type: Object, required: true }
});

const acceptCounterEvent = () => store.dispatch(`mail/${ACCEPT_COUNTER_EVENT}`);
const declineCounterEvent = () => store.dispatch(`mail/${DECLINE_COUNTER_EVENT}`);

const fromAttendee = computed(() =>
    props.event.attendees?.find(attendee => attendee.mail === props.message.from.address)
);
const isOccurrence = computed(() => !!props.message.eventInfo.recuridIsoDate);

const eventKey = computed(() =>
    isOccurrence.value ? "mail.viewer.invitation.counter.occurrence" : "mail.viewer.invitation.counter.event"
);

const statusKey = computed(
    () => (isOccurrence.value ? STATUS_KEY_FOR_OCCURRENCE : STATUS_KEY_FOR_EVENT)[fromAttendee.value.status]
);
const attendeeHeader = computed(() =>
    props.message?.headers?.find(({ name }) => name.toUpperCase() === MessageHeader.X_BM_COUNTER_ATTENDEE.toUpperCase())
);
</script>

<template>
    <div class="event-countered">
        <event-header v-if="event.counter && event.isWritable">
            <i18n :path="eventKey" tag="div" class="bold">
                <template #name>{{ fromAttendee.name }}</template>
                <template #status>
                    <span
                        :class="`event-countered-status-${fromAttendee.status.toLowerCase()}`"
                        class="event-countered-header"
                        >{{ $t(statusKey) }}</span
                    >
                </template>
                <template #counter>
                    <span class="event-countered-status-countered">{{
                        $t("mail.viewer.invitation.counter.status")
                    }}</span>
                </template>
            </i18n>

            <template #actions>
                <div class="counter-buttons">
                    <bm-toggleable-button icon="check" @click="acceptCounterEvent">
                        {{ $t("mail.viewer.invitation.counter.accept") }}
                    </bm-toggleable-button>
                    <bm-toggleable-button icon="cross" @click="declineCounterEvent">
                        <span class="d-lg-none">{{ $t("mail.viewer.invitation.counter.decline_short") }}</span>
                        <span class="d-none d-lg-inline">{{ $t("mail.viewer.invitation.counter.decline") }}</span>
                    </bm-toggleable-button>
                </div>
            </template>
        </event-header>

        <event-header v-else-if="attendeeHeader">
            <span class="bold"> {{ $t("mail.viewer.invitation.counter.attendees") }}</span>
        </event-header>
        <event-header v-else-if="!event.counter">
            <span class="bold"> {{ $t("mail.viewer.invitation.counter.answered") }}</span>
        </event-header>

        <event-detail :event="event" :message="message" />
        <event-footer :event="event" />
    </div>
</template>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.event-countered {
    display: flex;
    flex-direction: column;
    gap: $sp-4;

    .counter-buttons {
        display: flex;
        gap: $sp-4 ($sp-5 + $sp-3 + $sp-2);
        flex-wrap: wrap;

        .bm-toggleable-button {
            min-width: 0;
            flex: 0 1 auto;
        }

        .b-skeleton-button {
            height: base-px-to-rem(30);
        }
    }

    .event-countered-status-tentative {
        color: $warning-fg-hi1;
    }
    .event-countered-status-declined {
        color: $danger-fg-hi1;
    }
    .event-countered-status-countered {
        color: $info-fg-hi1;
    }
}
</style>
