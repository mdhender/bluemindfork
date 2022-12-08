<template>
    <bm-button
        class="insert-as-link-button"
        :disabled="disabled"
        variant="outline"
        :title="title"
        :class="{ selected: insertAsLink }"
    >
        <bm-label-icon icon="cloud">
            {{ $t("chooser.link") }}
        </bm-label-icon>
    </bm-button>
</template>

<script>
import { mapState } from "vuex";
import { BmButton, BmLabelIcon } from "@bluemind/ui-components";

export default {
    name: "InsertAsLinkButton",
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
                ? this.$t("chooser.too_heavy.link")
                : this.$tc("chooser.attach.as_links", Object.keys(this.selectedFiles).length);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

.insert-as-link-button {
    &.selected {
        background-color: $neutral-bg-hi1 !important;
    }
}
</style>
