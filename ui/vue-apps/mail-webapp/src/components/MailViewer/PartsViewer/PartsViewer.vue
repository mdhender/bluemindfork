<template>
    <div class="parts-viewer py-2">
        <template v-for="(part, index) in parts">
            <hr v-if="index !== 0" :key="part.address + '-separator'" class="part-separator" />
            <text-html-part-viewer
                v-if="isHtmlPart(part)"
                :key="part.address"
                :value="htmlWithImageInserted[index]"
                :message="message"
            />
            <text-plain-part-viewer
                v-else-if="isTextPart(part)"
                :key="part.address"
                :value="partsData[message.key] && partsData[message.key][part.address]"
            />
            <image-part-viewer v-else-if="isImagePart(part)" :key="part.address" :value="computeImageUrl(part)" />
        </template>
    </div>
</template>

<script>
import { mapActions, mapMutations, mapState } from "vuex";

import { computePreviewOrDownloadUrl, MimeType, InlineImageHelper } from "@bluemind/email";

import { getPartsFromCapabilities } from "~/model/part";
import { create as createAttachment, AttachmentStatus } from "~/model/attachment";
import ImagePartViewer from "./ImagePartViewer";
import TextHtmlPartViewer from "./TextHtmlPartViewer";
import TextPlainPartViewer from "./TextPlainPartViewer";
import { FETCH_PART_DATA } from "~/actions";
import { ADD_ATTACHMENT, REMOVE_ATTACHMENT } from "~/mutations";

const VIEWER_CAPABILITIES = [MimeType.TEXT_HTML, MimeType.TEXT_PLAIN, MimeType.IMAGE];

export default {
    name: "PartsViewer",
    components: {
        ImagePartViewer,
        TextHtmlPartViewer,
        TextPlainPartViewer
    },
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            parts: [],
            htmlWithImageInserted: [],
            localAttachments: []
        };
    },
    computed: {
        ...mapState("mail", ["partsData"])
    },
    watch: {
        "message.key": {
            handler: async function (messageKey, oldMessageKey) {
                this.clean(oldMessageKey);

                const inlines = getPartsFromCapabilities(this.message, VIEWER_CAPABILITIES);

                // trigger parts display even if content is not loaded (each child component manages its own loading status)
                this.parts = [...inlines.filter(part => !MimeType.isImage(part) || !part.contentId)];

                await this.FETCH_PART_DATA({
                    messageKey: this.message.key,
                    folderUid: this.message.folderRef.uid,
                    imapUid: this.message.remoteRef.imapUid,
                    inlines: inlines.filter(part => MimeType.isHtml(part) || MimeType.isText(part))
                });

                const { htmlParts, htmlContents, others } = await this.handleCidParts(inlines);
                const doesViewerSupportPart = ({ mime }) =>
                    VIEWER_CAPABILITIES.some(available => mime.startsWith(available) || available === mime);

                // trigger parts display with all data loaded
                this.htmlWithImageInserted = htmlContents;
                this.parts = others.filter(doesViewerSupportPart).concat(htmlParts);

                // display unsupported parts as attachment
                const unsupported = others.filter(part => !doesViewerSupportPart(part));
                this.displayAsAttachments(unsupported);
            },
            immediate: true
        }
    },
    methods: {
        ...mapActions("mail", { FETCH_PART_DATA }),
        ...mapMutations("mail", { ADD_ATTACHMENT, REMOVE_ATTACHMENT }),
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
        },
        clean(previousMessageKey) {
            this.localAttachments.forEach(attachment => {
                this.REMOVE_ATTACHMENT({ messageKey: previousMessageKey, address: attachment.address });
            });

            this.localAttachments = [];
            this.parts = [];
            this.htmlWithImageInserted = [];
        },
        async handleCidParts(inlines) {
            const htmlParts = inlines.filter(MimeType.isHtml);
            const htmlContents = htmlParts.map(part => this.partsData[this.message.key][part.address]);
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
            return { htmlParts, htmlContents: insertionResult.contentsWithImageInserted, others };
        },
        displayAsAttachments(unsupportedParts) {
            this.localAttachments = unsupportedParts.map(part => createAttachment(part, AttachmentStatus.ONLY_LOCAL));
            this.localAttachments.forEach(attachment =>
                this.ADD_ATTACHMENT({ messageKey: this.message.key, attachment })
            );
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
