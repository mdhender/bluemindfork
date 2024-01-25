<template>
    <div v-if="isInToolbar" :class="className" />
    <bm-dropdown-divider v-else :class="className" />
</template>

<script setup>
import { useToolbarContext } from "./toolbar";
import BmDropdownDivider from "../dropdown/BmDropdownDivider";

const { isInToolbar } = useToolbarContext();
const className = "bm-toolbar-divider";
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.bm-toolbar-divider {
    border-right: 1px $neutral-fg-lo3 solid;
}
</style>
