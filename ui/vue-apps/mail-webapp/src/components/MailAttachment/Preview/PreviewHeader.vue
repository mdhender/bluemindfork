<template>
    <div class="preview-header bg-light">
        <preview-message-header
            class="bg-surface"
            :expanded="expanded"
            @click.native="$emit('update:expanded', !expanded)"
        />
        <preview-attachment-header :part="part" />
        <bm-button-close class="mx-2" size="lg" variant="simple" @click="$emit('close')" />
    </div>
</template>

<script>
import { BmButtonClose } from "@bluemind/styleguide";
import PreviewAttachmentHeader from "./PreviewAttachmentHeader";
import PreviewMessageHeader from "./PreviewMessageHeader";

export default {
    name: "PreviewHeader",
    components: { PreviewMessageHeader, PreviewAttachmentHeader, BmButtonClose },
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
