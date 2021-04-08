<template>
    <div class="parts-viewer py-2">
        <template v-for="(part, index) in parts">
            <hr v-if="index !== 0" :key="part.address + '-separator'" class="part-separator" />
            <text-html-part-viewer v-if="isHtmlPart(part)" :key="part.address" :value="htmlWithImageInserted[index]" />
            <text-plain-part-viewer
                v-else-if="isTextPart(part)"
                :key="part.address"
                :value="activeMessage.partsDataByAddress[part.address]"
            />
            <image-part-viewer v-else-if="isImagePart(part)" :key="part.address" :value="computeImageUrl(part)" />
        </template>
    </div>
</template>

<script>
import { mapActions, mapState } from "vuex";

import { computePreviewOrDownloadUrl, MimeType, InlineImageHelper } from "@bluemind/email";

import { getPartsFromCapabilities } from "~model/part";
import ImagePartViewer from "./ImagePartViewer";
import TextHtmlPartViewer from "./TextHtmlPartViewer";
import TextPlainPartViewer from "./TextPlainPartViewer";
import { FETCH_ACTIVE_MESSAGE_INLINE_PARTS } from "~actions";

const VIEWER_CAPABILITIES = [MimeType.TEXT_HTML, MimeType.TEXT_PLAIN];

export default {
    name: "PartsViewer",
    components: {
        ImagePartViewer,
        TextHtmlPartViewer,
        TextPlainPartViewer
    },
    props: {
        messageKey: {
            type: [Number, String],
            required: true
        }
    },
    data() {
        return {
            parts: [],
            htmlWithImageInserted: []
        };
    },
    computed: {
        ...mapState("mail", ["messages", "activeMessage"]),
        message() {
            return this.messages[this.messageKey];
        }
    },
    watch: {
        messageKey: {
            handler: async function () {
                this.parts = [];
                this.htmlWithImageInserted = [];
                const inlines = getPartsFromCapabilities(this.message, VIEWER_CAPABILITIES);

                this.parts = [...inlines.filter(part => !MimeType.isImage(part) || !part.contentId)];

                await this.FETCH_ACTIVE_MESSAGE_INLINE_PARTS({
                    folderUid: this.message.folderRef.uid,
                    imapUid: this.message.remoteRef.imapUid,
                    inlines: inlines.filter(part => MimeType.isHtml(part) || MimeType.isText(part))
                });

                const html = inlines.filter(MimeType.isHtml);
                const htmlContents = html.map(part => this.activeMessage.partsDataByAddress[part.address]);
                const cidImages = inlines.filter(part => MimeType.isImage(part) && part.contentId);
                const insertionResult = await InlineImageHelper.insertAsUrl(
                    htmlContents,
                    cidImages,
                    this.message.folderRef.uid,
                    this.message.remoteRef.imapUid
                );
                const others = inlines.filter(
                    part =>
                        part.mime !== MimeType.TEXT_HTML &&
                        !insertionResult.imageInlined.map(p => p.contentId).includes(part.contentId)
                );
                this.htmlWithImageInserted = insertionResult.contentsWithImageInserted;
                this.parts = [...html, ...others];
            },
            immediate: true
        }
    },
    methods: {
        ...mapActions("mail", { FETCH_ACTIVE_MESSAGE_INLINE_PARTS }),
        isHtmlPart(part) {
            return MimeType.isHtml(part);
        },
        isTextPart(part) {
            return MimeType.isText(part);
        },
        isImagePart(part) {
            return MimeType.isImage(part);
        },
        computeImageUrl(part) {
            return computePreviewOrDownloadUrl(this.message.folderRef.uid, this.message.remoteRef.imapUid, part);
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
