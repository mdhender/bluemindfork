<template>
    <bm-toolbar-element>
        <template #toolbar>
            <div :class="className" class="vertical-divider" />
        </template>
        <template #menu>
            <bm-dropdown-divider :class="className" />
        </template>
    </bm-toolbar-element>
</template>

<script setup>
import BmDropdownDivider from "../dropdown/BmDropdownDivider";
import BmToolbarElement from "./BmToolbarElement";

const className = "bm-toolbar-divider";
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.bm-toolbar-divider {
    &.vertical-divider {
        border-right: 1px $neutral-fg-lo3 solid;
    }
}
</style>
