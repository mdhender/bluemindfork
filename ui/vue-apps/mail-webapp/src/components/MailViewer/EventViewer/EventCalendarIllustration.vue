<script setup>
import { computed } from "vue";
import { mapState } from "vuex";
import { BmIllustration } from "@bluemind/ui-components";
import EventHelper from "~/store/helpers/EventHelper";

defineProps({ illustration: { type: String, default: "calendar" } });
</script>

<template>
    <div class="event-calendar-illustration">
        <bm-illustration class="d-flex d-lg-none" :value="illustration" size="xxs" />
        <bm-illustration class="d-none d-lg-flex mx-5" :value="illustration" size="xs" />
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
