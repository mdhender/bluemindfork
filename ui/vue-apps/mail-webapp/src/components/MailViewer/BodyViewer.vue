<template>
    <bm-extension
        id="webapp.mail"
        path="viewer.body"
        type="chain-of-responsibility"
        :message="message"
        :inline-parts-by-capabilities="computedParts.inlinePartsByCapabilities"
    >
        <div class="body-viewer">
            <mail-top-frame :message="message" :files="files" />
            <slot name="attachments-block" :files="files" :message="message">
                <files-block
                    :files="files"
                    :message="message"
                    @click-item="previewOrDownload"
                    @remote-content="triggerRemoteContent"
                >
                    <template #actions="{ file }">
                        <file-toolbar
                            ref="toolbar"
                            :buttons="actionButtons"
                            :file="file"
                            :message="message"
                            @preview="previewFiles(file.key)"
                        />
                    </template>
                    <template #overlay="slotProps">
                        <preview-overlay v-if="slotProps.hasPreview" />
                        <filetype-overlay v-else :file="slotProps.file" />
                    </template>
                </files-block>
            </slot>
            <mail-inlines-block :message="message" :parts="inlineParts">
                <template v-for="(_, slot) of $scopedSlots" #[slot]="scope">
                    <slot :name="slot" v-bind="scope" />
                </template>
            </mail-inlines-block>
        </div>
    </bm-extension>
</template>

<script>
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";
import { MimeType } from "@bluemind/email";

import { BmExtension } from "@bluemind/extensions.vue";
import { hasRemoteImages } from "@bluemind/html-utils";
import { attachmentUtils, fileUtils, partUtils, messageUtils } from "@bluemind/mail";

import { FETCH_PART_DATA, SET_FILES } from "~/actions";
import { CONVERSATION_MESSAGE_BY_KEY } from "~/getters";
import { SET_PREVIEW_FILE_KEY, SET_PREVIEW_MESSAGE_KEY } from "~/mutations";

import FilesBlock from "../MailAttachment/FilesBlock";
import FileToolbar from "../MailAttachment/FileToolbar";
import FiletypeOverlay from "../MailAttachment/Overlays/FiletypeOverlay";
import MailInlinesBlock from "./MailInlinesBlock";
import PreviewOverlay from "../MailAttachment/Overlays/PreviewOverlay";
import MailTopFrame from "./MailTopFrame/MailTopFrame";

const { create: createAttachment, AttachmentAdaptor } = attachmentUtils;
const { FileStatus, isUploading, isAllowedToPreview, ActionButtons } = fileUtils;
const { VIEWER_CAPABILITIES, getPartsFromCapabilities, isViewable } = partUtils;
const { computeParts } = messageUtils;

export default {
    name: "BodyViewer",
    components: {
        BmExtension,
        FilesBlock,
        FileToolbar,
        FiletypeOverlay,
        MailInlinesBlock,
        PreviewOverlay,
        MailTopFrame
    },
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            actionButtons: [ActionButtons.PREVIEW, ActionButtons.DOWNLOAD, ActionButtons.OTHER],
            computedParts: {}
        };
    },
    computed: {
        ...mapState("mail", {
            currentEvent: state => state.consultPanel.currentEvent,
            filesToPreview: state => state.files || {}
        }),
        ...mapGetters("mail", { CONVERSATION_MESSAGE_BY_KEY }),
        contents() {
            return this.$store.state.mail.partsData.partsByMessageKey[this.message.key];
        },
        inlineParts() {
            return getPartsFromCapabilities(this.computedParts, VIEWER_CAPABILITIES);
        },
        files() {
            const { files } = AttachmentAdaptor.extractFiles(this.computedParts.attachments, this.message);
            const fallback = this.inlineParts
                .filter(part => !isViewable(part))
                .map(part => createAttachment(part, FileStatus.ONLY_LOCAL));
            return [...files, ...fallback];
        }
    },
    watch: {
        "message.structure": {
            handler(structure) {
                this.computedParts = computeParts(structure);
            },
            deep: false,
            immediate: true
        }
    },
    async created() {
        const texts = this.inlineParts.filter(part => MimeType.isHtml(part));
        await this.FETCH_PART_DATA({
            messageKey: this.message.key,
            folderUid: this.message.folderRef.uid,
            imapUid: this.message.remoteRef.imapUid,
            parts: texts
        });
        const hasImages = texts.some(part => MimeType.isHtml(part) && hasRemoteImages(this.contents[part.address]));
        if (hasImages) {
            this.triggerRemoteContent();
        }
    },
    methods: {
        ...mapActions("mail", { FETCH_PART_DATA, SET_FILES }),
        ...mapMutations("mail", { SET_PREVIEW_MESSAGE_KEY, SET_PREVIEW_FILE_KEY }),
        download(file) {
            this.$refs.toolbar.download(file);
        },
        triggerRemoteContent() {
            this.$emit("remote-content", this.message);
        },
        previewOrDownload(file) {
            if (!isUploading(file)) {
                if (isAllowedToPreview(file)) {
                    this.previewFiles(file.key);
                } else {
                    this.download(file);
                }
            }
        },
        previewFiles(fileKey) {
            if (!Object.keys(this.filesToPreview).includes(fileKey)) {
                this.SET_FILES({ files: this.files });
            }
            this.SET_PREVIEW_MESSAGE_KEY(this.message.key);
            this.SET_PREVIEW_FILE_KEY(fileKey);
            this.$bvModal.show("preview-modal");
        },
        isViewable,
        isAllowedToPreview
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.body-viewer {
    display: flex;
    flex-direction: column;
    gap: $sp-5;
}
</style>
