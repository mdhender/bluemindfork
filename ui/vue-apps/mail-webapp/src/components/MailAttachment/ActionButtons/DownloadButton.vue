<template>
    <bm-icon-button
        ref="download-button"
        variant="compact"
        class="download-button"
        size="sm"
        icon="download"
        :title="$t('common.downloadAttachment')"
        :href="file.url"
        :download="file.name"
        :disabled="disabled"
        @click.stop
    />
</template>

<script>
import { BmIconButton } from "@bluemind/styleguide";

export default {
    name: "DownloadButton",
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
        clickButton() {
            this.$refs["download-button"].$el.click();
        }
    }
};
</script>
