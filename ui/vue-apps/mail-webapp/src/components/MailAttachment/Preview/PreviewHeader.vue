<template>
    <div class="preview-header bg-light">
        <preview-message-header
            class="bg-surface"
            :expanded="expanded"
            @click.native="$emit('update:expanded', !expanded)"
        />
        <preview-attachment-header :part="part" />
        <div class="d-flex align-items-center">
            <bm-button variant="inline" @click="$emit('previous')"><bm-icon icon="chevron-left" size="lg" /></bm-button>
            <bm-button variant="inline" @click="$emit('next')"><bm-icon icon="chevron-right" size="lg" /></bm-button>
            <bm-button-close class="mx-2" size="lg" @click="$emit('close')" />
        </div>
    </div>
</template>

<script>
import { BmButtonClose, BmIcon, BmButton } from "@bluemind/styleguide";
import PreviewAttachmentHeader from "./PreviewAttachmentHeader";
import PreviewMessageHeader from "./PreviewMessageHeader";

export default {
    name: "PreviewHeader",
    components: { PreviewMessageHeader, PreviewAttachmentHeader, BmButtonClose, BmIcon, BmButton },
    props: {
        part: {
            type: Object,
            required: true
        },
        expanded: { type: Boolean, required: true }
    },
    methods: {}
};
</script>

<style lang="scss">
.preview-header {
    display: flex;
}
</style>
