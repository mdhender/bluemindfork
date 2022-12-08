<template>
    <bm-icon-dropdown
        class="align-button"
        variant="compact"
        size="lg"
        icon="rich-editor-align-left"
        :disabled="disabled"
    >
        <bm-dropdown-item> <align-left-button :editor="editor" :disabled="disabled" /></bm-dropdown-item>
        <bm-dropdown-item> <align-center-button :editor="editor" :disabled="disabled" /></bm-dropdown-item>
        <bm-dropdown-item> <align-right-button :editor="editor" :disabled="disabled" /></bm-dropdown-item>
    </bm-icon-dropdown>
</template>

<script>
import BmIconDropdown from "../../dropdown/BmIconDropdown";
import AlignLeftButton from "./AlignLeftButton";
import AlignCenterButton from "./AlignCenterButton";
import AlignRightButton from "./AlignRightButton";
import BmDropdownItem from "../../dropdown/BmDropdownItem.vue";

export default {
    components: { AlignLeftButton, AlignCenterButton, AlignRightButton, BmIconDropdown, BmDropdownItem },
    props: {
        disabled: {
            type: Boolean,
            default: false
        },
        editor: {
            type: Object,
            required: true
        }
    }
};
</script>

<style lang="scss">
@import "../../../css/_variables.scss";

.align-button {
    .dropdown-toggle {
        display: flex;
        align-items: center;
    }
    .dropdown-menu {
        min-width: 0;
        padding: 0;
    }
    .dropdown-item {
        padding: 0;
        .btn {
            padding: $sp-2 $sp-3;
        }
    }
}
</style>
