<template>
    <bm-icon-button
        variant="compact"
        class="preview-button"
        size="sm"
        :icon="matchingPreviewIcon(file)"
        :title="$t('mail.preview.open')"
        :disabled="disabled"
        @click.stop="preview"
    />
</template>

<script>
import { BmIconButton } from "@bluemind/ui-components";
import { MimeType } from "@bluemind/email";

export default {
    name: "PreviewButton",
    components: { BmIconButton },
    props: {
        disabled: {
            type: Boolean,
            default: false
        },
        file: {
            type: Object,
            required: true
        }
    },
    methods: {
        preview() {
            if (!this.disabled) {
                this.$emit("preview");
            }
        },
        matchingPreviewIcon(file) {
            return MimeType.isAudio(file) || MimeType.isVideo(file) ? "play" : "eye";
        }
    }
};
</script>
