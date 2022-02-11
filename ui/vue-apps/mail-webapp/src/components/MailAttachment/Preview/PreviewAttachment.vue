<template>
    <div class="preview-attachment d-flex flex-column bg-dark">
        <image-part-viewer :message="message" :part="part" class="flex-shrink-1" />
    </div>
</template>

<script>
import ImagePartViewer from "../../MailViewer/PartsViewer/ImagePartViewer";

export default {
    name: "PreviewAttachment",
    components: { ImagePartViewer },
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
