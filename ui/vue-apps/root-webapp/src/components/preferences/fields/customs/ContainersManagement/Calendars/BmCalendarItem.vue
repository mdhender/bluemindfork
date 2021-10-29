<template>
    <div class="bm-calendar-item d-flex align-items-center">
        <bm-color-badge v-if="calendar.settings.bm_color" :value="calendar.settings.bm_color" class="mr-1" />
        <div v-else class="empty d-inline-block" />
        {{ calendar.name }}
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

<style>
.bm-calendar-item div.empty {
    width: 20px;
    height: 20px;
}
</style>
