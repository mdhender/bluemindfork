<script setup>
import { computed } from "vue";
import { mapState } from "vuex";
import { BmIcon, BmRow, BmIllustration } from "@bluemind/ui-components";
import EventHelper from "~/store/helpers/EventHelper";
import EventCalendarIllustration from "./EventCalendarIllustration";

defineProps({ illustration: { type: String, default: "calendar" } });
</script>

<template>
    <div class="event-error-message">
        <event-calendar-illustration :illustration="illustration" />
        <div class="event-message">
            <slot />
        </div>
    </div>
</template>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";
@import "~@bluemind/ui-components/src/css/utils/responsiveness";

.event-error-message {
    display: flex;
    padding: 0 $sp-5 0px $sp-5;

    .event-message {
        color: $primary-fg-hi1;
        padding-top: $sp-3;
    }
}
</style>
