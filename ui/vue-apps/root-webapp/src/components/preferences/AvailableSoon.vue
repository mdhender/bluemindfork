<template>
    <span class="available-soon bold">{{ $t("common.available_soon") }}</span>
</template>

<script>
export default {
    name: "AvailableSoon"
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.available-soon {
    color: $fill-tertiary-fg;
    background-color: $fill-tertiary-bg;
    &::before,
    &::after {
        content: "\00a0";
    }
}
</style>
