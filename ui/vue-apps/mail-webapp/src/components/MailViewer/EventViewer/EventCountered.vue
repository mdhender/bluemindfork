<script setup>
import { computed } from "vue";
import store from "@bluemind/store";
import { BmToggleableButton } from "@bluemind/ui-components";
import { ACCEPT_COUNTER_EVENT, DECLINE_COUNTER_EVENT } from "~/actions";
import EventHeader from "./EventHeader";
import EventDetail from "./EventDetail";
import EventFooter from "./EventFooter";
import { STATUS_KEY_FOR_OCCURRENCE, STATUS_KEY_FOR_EVENT } from "./replyActions";

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
</script>

<template>
    <div class="event-request">
        <event-header v-if="event.counter">
            <i18n :path="eventKey" tag="span">
                <template #name>{{ fromAttendee.name }}</template>
                <template #status>
                    <span
                        :class="`event-countered-status-${fromAttendee.status.toLowerCase()}`"
                        class="font-weight-bold event-countered-header"
                        >{{ $t(statusKey) }}</span
                    >
                </template>
                <template #counter>
                    <span class="font-weight-bold event-countered-status-countered">{{
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

        <event-header v-else>
            {{ $t("mail.viewer.invitation.counter.answered") }}
        </event-header>

        <event-detail :event="event" :message="message" />
        <event-footer :event="event" />
    </div>
</template>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.event-request {
    display: flex;
    flex-direction: column;
    gap: $sp-4;

    .counter-buttons {
        flex-basis: 100%;
        display: flex;
        gap: $sp-6;
        flex-wrap: wrap;

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
