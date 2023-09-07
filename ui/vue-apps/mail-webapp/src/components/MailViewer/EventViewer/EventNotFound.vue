<script setup>
import EventCalendarIllustration from "./EventCalendarIllustration.vue";
</script>

<template>
    <div class="event-not-found">
        <event-calendar-illustration illustration="calendar-error" />
        <i18n path="mail.viewer.invitation.not_found.event" tag="div" class="font-weight-bold">
            <template #status>
                <span class="event-not-found-status">{{ $t("mail.viewer.invitation.not_found.status") }}</span>
            </template>
        </i18n>
    </div>
</template>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";
@import "~@bluemind/ui-components/src/css/utils/responsiveness";

.event-not-found {
    display: flex;
    flex-direction: row;
    align-items: center;
    gap: $sp-6;
    padding: $sp-4 $sp-5 0px $sp-5;
    .event-not-found-status {
        color: $danger-fg-hi1;
    }
}
</style>
