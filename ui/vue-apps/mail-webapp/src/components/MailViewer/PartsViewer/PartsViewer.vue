<template>
    <div class="parts-viewer py-2">
        <template v-for="(part, index) in parts">
            <template v-if="isSupportedPart(part)">
                <hr v-if="index !== 0" :key="part.address + '-sepatator'" class="part-separator" />
                <component :is="computePartComponent(part.mime)" :key="part.address" :value="part.content" />
            </template>
        </template>
    </div>
</template>

<script>
import { mapGetters } from "vuex";
import { MimeType } from "@bluemind/email";
import ImagePartViewer from "./ImagePartViewer";
import TextHtmlPartViewer from "./TextHtmlPartViewer";
import TextPlainPartViewer from "./TextPlainPartViewer";

export default {
    name: "PartsViewer",
    components: {
        ImagePartViewer,
        TextHtmlPartViewer,
        TextPlainPartViewer
    },
    computed: {
        ...mapGetters("mail-webapp/currentMessage", { parts: "content", message: "message" })
    },
    methods: {
        isHtmlPart(part) {
            return MimeType.isHtml(part);
        },
        isTextPart(part) {
            return MimeType.isText(part);
        },
        isImagePart(part) {
            return MimeType.isImage(part);
        },
        isSupportedPart(part) {
            return this.isHtmlPart(part) || this.isTextPart(part) || this.isImagePart(part);
        },
        computePartComponent(mimeType) {
            let name;
            if (MimeType.isImage({ mime: mimeType })) {
                name = "Image";
            } else {
                name = mimeType
                    .split("/")
                    .map(subtype => subtype[0].toUpperCase() + subtype.substring(1, subtype.length))
                    .join("");
            }
            return name + "PartViewer";
        }
    }
};
</script>

<style lang="scss">
.parts-viewer {
    .part-separator {
        margin: 1rem 0;
        border: 0;
        border-top: 1px solid rgba(0, 0, 0, 0.3);
        height: 0;
    }
}
</style>
