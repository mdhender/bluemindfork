<template>
    <div v-if="messageList" class="bm-rich-editor-status-bar text-truncate" :aria-label="$t(messageList)">
        <bm-icon icon="info-circle" size="xs" />
        <div class="text-truncate">{{ $t(`${messageList}`) }}</div>
    </div>
</template>

<script>
import BmIcon from "../BmIcon.vue";

const l10nBasePath = "styleguide.rich_editor.hint.";

export default {
    name: "BmRichEditorStatusBar",
    components: { BmIcon },
    props: {
        editor: { type: Object, required: true }
    },
    data() {
        return {
            plugin: this.editor.core.plugins.find(plugin => plugin.getName() === "StatusBarPlugin")
        };
    },
    computed: {
        messageList() {
            return this.plugin.hasMessage ? (l10nBasePath + this.plugin.message.toLowerCase()).toString() : "";
        }
    }
};
</script>

<style lang="scss">
@import "../../css/_type.scss";
@import "../../css/_variables.scss";

.bm-rich-editor-status-bar {
    display: flex;
    align-items: center;
    gap: $sp-3;
    height: $hint-height;
    @extend %caption;
    padding: 0 $sp-4;
    color: $neutral-fg-lo1;
    background: $surface;
}
</style>
