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
                        <preview-button
                            v-if="!isLarge(slotFile) && isViewable(slotFile)"
                            @preview="openPreview(slotFile)"
                        />
                        <download-button :ref="`download-button-${slotFile.key}`" :file="slotFile" />
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
import PreviewButton from "../ActionButtons/PreviewButton";
import DownloadButton from "../ActionButtons/DownloadButton";
import PreviewOverlay from "../Overlays/PreviewOverlay";
import FiletypeOverlay from "../Overlays/FiletypeOverlay";
const { isViewable } = partUtils;
const { isUploading, isAllowedToPreview, isLarge } = fileUtils;

export default {
    name: "PreviewMessage",
    components: {
        MailViewerContent,
        FileItem,
        FilesHeader,
        PreviewButton,
        DownloadButton,
        PreviewOverlay,
        FiletypeOverlay
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
    methods: {
        ...mapMutations("mail", {
            SET_PREVIEW_MESSAGE_KEY,
            SET_PREVIEW_FILE_KEY
        }),
        openPreview(file) {
            this.SET_PREVIEW_MESSAGE_KEY(this.message.key);
            this.SET_PREVIEW_FILE_KEY(file.key);
            this.$bvModal.show("preview-modal");
        },
        download(file) {
            this.$refs[`download-button-${file.key}`][0].clickButton();
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
@import "~@bluemind/styleguide/css/_variables";

.preview-message.mail-viewer-content {
    padding-top: $sp-4;
    display: flex;
    flex-direction: column;
    .from {
        display: none;
    }
    * .date {
        text-align: left;
        white-space: nowrap;
        font-weight: $font-weight-bold;
    }
    .mail-viewer-splitter {
        order: 1;
        padding: 0;
        margin-top: $sp-3;
    }
    .mail-viewer-splitter > hr {
        margin: 0;
    }
    .mail-inlines-block {
        padding: 0 $sp-4;
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
    .subject {
        padding-left: $sp-4;
        padding-right: $sp-4;
    }
    & > hr {
        margin: $sp-3 0 0 0;
    }
    .mail-viewer-recipient {
        flex-wrap: nowrap;
        white-space: nowrap;
    }
    .mail-files {
        padding: $sp-2 $sp-4;
        background-color: $neutral-bg-lo1;
        .file-item {
            border-width: 2px !important;
        }

        .active.file-item .container {
            border-color: $secondary-fg !important;
        }
    }
}
</style>
