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
            :dark-mode="IS_COMPUTED_THEME_DARK"
            :default-font-family="composer_default_font"
            :extra-font-families="EXTRA_FONT_FAMILIES"
            class="flex-grow-1"
            name="composer"
            @input="updateEditorContent"
        >
            <bm-icon-button
                v-if="messageCompose.collapsedContent"
                size="sm"
                class="align-self-start mx-5 my-6"
                icon="3dots"
                @click="expandContent"
            />
        </bm-rich-editor>
    </bm-file-drop-zone>
</template>

<script>
import debounce from "lodash.debounce";
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";

import { createCid, CID_DATA_ATTRIBUTE } from "@bluemind/email";
import { BmFileDropZone, BmIcon, BmIconButton, BmRichEditor } from "@bluemind/ui-components";
import { draftUtils, signatureUtils } from "@bluemind/mail";

import { SET_DRAFT_CONTENT } from "~/actions";
import { SET_DRAFT_COLLAPSED_CONTENT, SET_DRAFT_EDITOR_CONTENT } from "~/mutations";
import { ComposerActionsMixin, FileDropzoneMixin, SignatureMixin, WaitForMixin } from "~/mixins";
import MailViewerContentLoading from "../MailViewer/MailViewerContentLoading";
import { useComposerInit } from "~/composables/composer/ComposerInit";
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
    mixins: [ComposerActionsMixin, FileDropzoneMixin, SignatureMixin, WaitForMixin],
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    setup() {
        const { initFromRemoteMessage } = useComposerInit();
        return { initFromRemoteMessage };
    },
    data() {
        return { componentGotMounted: false, draggedFilesCount: -1, loading: false };
    },
    computed: {
        ...mapState("mail", ["messageCompose"]),
        ...mapState("settings", ["composer_default_font"]),
        ...mapGetters("settings", ["IS_COMPUTED_THEME_DARK", "EXTRA_FONT_FAMILIES"])
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
                    this.$refs["message-content"].focusBefore(PERSONAL_SIGNATURE_SELECTOR(this.signature?.id));
                }
            },
            immediate: true
        },
        async "messageCompose.editorContent"() {
            if (!this.loading) {
                const editor = await this.getEditorRef();
                if (editor.getContent() !== this.messageCompose.editorContent) {
                    editor.setContent(this.messageCompose.editorContent);
                }
            }
        }
    },
    mounted() {
        this.componentGotMounted = true;
    },
    methods: {
        ...mapMutations("mail", [SET_DRAFT_COLLAPSED_CONTENT, SET_DRAFT_EDITOR_CONTENT]),
        ...mapActions("mail", { SET_DRAFT_CONTENT }),
        async updateEditorContent(newContent) {
            await this.SET_DRAFT_CONTENT({ draft: this.message, html: newContent });
            this.debouncedSave();
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
@import "~@bluemind/ui-components/src/css/utils/variables";

.mail-composer-content {
    .bm-rich-editor .roosterjs-container {
        min-height: 12rem;
        padding-top: $sp-6;
        padding-bottom: $sp-5;
    }
}
</style>
