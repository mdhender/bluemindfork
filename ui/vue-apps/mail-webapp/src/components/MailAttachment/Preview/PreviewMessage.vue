<template>
    <mail-viewer-content class="preview-message" :message="message">
        <template #attachments-block="scope">
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
                    <template #actions="{ file: slotFile }">
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
import { mapActions, mapMutations, mapState } from "vuex";
import { partUtils, fileUtils } from "@bluemind/mail";
import { SET_PREVIEW } from "~/actions";
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
    computed: {
        ...mapState("mail", {
            filesToPreview: state => state.files || {}
        })
    },
    methods: {
        ...mapActions("mail", {
            SET_PREVIEW
        }),
        download(file) {
            this.$refs.toolbar[0].download(file);
        },
        previewOrDownload(file) {
            if (!isUploading(file)) {
                if (isAllowedToPreview(file)) {
                    this.previewFile(file.key);
                } else {
                    this.download(file);
                }
            }
        },
        previewFile(fileKey) {
            this.SET_PREVIEW({ messageKey: this.message.key, fileKey: fileKey });
        },
        isViewable,
        isLarge
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.preview-message.mail-viewer-content {
    display: flex;
    flex-direction: column;

    .sender .contact {
        display: none;
    }
    * .date {
        white-space: nowrap;
        color: $neutral-fg-lo1;
    }
    .mail-inlines-block {
        padding-left: $sp-6;
        padding-right: $sp-5;
    }
    .mail-sender-splitter {
        display: none;
    }
    .sender-and-recipients {
        display: none;
    }
    .body-viewer {
        order: 2;
        margin-top: $sp-5;
        padding: 0;
    }
    .title,
    * .date {
        padding-left: $sp-6;
        padding-right: $sp-5;
    }
    .title {
        padding-top: $sp-5;
        color: $neutral-fg-hi1;
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

        .files-header {
            color: $neutral-fg-hi1;
        }
    }
}
</style>
