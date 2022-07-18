<template>
    <bm-button
        variant="inline-neutral"
        class="preview-button p-0"
        size="md"
        :title="$t('mail.preview.open')"
        :disabled="disabled"
        @click.stop="preview"
    >
        <bm-icon icon="eye" size="2x" class="p-1" />
    </bm-button>
</template>

<script>
import { BmIcon, BmButton } from "@bluemind/styleguide";

export default {
    name: "PreviewButton",
    components: { BmIcon, BmButton },
    props: {
        disabled: {
            type: Boolean,
            default: false
        }
    },
    methods: {
        preview() {
            if (!this.disabled) {
                this.$emit("preview");
            }
        }
    }
};
</script>

<style></style>
