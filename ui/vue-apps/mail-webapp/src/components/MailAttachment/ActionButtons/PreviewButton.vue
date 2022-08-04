<template>
    <bm-icon-button
        variant="compact"
        class="preview-button"
        size="sm"
        icon="eye"
        :title="$t('mail.preview.open')"
        :disabled="disabled"
        @click.stop="preview"
    />
</template>

<script>
import { BmIconButton } from "@bluemind/styleguide";

export default {
    name: "PreviewButton",
    components: { BmIconButton },
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
