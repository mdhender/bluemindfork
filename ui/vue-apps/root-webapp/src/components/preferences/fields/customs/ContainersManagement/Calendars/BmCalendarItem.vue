<template>
    <div class="bm-calendar-item d-flex align-items-center">
        <bm-color-badge size="lg" :value="calendar.settings.bm_color || 'transparent'" no-cross-line />
        <span class="text-truncate">{{ calendar.name }}</span>
    </div>
</template>

<script>
import { BmColorBadge } from "@bluemind/styleguide";

export default {
    name: "BmCalendarItem",
    components: { BmColorBadge },
    props: {
        calendar: {
            type: Object,
            required: true
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.bm-calendar-item {
    .bm-color-badge {
        flex: none;
        margin-right: $sp-4;
        margin-bottom: base-px-to-rem(2);
    }
}
</style>
