<script setup>
import { computed } from "vue";
import { BmIllustration } from "@bluemind/ui-components";

const props = defineProps({ message: { type: Object, required: true } });

const isOccurrence = computed(() => !!props.message.eventInfo.recuridIsoDate);
const eventKey = computed(() =>
    isOccurrence.value ? "mail.viewer.invitation.canceled.occurrence" : "mail.viewer.invitation.canceled.event"
);
const statusKey = computed(() =>
    isOccurrence.value ? "mail.viewer.invitation.canceled.status.occurrence" : "mail.viewer.invitation.canceled.status"
);
</script>

<template>
    <div class="event-canceled">
        <bm-illustration class="d-flex d-lg-none flex-shrink-0" over-background value="calendar-removed" size="xxs" />
        <bm-illustration class="d-none d-lg-flex mx-5" over-background value="calendar-removed" size="xs" />
        <i18n :path="eventKey" tag="span" class="bold event-canceled-text">
            <template #name>{{ message.from.dn }}</template>
            <template #status>
                <span class="event-canceled-status">{{ $t(statusKey) }}</span>
            </template>
        </i18n>
    </div>
</template>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";

.event-canceled {
    .event-canceled-text .event-canceled-status {
        color: $danger-fg-hi1;
    }
    display: flex;
    flex-direction: row;
    align-items: center;
    gap: $sp-6;
    padding: $sp-4 $sp-5 0 $sp-5;
}
</style>
