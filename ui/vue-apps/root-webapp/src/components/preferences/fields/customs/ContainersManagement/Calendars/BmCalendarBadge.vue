<template>
    <bm-badge pill :closeable="closeable" class="bm-calendar-badge" @close="$emit('close')">
        <bm-calendar-item :calendar="calendar" />
    </bm-badge>
</template>

<script>
import BmCalendarItem from "./BmCalendarItem";
import { BmBadge } from "@bluemind/styleguide";

export default {
    name: "BmCalendarBadge",
    components: { BmBadge, BmCalendarItem },
    props: {
        calendar: {
            type: Object,
            required: true
        },
        closeable: {
            type: Boolean,
            default: false
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_type";
@import "~@bluemind/styleguide/css/_variables";

.badge.bm-calendar-badge {
    padding: 0;
    padding-right: $sp-4;
    gap: $sp-4;
    @extend %caption-bold;

    .bm-color-badge {
        margin: base-px-to-rem(-3);
        margin-right: $sp-4;
    }
}
</style>
