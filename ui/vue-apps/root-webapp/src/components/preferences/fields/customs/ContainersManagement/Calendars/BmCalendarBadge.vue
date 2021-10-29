<template>
    <h1 class="d-inline">
        <bm-badge pill :closeable="closeable" class="mr-2 align-items-center" @close="$emit('close')">
            <bm-calendar-item :calendar="calendar" />
        </bm-badge>
    </h1>
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
