<script setup>
import EventHeader from "./base/EventHeader";
import EventDetail from "./base/EventDetail";

defineProps({
    event: { type: Object, required: true },
    message: { type: Object, required: true }
});
</script>

<template>
    <div class="event-counter-decline">
        <event-header>
            <i18n path="mail.viewer.invitation.counter.declined">
                <template #status>
                    <span class="counter-decline-status">{{
                        $t("mail.viewer.invitation.counter.declined.status")
                    }}</span>
                </template>
            </i18n>
        </event-header>
        <event-detail :event="event" :message="message" />
    </div>
</template>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.event-counter-decline {
    display: flex;
    flex-direction: column;
    gap: $sp-4;
    .counter-decline-status {
        color: $danger-fg-hi1;
    }
}
</style>
