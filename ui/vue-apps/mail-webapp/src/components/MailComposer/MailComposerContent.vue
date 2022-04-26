<template>
    <mail-viewer-content-loading v-if="loading" class="flex-grow-1 mx-2" />
    <bm-file-drop-zone
        v-else
        class="mail-composer-content z-index-110 as-attachments flex-grow-1"
        file-type-regex="^(?!.*image/(jpeg|jpg|png|gif)).*$"
        at-least-one-match
        @files-count="draggedFilesCount = $event"
        @drop-files="addAttachments($event)"
    >
        <template #dropZone>
            <h2 class="text-center p-2">{{ $tc("mail.new.attachments.drop.zone", draggedFilesCount) }}</h2>
            <bm-icon icon="arrow-up" size="2x" />
        </template>
        <bm-file-drop-zone class="z-index-110 flex-grow-1" inline file-type-regex="image/(jpeg|jpg|png|gif)">
            <template #dropZone>
                <bm-icon class="text-dark" icon="file-type-image" size="2x" />
                <h2 class="text-center p-2">{{ $tc("mail.new.images.drop.zone", draggedFilesCount) }}</h2>
            </template>
            <bm-rich-editor
                ref="message-content"
                :init-value="messageCompose.editorContent"
                :show-toolbar="userPrefIsMenuBarOpened"
                :adapt-output="setCidDataAttr"
                class="flex-grow-1"
                @input="updateEditorContent"
            >
                <bm-button
                    v-if="messageCompose.collapsedContent"
                    variant="outline-dark"
                    class="align-self-start mb-1"
                    @click="expandContent"
                >
                    <bm-icon icon="3dots" size="sm" />
                </bm-button>
                <!-- eslint-disable vue/no-v-html -->
                <div
                    v-if="corporateSignature && !corporateSignature.usePlaceholder"
                    class="cursor-not-allowed"
                    :title="contentIsReadOnly"
                    v-html="corporateSignature.html"
                />
                <div v-if="disclaimer" class="cursor-not-allowed" :title="contentIsReadOnly" v-html="disclaimer.html" />
            </bm-rich-editor>
        </bm-file-drop-zone>
    </bm-file-drop-zone>
</template>

<script>
import { mapMutations, mapState } from "vuex";

import { createCid, CID_DATA_ATTRIBUTE } from "@bluemind/email";
import { BmButton, BmFileDropZone, BmIcon, BmRichEditor } from "@bluemind/styleguide";

import { SET_DRAFT_COLLAPSED_CONTENT, SET_DRAFT_EDITOR_CONTENT } from "~/mutations";
import { isNewMessage } from "~/model/draft";
import { HTML_SIGNATURE_ATTR } from "~/model/signature";
import { ComposerActionsMixin, ComposerInitMixin, SignatureMixin } from "~/mixins";
import MailViewerContentLoading from "../MailViewer/MailViewerContentLoading";

export default {
    name: "MailComposerContent",
    components: { BmButton, BmFileDropZone, BmIcon, BmRichEditor, MailViewerContentLoading },
    mixins: [ComposerActionsMixin, ComposerInitMixin, SignatureMixin],
    props: {
        userPrefIsMenuBarOpened: {
            type: Boolean,
            default: false
        },
        message: {
            type: Object,
            required: true
        }
    },
    data() {
        return { draggedFilesCount: -1, loading: false };
    },
    computed: {
        ...mapState("mail", ["messageCompose"]),
        corporateSignature() {
            return this.messageCompose.corporateSignature;
        },
        disclaimer() {
            return this.messageCompose.disclaimer;
        },
        contentIsReadOnly() {
            return this.$t("mail.compose.read_only");
        }
    },
    watch: {
        "message.key": {
            async handler() {
                if (!isNewMessage(this.message)) {
                    this.loading = true;
                    await this.initFromRemoteMessage(this.message);
                    this.loading = false;
                }

                // focus on content when a recipient is already set
                if (this.message.to.length > 0) {
                    await this.$nextTick();
                    this.$refs["message-content"].focusBeforeCustomContent("[" + HTML_SIGNATURE_ATTR + "]");
                }
            },
            immediate: true
        },
        "messageCompose.editorContent"() {
            if (!this.lock) {
                this.$refs["message-content"]?.setContent(this.messageCompose.editorContent);
            }
        }
    },
    methods: {
        ...mapMutations("mail", [SET_DRAFT_COLLAPSED_CONTENT, SET_DRAFT_EDITOR_CONTENT]),
        updateEditorContent(newContent) {
            try {
                this.lock = true;
                this.SET_DRAFT_EDITOR_CONTENT(newContent);
                this.debouncedSave();
            } finally {
                this.lock = false;
            }
        },
        expandContent() {
            this.SET_DRAFT_EDITOR_CONTENT(this.messageCompose.editorContent + this.messageCompose.collapsedContent);
            this.SET_DRAFT_COLLAPSED_CONTENT(null);
        },
        toggleSignature(signature) {
            this.$refs["message-content"].toggleCustomContent(signature, HTML_SIGNATURE_ATTR);
        },
        setCidDataAttr(container) {
            const images = container.querySelectorAll('img[src^="data:image"]:not([' + CID_DATA_ATTRIBUTE + "])");
            images.forEach(imgNode => imgNode.setAttribute(CID_DATA_ATTRIBUTE, createCid()));
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-composer-content {
    .bm-rich-editor .roosterjs-container {
        min-height: 12rem;
    }
}
</style>
