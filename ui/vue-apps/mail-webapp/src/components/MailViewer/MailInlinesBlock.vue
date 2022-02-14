<template>
    <div class="mail-inlines-block py-2">
        <div v-for="(part, index) in parts" :key="part.address">
            <hr v-if="index !== 0" class="part-separator" />
            <part-viewer-facade :message="message" :part="part" />
        </div>
    </div>
</template>

<script>
import PartViewerFacade from "./PartsViewer/PartViewerFacade";

export default {
    name: "MailInlinesBlock",
    components: { PartViewerFacade },
    props: {
        message: {
            type: Object,
            required: true
        },
        parts: {
            type: Array,
            required: true
        }
    },
    data() {
        return { htmlWithImageInserted: [], localAttachments: [] };
    }
};
</script>

<style lang="scss">
.mail-inlines-block {
    .part-separator {
        margin: 1rem 0;
        border: 0;
        border-top: 1px solid rgba(0, 0, 0, 0.3);
        height: 0;
    }
}
</style>
