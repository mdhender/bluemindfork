<template>
    <div class="mail-inlines-block py-2">
        <div v-for="(part, index) in parts" :key="part.address">
            <hr v-if="index !== 0" class="part-separator" />
            <slot :name="slotName(part)" :message="message" :part="part">
                <component :is="componentName(part)" :message="message" :part="part"></component>
            </slot>
        </div>
    </div>
</template>

<script>
import { MimeType } from "@bluemind/email";

import ImagePartViewer from "./PartsViewer/ImagePartViewer";
import IframedTextHtmlPartViewer from "./PartsViewer/IframedTextHtmlPartViewer.vue";
import TextPlainPartViewer from "./PartsViewer/TextPlainPartViewer";

export default {
    name: "MailInlinesBlock",
    components: { ImagePartViewer, IframedTextHtmlPartViewer, TextPlainPartViewer },
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
    },
    methods: {
        slotName({ mime }) {
            return mime.replaceAll("/", "-");
        },
        componentName(part) {
            if (MimeType.isHtml(part)) {
                return "iframed-text-html-part-viewer";
            } else if (MimeType.isImage(part)) {
                return "image-part-viewer";
            } else if (MimeType.isText(part)) {
                return "text-plain-part-viewer";
            }
        }
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
