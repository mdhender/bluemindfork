<template>
    <div class="body-viewer">
        <slot name="attachments-block" :files="files" :message="message">
            <files-block
                :files="files"
                :message="message"
                @click-item="previewOrDownload"
                @remote-content="triggerRemoteContent"
            >
                <template v-slot:actions="{ file }">
                    <preview-button
                        v-if="isViewable(file)"
                        :disabled="!isAllowedToPreview"
                        @preview="openPreview(file)"
                    />
                    <download-button :ref="`download-button-${file.key}`" :file="file" />
                </template>
                <template #overlay="slotProps">
                    <preview-overlay v-if="slotProps.hasPreview" />
                    <filetype-overlay v-else :file="slotProps.file" />
                </template>
            </files-block>
        </slot>
        <event-viewer v-if="message.hasICS && currentEvent" :parts="inlines" :message="message">
            <template v-for="(_, slot) of $scopedSlots" v-slot:[slot]="scope">
                <slot :name="slot" v-bind="scope" />
            </template>
        </event-viewer>
        <mail-inlines-block v-else :message="message" :parts="inlines">
            <template v-for="(_, slot) of $scopedSlots" v-slot:[slot]="scope">
                <slot :name="slot" v-bind="scope" />
            </template>
        </mail-inlines-block>
    </div>
</template>

<script>
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";
import { MimeType, InlineImageHelper } from "@bluemind/email";
import { hasRemoteImages } from "@bluemind/html-utils";
import { attachmentUtils, fileUtils, partUtils } from "@bluemind/mail";

import { COMPUTE_QUOTE_NODES, FETCH_PART_DATA } from "~/actions";
import { CONVERSATION_MESSAGE_BY_KEY } from "~/getters";
import { SET_PREVIEW_FILE_KEY, SET_PREVIEW_MESSAGE_KEY } from "~/mutations";

import MailInlinesBlock from "./MailInlinesBlock";
import EventViewer from "./EventViewer";
import FilesBlock from "../MailAttachment/FilesBlock";
import PreviewButton from "../MailAttachment/ActionButtons/PreviewButton";
import DownloadButton from "../MailAttachment/ActionButtons/DownloadButton";
import PreviewOverlay from "../MailAttachment/Overlays/PreviewOverlay";
import FiletypeOverlay from "../MailAttachment/Overlays/FiletypeOverlay";

const { create: createAttachment } = attachmentUtils;
const { FileStatus, isUploading, isAllowedToPreview } = fileUtils;
const { VIEWER_CAPABILITIES, getPartsFromCapabilities, isViewable } = partUtils;

export default {
    name: "BodyViewer",
    components: {
        EventViewer,
        FilesBlock,
        MailInlinesBlock,
        PreviewButton,
        DownloadButton,
        PreviewOverlay,
        FiletypeOverlay
    },
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapState("mail", { currentEvent: state => state.consultPanel.currentEvent }),
        ...mapGetters("mail", { CONVERSATION_MESSAGE_BY_KEY }),
        contents() {
            return this.$store.state.mail.partsData.partsByMessageKey[this.message.key];
        },
        inlines() {
            let contents = this.$store.state.mail.partsData.partsByMessageKey[this.message.key] || [];
            const cids = new CidSet(
                this.parts.flatMap(({ address, mime }) =>
                    MimeType.isHtml({ mime }) && contents[address] ? InlineImageHelper.cids(contents[address]) : []
                )
            );
            return this.parts.filter(part => isViewable(part) && !(MimeType.isImage(part) && cids.has(part.contentId)));
        },
        parts() {
            return getPartsFromCapabilities(this.message, VIEWER_CAPABILITIES);
        },
        attachments() {
            const fallback = this.parts
                .filter(part => !isViewable(part))
                .map(part => createAttachment(part, FileStatus.ONLY_LOCAL));
            return [...this.message.attachments, ...fallback];
        },
        files() {
            return this.message.attachments.map(({ fileKey }) => this.$store.state.mail.files[fileKey]);
        }
    },
    async created() {
        const texts = this.parts.filter(part => MimeType.isHtml(part));
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
        const conversationMessages = this.message.conversationRef
            ? this.CONVERSATION_MESSAGE_BY_KEY(this.message.conversationRef.key)
            : [this.message];
        this.COMPUTE_QUOTE_NODES({ message: this.message, conversationMessages });
    },
    methods: {
        ...mapActions("mail", { FETCH_PART_DATA, COMPUTE_QUOTE_NODES }),
        ...mapMutations("mail", { SET_PREVIEW_MESSAGE_KEY, SET_PREVIEW_FILE_KEY }),
        openPreview(file) {
            this.SET_PREVIEW_MESSAGE_KEY(this.message.key);
            this.SET_PREVIEW_FILE_KEY(file.key);
            this.$bvModal.show("preview-modal");
        },
        download(file) {
            this.$refs[`download-button-${file.key}`].clickButton();
        },
        triggerRemoteContent() {
            this.$emit("remote-content", this.message);
        },
        previewOrDownload(file) {
            if (!isUploading(file)) {
                if (isAllowedToPreview(file)) {
                    this.openPreview(file, this.message);
                } else {
                    this.download(file);
                }
            }
        },
        isViewable,
        isAllowedToPreview
    }
};

class CidSet extends Set {
    has(cid) {
        const r = /^<?([^>]*)>?$/;
        return cid && super.has(cid.replace(r, "$1").toUpperCase());
    }
}
</script>

<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

.body-viewer {
    display: flex;
    flex-direction: column;
    gap: $sp-5;
}
</style>
