<template>
    <bm-button
        ref="download-button"
        variant="inline-neutral"
        class="download-button p-0"
        size="md"
        :title="$t('common.downloadAttachment')"
        :href="file.url"
        :download="file.name"
        :disabled="disabled"
        @click.stop
    >
        <bm-icon icon="download" size="2x" class="p-1" />
    </bm-button>
</template>

<script>
import { BmIcon, BmButton } from "@bluemind/styleguide";

export default {
    name: "DownloadButton",
    components: { BmIcon, BmButton },
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

<style></style>
