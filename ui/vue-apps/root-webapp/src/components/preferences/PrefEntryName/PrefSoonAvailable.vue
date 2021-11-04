<template>
    <div class="d-flex align-items-center">
        <span class="h2 pr-2" :class="{ 'text-alternate-light': entry.disabled }">{{ entry.name }}</span>
        <span v-if="entry.disabled" class="available-soon h2">{{ $t("common.available_soon") }}</span>
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

.available-soon {
    color: color-yiq($yellow);
    background-color: $yellow;
    &::before,
    &::after {
        content: "\00a0";
    }
}
</style>
