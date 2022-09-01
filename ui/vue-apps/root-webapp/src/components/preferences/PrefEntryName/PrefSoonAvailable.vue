<template>
    <div class="pref-soon-available d-flex align-items-center">
        <span class="h3 pr-5" :class="{ 'entry-disabled': entry.disabled }">{{ entry.name }}</span>
        <span v-if="entry.disabled" class="available-soon bold">{{ $t("common.available_soon") }}</span>
    </div>
</template>
<script>
export default {
    name: "PrefSoonAvailable",
    props: {
        entry: {
            type: Object,
            required: true
        }
    }
};
</script>
<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

.pref-soon-available {
    .entry-disabled {
        color: $neutral-fg-disabled;
    }
}
.available-soon {
    color: $fill-tertiary-fg;
    background-color: $fill-tertiary-bg;
    &::before,
    &::after {
        content: "\00a0";
    }
}
</style>
