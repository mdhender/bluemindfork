<template>
    <div class="mail-inlines-block">
        <div v-for="(part, index) in parts" :key="part.address">
            <hr v-if="index !== 0" class="part-separator" />
            <file-viewer-facade :message="message" :file="part">
                <template v-for="(_, slot) of $scopedSlots" v-slot:[slot]="scope">
                    <slot :name="slot" v-bind="scope" />
                </template>
            </file-viewer-facade>
        </div>
    </div>
</template>

<script>
import FileViewerFacade from "./FilesViewer/FileViewerFacade";

export default {
    name: "MailInlinesBlock",
    components: { FileViewerFacade },
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
@import "~@bluemind/styleguide/css/_variables";

.mail-inlines-block {
    padding-top: $sp-6;
    padding-bottom: $sp-6;

    .part-separator {
        margin: 1rem 0;
        border: 0;
        border-top: 1px solid $neutral-fg-lo2;
        height: 0;
    }
}
</style>
