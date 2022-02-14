<template>
    <div class="preview-attachment bg-dark">
        <part-viewer-facade :message="message" :part="part" />
    </div>
</template>

<script>
import PartViewerFacade from "../../MailViewer/PartsViewer/PartViewerFacade";

export default {
    name: "PreviewAttachment",
    components: { PartViewerFacade },
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
@import "@bluemind/styleguide/css/_variables.scss";

.preview-attachment {
    .part-viewer-facade {
        width: 100%;
        height: 100%;
        overflow: auto;

        .image-part-viewer {
            height: 100%;
        }
        .text-html-part-viewer,
        .text-plain-part-viewer {
            background-color: $surface-bg;
            width: 80%;
            min-height: 100%;
            margin: 0 auto;
        }

        .text-plain-part-viewer {
            padding: $sp-4;
        }
    }
}
</style>
