<script setup>
import { computed } from "vue";
import EventCalendarIllustration from "./EventCalendarIllustration";

const props = defineProps({
    message: { type: Object, required: true },
    event: { type: Object, required: true }
});

const isOccurrence = computed(() => !!props.message.eventInfo.recuridIsoDate);
const eventKey = computed(() =>
    isOccurrence.value ? "mail.viewer.invitation.canceledOccurrence" : "mail.viewer.invitation.canceled"
);
const statusKey = computed(() =>
    isOccurrence.value ? "mail.viewer.invitationCanceled.statusOccurrence" : "mail.viewer.invitationCanceled.status"
);
</script>

<template>
    <div class="event-canceled">
        <div>
            <event-calendar-illustration illustration="calendar-removed" />
            <i18n :path="eventKey" tag="span" class="font-weight-bold event-canceled-header">
                <template #name>{{ message.from.dn }}</template>
                <template #status>
                    <span class="event-canceled-status">{{ $t(statusKey) }}</span>
                </template>
            </i18n>
        </div>
    </div>
</template>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";

.event-canceled {
    .event-canceled-header .event-canceled-status {
        color: $danger-fg-hi1;
    }
    > div {
        display: flex;
        flex-direction: row;
        align-items: center;
        gap: $sp-6;
        padding: $sp-4 $sp-5 $sp-4 $sp-5;
    }
}
</style>
