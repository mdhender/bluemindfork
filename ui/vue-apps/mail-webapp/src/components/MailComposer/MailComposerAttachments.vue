<template>
    <bm-file-drop-zone
        class="mail-composer-attachments attachments"
        :should-activate-fn="shouldActivateForImages"
        adapt-to-content
        v-on="$listeners"
    >
        <template #dropZone>
            <bm-icon class="text-neutral" icon="attach" size="xl" />
            <div class="text-center text-neutral p-4">
                <h3 class="p-2">{{ $tc("mail.new.attachments.images.drop.zone", draggedFilesCount) }}</h3>
            </div>
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
                <file-toolbar ref="toolbar" :file="file" :message="message" :buttons="actionButtons" />
            </template>
            <template #overlay="slotProps">
                <preview-overlay v-if="slotProps.hasPreview" />
                <filetype-overlay v-else :file="slotProps.file" />
            </template>
        </files-block>
    </bm-file-drop-zone>
</template>

<script>
import { mapActions, mapMutations, mapState } from "vuex";
import { BmIcon, BmFileDropZone } from "@bluemind/ui-components";
import { attachmentUtils, partUtils, fileUtils, messageUtils } from "@bluemind/mail";
import FilesBlock from "../MailAttachment/FilesBlock";
import { RemoveAttachmentCommand } from "~/commands";
import { RESET_FILES } from "~/mutations";
import { SET_PREVIEW } from "~/actions";
import PreviewOverlay from "../MailAttachment/Overlays/PreviewOverlay";
import FiletypeOverlay from "../MailAttachment/Overlays/FiletypeOverlay";
import FileToolbar from "../MailAttachment/FileToolbar";
import messageCompose from "~/store/messageCompose";

const { isViewable } = partUtils;
const { ActionButtons, isUploading, isAllowedToPreview } = fileUtils;
const { AttachmentAdaptor } = attachmentUtils;
const { computeParts } = messageUtils;

export default {
    name: "MailComposerAttachments",
    components: {
        BmFileDropZone,
        BmIcon,
        FilesBlock,
        PreviewOverlay,
        FiletypeOverlay,
        FileToolbar
    },
    mixins: [RemoveAttachmentCommand],
    props: {
        draggedFilesCount: { type: Number, default: 0 },
        message: { type: Object, required: true },
        attachments: { type: Array, required: true }
    },
    data() {
        return {
            actionButtons: [ActionButtons.PREVIEW, ActionButtons.DOWNLOAD, ActionButtons.REMOVE]
        };
    },
    computed: {
        ...mapState("mail", {
            attachmentsMaxWeight: ({ messageCompose }) => messageCompose.maxMessageSize,
            uploadingFiles: ({ messageCompose }) => messageCompose.uploadingFiles
        }),
        files() {
            const files = AttachmentAdaptor.extractFiles(this.attachments, this.message);
            return files.map(file => ({ ...file, ...this.uploadingFiles[file.key] }));
        }
    },
    destroyed() {
        this.RESET_FILES();
    },
    methods: {
        ...mapMutations("mail", { RESET_FILES }),
        ...mapActions("mail", { SET_PREVIEW }),
        shouldActivateForImages(event) {
            const regex = "image/(jpeg|jpg|png|gif)";
            const files = event.dataTransfer.items.length
                ? Object.keys(event.dataTransfer.items).map(key => event.dataTransfer.items[key])
                : [];
            const matchFunction = f => f.type.match(new RegExp(regex, "i"));
            return files.length > 0 && files.every(matchFunction);
        },
        openPreview(fileKey) {
            this.SET_PREVIEW({ messageKey: this.message.key, fileKey: fileKey });
            this.$bvModal.show("preview-modal");
        },
        download(file) {
            this.$refs.toolbar.download(file);
        },
        previewOrDownload(file) {
            if (!isUploading(file)) {
                if (isAllowedToPreview(file)) {
                    this.openPreview(file.key);
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
