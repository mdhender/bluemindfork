<script setup>
import { BmIllustration } from "@bluemind/ui-components";
</script>

<template>
    <div class="event-not-found">
        <bm-illustration class="d-flex d-lg-none flex-shrink-0" over-background value="calendar-error" size="xxs" />
        <bm-illustration class="d-none d-lg-flex mx-5" over-background value="calendar-error" size="xs" />
        <div>
            <div class="bold mb-4">{{ $t("mail.viewer.invitation.not_found.title") }}</div>
            <div class="regular">{{ $t("mail.viewer.invitation.not_found.description") }}</div>
        </div>
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
    .bm-illustration {
        flex: none;
    }
    .event-not-found-status {
        color: $danger-fg-hi1;
    }
}
</style>
