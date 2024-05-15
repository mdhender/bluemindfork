<template>
    <bm-toolbar-icon-dropdown
        class="align-button"
        variant="compact"
        size="lg"
        icon="align-left"
        :disabled="disabled"
        :text="$t('styleguide.rich_editor.align.tooltip')"
    >
        <align-left-button :editor="editor" :disabled="disabled" />
        <align-center-button :editor="editor" :disabled="disabled" />
        <align-right-button :editor="editor" :disabled="disabled" />
    </bm-toolbar-icon-dropdown>
</template>

<script>
import AlignLeftButton from "./AlignLeftButton";
import AlignCenterButton from "./AlignCenterButton";
import AlignRightButton from "./AlignRightButton";
import BmToolbarIconDropdown from "../../BmToolbar/BmToolbarIconDropdown";

export default {
    components: { AlignLeftButton, AlignCenterButton, AlignRightButton, BmToolbarIconDropdown },

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
