<template>
    <div class="preview-attachment d-flex flex-column bg-dark">
        <preview-attachment-header :part="part" @close="$emit('close')" />
        <image-part-viewer :message="message" :part="part" class="flex-shrink-1" />
    </div>
</template>

<script>
import ImagePartViewer from "../../MailViewer/PartsViewer/ImagePartViewer";
import PreviewAttachmentHeader from "./PreviewAttachmentHeader";

export default {
    name: "PreviewAttachment",
    components: { ImagePartViewer, PreviewAttachmentHeader },
    props: {
        message: {
            type: Object,
            required: true
        },
        part: {
            type: Object,
            required: true
        }
    }
};
</script>

<style lang="scss">
.preview-attachment {
    flex: 1 1 auto;

    .image-part-viewer {
        max-width: 100%;
        min-height: 0;
    }
}
</style>
