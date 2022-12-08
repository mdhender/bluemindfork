<template>
    <mail-viewer-content-loading v-if="loading" class="flex-grow-1 mx-2" />
    <bm-file-drop-zone
        v-else
        class="mail-composer-content z-index-110 flex-grow-1"
        inline
        :should-activate-fn="shouldActivateForImages"
    >
        <template #dropZone>
            <div class="d-flex flex-column justify-content-start flex-fill align-items-center mt-6">
                <bm-icon class="text-neutral" icon="file-type-image" size="xl" />
                <div class="text-neutral p-4">
                    <h3 class="p-2">{{ $tc("mail.new.images.drop.zone", draggedFilesCount) }}</h3>
                </div>
            </div>
        </template>
        <bm-rich-editor
            ref="message-content"
            :init-value="messageCompose.editorContent"
            :show-toolbar="false"
            :adapt-output="setCidDataAttr"
            class="flex-grow-1"
            name="composer"
            @input="updateEditorContent"
        >
            <bm-icon-button
                v-if="messageCompose.collapsedContent"
                size="sm"
                class="align-self-start mb-1"
                icon="3dots"
                @click="expandContent"
            />
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
</template>

<script>
import { mapMutations, mapState } from "vuex";

import { createCid, CID_DATA_ATTRIBUTE } from "@bluemind/email";
import { BmFileDropZone, BmIcon, BmIconButton, BmRichEditor } from "@bluemind/ui-components";
import { draftUtils, signatureUtils } from "@bluemind/mail";

import { SET_DRAFT_COLLAPSED_CONTENT, SET_DRAFT_EDITOR_CONTENT } from "~/mutations";
import { ComposerActionsMixin, ComposerInitMixin, FileDropzoneMixin, SignatureMixin, WaitForMixin } from "~/mixins";
import MailViewerContentLoading from "../MailViewer/MailViewerContentLoading";

const { isNewMessage } = draftUtils;
const { PERSONAL_SIGNATURE_SELECTOR } = signatureUtils;

export default {
    name: "MailComposerContent",
    components: {
        BmFileDropZone,
        BmIcon,
        BmIconButton,
        BmRichEditor,
        MailViewerContentLoading
    },
    mixins: [ComposerActionsMixin, ComposerInitMixin, FileDropzoneMixin, SignatureMixin, WaitForMixin],
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    data() {
        return { componentGotMounted: false, draggedFilesCount: -1, loading: false };
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
            return this.$t("mail.compose.corporate_signature.read_only");
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
                    await this.getEditorRef();
                    this.$refs["message-content"].focusBefore(PERSONAL_SIGNATURE_SELECTOR(this.personalSignature.id));
                }
            },
            immediate: true
        },
        async "messageCompose.editorContent"() {
            if (!this.lock && !this.loading) {
                await this.getEditorRef();
                this.$refs["message-content"].setContent(this.messageCompose.editorContent);
            }
        }
    },
    mounted() {
        this.componentGotMounted = true;
    },
    methods: {
        ...mapMutations("mail", [SET_DRAFT_COLLAPSED_CONTENT, SET_DRAFT_EDITOR_CONTENT]),
        async updateEditorContent(newContent) {
            try {
                this.lock = true;
                this.SET_DRAFT_EDITOR_CONTENT(newContent);
                this.debouncedSave();
                await this.$nextTick();
            } finally {
                this.lock = false;
            }
        },
        expandContent() {
            this.SET_DRAFT_EDITOR_CONTENT(this.messageCompose.editorContent + this.messageCompose.collapsedContent);
            this.SET_DRAFT_COLLAPSED_CONTENT(null);
        },
        setCidDataAttr(container) {
            const images = container.querySelectorAll('img[src^="data:image"]:not([' + CID_DATA_ATTRIBUTE + "])");
            images.forEach(imgNode => imgNode.setAttribute(CID_DATA_ATTRIBUTE, createCid()));
        },
        async getEditorRef() {
            await this.$waitFor("componentGotMounted");
            await this.$waitFor("loading", loading => loading === false); // component must be loaded to be able to use ref
            return this.$refs["message-content"];
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

.mail-composer-content {
    .bm-rich-editor .roosterjs-container {
        min-height: 12rem;
    }
}
</style>
