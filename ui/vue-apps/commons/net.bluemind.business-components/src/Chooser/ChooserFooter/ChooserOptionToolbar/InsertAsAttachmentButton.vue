<template>
    <bm-button
        class="insert-as-attachment-button"
        :class="{ selected: !insertAsLink }"
        :disabled="disabled"
        :title="title"
        variant="outline"
    >
        <bm-label-icon icon="paper-clip">
            {{ $t("common.attachment") }}
        </bm-label-icon>
    </bm-button>
</template>

<script>
import { BmButton, BmLabelIcon } from "@bluemind/styleguide";
import { mapState } from "vuex";

export default {
    name: "InsertAsAttachmentButton",
    components: { BmButton, BmLabelIcon },
    props: {
        disabled: {
            type: Boolean,
            default: false
        }
    },
    computed: {
        ...mapState("chooser", ["selectedFiles", "insertAsLink"]),
        title() {
            return this.disabled
                ? this.$t("chooser.too_heavy.attachment")
                : this.$tc("chooser.attach.as_attachments", Object.keys(this.selectedFiles).length);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.insert-as-attachment-button {
    &.selected {
        background-color: $neutral-bg-hi1 !important;
    }
}
</style>
