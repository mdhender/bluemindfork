<script setup>
import EventCalendarIllustration from "./EventCalendarIllustration";
import { BmRow } from "@bluemind/ui-components";
</script>

<template>
    <div class="event-not-found">
        <div>
            <event-calendar-illustration illustration="calendar-error" />
            <div>Event not found</div>
        </div>
    </div>
</template>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";
@import "~@bluemind/ui-components/src/css/utils/responsiveness";

.event-not-found {
    > div {
        display: flex;
        flex-direction: row;
        align-items: center;
        gap: $sp-6;
        padding: 0 $sp-5 0px $sp-5;
        @include until-lg {
            padding: $sp-5;
        }
    }
}
</style>
