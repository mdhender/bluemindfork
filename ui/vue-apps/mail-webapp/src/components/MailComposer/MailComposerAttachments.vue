<template>
    <bm-file-drop-zone
        class="mail-composer-attachments z-index-110 attachments mb-2"
        :should-activate-fn="shouldActivateForImages"
        v-on="$listeners"
    >
        <template #dropZone>
            <bm-icon class="text-neutral" icon="paper-clip" size="xl" />
            <div class="text-center text-neutral p-4">
                {{ $tc("mail.new.attachments.images.drop.zone", draggedFilesCount) }}
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
import { mapMutations, mapState } from "vuex";
import { BmIcon, BmFileDropZone } from "@bluemind/styleguide";
import { partUtils, fileUtils } from "@bluemind/mail";
import FilesBlock from "../MailAttachment/FilesBlock";
import { RemoveAttachmentCommand } from "~/commands";
import { SET_PREVIEW_MESSAGE_KEY, SET_PREVIEW_FILE_KEY } from "~/mutations";
import PreviewOverlay from "../MailAttachment/Overlays/PreviewOverlay";
import FiletypeOverlay from "../MailAttachment/Overlays/FiletypeOverlay";
import FileToolbar from "../MailAttachment/FileToolbar";
const { isViewable } = partUtils;
const { ActionButtons, isUploading, isAllowedToPreview } = fileUtils;

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
        draggedFilesCount: {
            type: Number,
            default: 0
        },
        message: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            actionButtons: [ActionButtons.PREVIEW, ActionButtons.DOWNLOAD, ActionButtons.REMOVE]
        };
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
        openPreview(file, message) {
            this.$refs.toolbar.openPreview(file, message);
        },
        download(file) {
            this.$refs.toolbar.download(file);
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
