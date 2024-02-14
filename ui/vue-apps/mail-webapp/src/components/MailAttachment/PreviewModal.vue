<template>
    <bm-modal
        id="preview-modal"
        ref="modal"
        class="preview-modal position-relative"
        variant="advanced"
        size="xl"
        height="lg"
        hide-footer
        :scrollable="false"
        @hidden="RESET_PREVIEW"
    >
        <global-events @keydown.left="previous" @keydown.up="previous" @keydown.down="next" @keydown.right="next" />

        <template #modal-header>
            <preview-header
                :file="file"
                :files-count="filesCount"
                :message="message"
                :expanded.sync="expanded"
                @close="$refs.modal.hide()"
                @previous="previous"
                @next="next"
            />
        </template>

        <div class="content">
            <bm-collapse v-model="expanded" class="scroller-y desktop-only" :class="{ 'd-none': !expanded }">
                <preview-message :message="message" :active-file="file" />
            </bm-collapse>
            <div class="main-part">
                <bm-alert-area class="preview-alert-area" :alerts="alerts" @remove="REMOVE">
                    <template #default="context">
                        <component :is="context.alert.renderer" :alert="context.alert" />
                    </template>
                </bm-alert-area>
                <preview-file :message="message" :file="file" />
            </div>
        </div>
    </bm-modal>
</template>

<script>
import { mapActions, mapMutations, mapState } from "vuex";
import { BmAlertArea, BmCollapse, BmModal } from "@bluemind/ui-components";
import { REMOVE } from "@bluemind/alert.store";
import { attachmentUtils, messageUtils } from "@bluemind/mail";

import { RESET_PREVIEW, SET_PREVIEW_FILE_KEY } from "~/mutations";
import PreviewFile from "./Preview/PreviewFile";
import PreviewMessage from "./Preview/PreviewMessage";
import PreviewHeader from "./Preview/PreviewHeader";
import GlobalEvents from "vue-global-events";
import PreviewFileHeader from "./Preview/PreviewFileHeader";
const { computeParts } = messageUtils;

const { AttachmentAdaptor } = attachmentUtils;

export default {
    name: "PreviewModal",
    components: {
        BmAlertArea,
        BmCollapse,
        BmModal,
        PreviewFile,
        PreviewMessage,
        PreviewHeader,
        GlobalEvents
    },
    data() {
        return { expanded: true, computedParts: {} };
    },
    computed: {
        ...mapState({
            alerts: state => state.alert.filter(({ area }) => !area),
            fileKeyToPreview: state => state.mail.preview.fileKey
        }),
        files() {
            return AttachmentAdaptor.extractFiles(this.computedParts.attachments, this.message);
        },
        message() {
            const message = this.$store.state.mail.conversations.messages[this.$store.state.mail.preview.messageKey];
            return message
                ? {
                      ...message,
                      composing: false
                  }
                : undefined;
        },
        file() {
            const file = this.files.find(({ key }) => key === this.fileKeyToPreview);
            const uploadingFile = this.$store.state.mail.messageCompose.uploadingFiles[this.fileKeyToPreview];
            return uploadingFile ? Object.assign(file, uploadingFile) : file;
        },
        fileIndex() {
            return this.files.findIndex(({ key }) => this.fileKeyToPreview === key);
        },
        filesCount() {
            return this.files.length;
        }
    },
    watch: {
        "message.structure": {
            handler(structure) {
                this.computedParts = computeParts(structure);
            },
            immediate: true
        }
    },
    methods: {
        ...mapActions("alert", { REMOVE }),
        ...mapMutations("mail", { RESET_PREVIEW, SET_PREVIEW_FILE_KEY }),
        next() {
            this.selectPreview(this.fileIndex, index => index + 1);
        },
        previous() {
            this.selectPreview(this.fileIndex, index => index + this.filesCount - 1);
        },
        selectPreview(current, iterator) {
            const index = iterator(current) % this.filesCount;
            if (index !== this.fileIndex) {
                const file = this.files[index];
                this.SET_PREVIEW_FILE_KEY(file.key);
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";

#preview-modal .modal-content {
    background-color: $surface;

    .preview-message-header,
    .collapse {
        flex: none;
        width: 25%;
    }

    .modal-header {
        .preview-header {
            flex: 1;
            z-index: 1;
        }

        border-bottom: none !important;
    }

    .modal-body {
        padding: 0;
        .content {
            display: flex;
            height: 100%;
        }
        .main-part {
            flex: 1;
            position: relative;

            @include from-lg {
                &::before {
                    content: "";
                    position: absolute;
                    top: 0;
                    bottom: 0;
                    left: 0;
                    right: 0;
                    pointer-events: none;
                    box-shadow: inset 0 1px 0 $neutral-fg-lo3; // "inner" border-top
                }
            }

            .preview-alert-area {
                position: absolute;
                width: 100%;
                margin-bottom: 0;
            }

            .preview-file {
                height: 100%;
            }
        }
    }
}
</style>
