<template>
    <div class="bm-calendar-item d-flex align-items-center">
        <bm-color-badge
            size="lg"
            :value="calendar.settings.bm_color || 'transparent'"
            transparent-variant="dashed-border"
        />
        <span class="text-truncate">{{ calendar.name }}</span>
    </div>
</template>

<script>
import { BmColorBadge } from "@bluemind/ui-components";

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
@import "~@bluemind/ui-components/src/css/utils/variables";

.bm-calendar-item {
    .bm-color-badge {
        margin-right: $sp-4;
        margin-bottom: base-px-to-rem(2);
    }
}
</style>
