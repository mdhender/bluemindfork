<template>
    <mail-viewer-content class="preview-message" :message="message">
        <template v-slot:attachments-block="scope">
            <div class="mail-files">
                <files-header :files="scope.files" :message="scope.message" />
                <file-item
                    v-for="file in scope.files"
                    :key="file.key"
                    :file="file"
                    :message="scope.message"
                    :class="file.key === activeFile.key ? 'active' : ''"
                    @click-item="previewOrDownload"
                >
                    <template v-slot:actions="{ file: slotFile }">
                        <file-toolbar ref="toolbar" :buttons="actionButtons" :file="slotFile" :message="message" />
                    </template>
                    <template #overlay="{ file: slotFile, hasPreview }">
                        <preview-overlay v-if="hasPreview" />
                        <filetype-overlay v-else :file="slotFile" />
                    </template>
                </file-item>
            </div>
        </template>
    </mail-viewer-content>
</template>

<script>
import { mapMutations } from "vuex";
import { partUtils, fileUtils } from "@bluemind/mail";
import { SET_PREVIEW_MESSAGE_KEY, SET_PREVIEW_FILE_KEY } from "~/mutations";
import MailViewerContent from "../../MailViewer/MailViewerContent";
import FileItem from "../FileItem";
import FilesHeader from "../FilesHeader";
import PreviewOverlay from "../Overlays/PreviewOverlay";
import FiletypeOverlay from "../Overlays/FiletypeOverlay";
import FileToolbar from "../FileToolbar";
const { isViewable } = partUtils;
const { ActionButtons, isUploading, isAllowedToPreview, isLarge } = fileUtils;

export default {
    name: "PreviewMessage",
    components: {
        MailViewerContent,
        FileItem,
        FilesHeader,
        PreviewOverlay,
        FiletypeOverlay,
        FileToolbar
    },
    props: {
        message: {
            type: Object,
            required: true
        },
        activeFile: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            actionButtons: [ActionButtons.PREVIEW, ActionButtons.DOWNLOAD, ActionButtons.OTHER]
        };
    },
    methods: {
        ...mapMutations("mail", {
            SET_PREVIEW_MESSAGE_KEY,
            SET_PREVIEW_FILE_KEY
        }),
        openPreview(file, message) {
            this.$refs.toolbar[0].openPreview(file, message);
        },
        download(file) {
            this.$refs.toolbar[0].download(file);
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
        isLarge
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

.preview-message.mail-viewer-content {
    display: flex;
    flex-direction: column;

    .sender .bm-contact {
        display: none;
    }
    * .date {
        text-align: left;
        white-space: nowrap;
        color: $neutral-fg-lo1;
    }
    .mail-inlines-block {
        padding-left: $sp-6;
    }
    .mail-sender-splitter {
        display: none;
    }
    .mail-viewer-recipients {
        display: none;
    }
    .sender,
    .mail-viewer-recipients {
        order: 0;
        margin: 0;
    }
    .body-viewer {
        order: 2;
        padding: 0;
    }
    .sender,
    .mail-viewer-recipients,
    .title {
        padding-left: $sp-6;
        padding-right: $sp-5;
    }
    .title {
        padding-top: $sp-5;
        color: $neutral-fg-hi1;
    }
    .sender {
        margin-bottom: $sp-5;
    }
    & > hr {
        margin: $sp-3 0 0 0;
    }
    .mail-viewer-recipient {
        flex-wrap: nowrap;
        white-space: nowrap;
    }
    .mail-files {
        padding: $sp-4 $sp-5 $sp-5 $sp-6;
        background-color: $neutral-bg-lo1;

        display: flex;
        flex-direction: column;
        gap: $sp-4;
    }
}
</style>
