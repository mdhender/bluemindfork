<script setup>
import { computed } from "vue";
import { BmIcon, BmRow, BmIllustration } from "@bluemind/ui-components";

const props = defineProps({
    illustration: { type: String, default: "calendar" },
    message: { type: Object, default: null },
    event: { type: Object, default: null }
});

// TODO
// const year = computed();
// const weekDay = computed();
// const day = computed();
// const month = computed();
// const iconStatus = computed();
// const iconRepeat = computed();
</script>

<template>
    <div class="event-calendar-illustration">
        <bm-illustration class="d-flex d-lg-none" :value="illustration" size="xxs" />
        <bm-illustration class="d-none d-lg-flex mx-5" :value="illustration" size="xs" />
        <!-- TODO -->
        <!-- <div v-if="year && weekDay && day && month" class="event-calendar-illustration-text">
            <div>{{ year }}</div>
            <div>{{ weekDay }}</div>
            <div>{{ day }}</div>
            <div>{{ month }}</div>
            <bm-icon v-if="iconStatus" :icon="iconStatus" class="illustration-icon-status" />
            <bm-icon v-if="iconRepeat" :icon="iconRepeat" class="illustration-icon-repeat" />
        </div> -->
    </div>
</template>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";
@import "~@bluemind/ui-components/src/css/utils/responsiveness";

.event-calendar-illustration {
    display: flex;
    align-items: center;

    @include until-lg {
        width: 75px;
        height: 75px;
    }
    @include from-lg {
        width: 120px;
        height: 100px;
    }
}
</style>
