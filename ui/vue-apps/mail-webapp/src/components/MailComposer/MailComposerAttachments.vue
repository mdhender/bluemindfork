<template>
    <bm-file-drop-zone
        class="mail-composer-attachments z-index-110 attachments mb-2"
        :should-activate-fn="shouldActivateForImages"
        v-on="$listeners"
    >
        <template #dropZone>
            <bm-icon icon="paper-clip" />
            <h2 class="text-center p-2">
                {{ $tc("mail.new.attachments.images.drop.zone", draggedFilesCount) }}
            </h2>
        </template>
        <files-block
            :files="files"
            :message="message"
            :max-size="attachmentsMaxWeight"
            expanded
            :class="{ clickable: !message.composing }"
            @click-item="previewOrDownload"
        >
            <template #actions="{ file }">
                <preview-button
                    v-if="isViewable(file)"
                    :disabled="!isAllowedToPreview(file)"
                    @preview="openPreview(file)"
                />
                <download-button :ref="`download-button-${file.key}`" :file="file" class="d-none" />
                <cancel-button v-if="isUploading(file)" @cancel="cancel(file)" />
                <remove-button v-else @remove="removeAttachment(file)" />
            </template>
            <template #overlay="slotProps">
                <preview-overlay v-if="slotProps.hasPreview" />
                <filetype-overlay v-else :file="slotProps.file" />
            </template>
        </files-block>
    </bm-file-drop-zone>
</template>

<script>
import { mapMutations, mapState } from "vuex";

import global from "@bluemind/global";
import { BmIcon, BmFileDropZone } from "@bluemind/styleguide";
import { partUtils, fileUtils } from "@bluemind/mail";
import FilesBlock from "../MailAttachment/FilesBlock";
import PreviewButton from "../MailAttachment/ActionButtons/PreviewButton";
import CancelButton from "../MailAttachment/ActionButtons/CancelButton";
import RemoveButton from "../MailAttachment/ActionButtons/RemoveButton";
import DownloadButton from "../MailAttachment/ActionButtons/DownloadButton";
import { RemoveAttachmentCommand } from "~/commands";
import { SET_PREVIEW_MESSAGE_KEY, SET_PREVIEW_FILE_KEY } from "~/mutations";
import PreviewOverlay from "../MailAttachment/Overlays/PreviewOverlay";
import FiletypeOverlay from "../MailAttachment/Overlays/FiletypeOverlay";
const { isViewable } = partUtils;
const { isUploading, isAllowedToPreview } = fileUtils;

export default {
    name: "MailComposerAttachments",
    components: {
        BmFileDropZone,
        BmIcon,
        FilesBlock,
        PreviewButton,
        CancelButton,
        RemoveButton,
        DownloadButton,
        PreviewOverlay,
        FiletypeOverlay
    },
    mixins: [RemoveAttachmentCommand],
    props: {
        draggedFilesCount: {
            type: Number,
            default: 0
        },
        message: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapState("mail", { attachmentsMaxWeight: ({ messageCompose }) => messageCompose.maxMessageSize }),
        files() {
            return this.message.attachments.map(({ fileKey }) => this.$store.state.mail.files[fileKey]);
        }
    },
    methods: {
        ...mapMutations("mail", {
            SET_PREVIEW_MESSAGE_KEY,
            SET_PREVIEW_FILE_KEY
        }),
        shouldActivateForImages(event) {
            const regex = "image/(jpeg|jpg|png|gif)";
            const files = event.dataTransfer.items.length
                ? Object.keys(event.dataTransfer.items).map(key => event.dataTransfer.items[key])
                : [];
            const matchFunction = f => f.type.match(new RegExp(regex, "i"));
            return files.length > 0 && files.every(matchFunction);
        },
        openPreview(file) {
            this.SET_PREVIEW_MESSAGE_KEY(this.message.key);
            this.SET_PREVIEW_FILE_KEY(file.key);
            this.$bvModal.show("preview-modal");
        },
        cancel(file) {
            global.cancellers[file.key].cancel();
        },
        removeAttachment({ key }) {
            const attachment = this.message.attachments.find(attachment => attachment.fileKey === key);
            this.$execute("remove-attachment", { attachment, message: this.message });
        },
        download(file) {
            this.$refs[`download-button-${file.key}`].clickButton();
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
        isUploading,
        isViewable,
        isAllowedToPreview
    }
};
</script>

<style lang="scss">
.mail-composer-attachments {
    .clickable .file-item {
        cursor: pointer;
    }
}
</style>
