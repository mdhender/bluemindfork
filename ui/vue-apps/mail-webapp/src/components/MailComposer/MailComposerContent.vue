<template>
    <bm-file-drop-zone
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
                :value="messageCompose.editorContent"
                :is-menu-bar-opened="userPrefIsMenuBarOpened"
                class="flex-grow-1"
                @input="updateEditorContent"
            >
                <bm-button
                    v-if="messageCompose.collapsedContent"
                    variant="outline-dark"
                    class="align-self-start ml-3 mb-2"
                    @click="expandContent"
                >
                    <bm-icon icon="3dots" size="sm" />
                </bm-button>
            </bm-rich-editor>
        </bm-file-drop-zone>
    </bm-file-drop-zone>
</template>

<script>
import { mapMutations, mapState } from "vuex";

import ItemUri from "@bluemind/item-uri";
import { BmButton, BmFileDropZone, BmIcon, BmRichEditor } from "@bluemind/styleguide";

import { REMOVE_MESSAGES, SET_DRAFT_COLLAPSED_CONTENT, SET_DRAFT_EDITOR_CONTENT } from "~mutations";
import { isInternalIdFaked } from "~model/draft";
import { ComposerActionsMixin, ComposerInitMixin } from "~mixins";

export default {
    name: "MailComposerContent",
    components: {
        BmButton,
        BmFileDropZone,
        BmIcon,
        BmRichEditor
    },
    mixins: [ComposerActionsMixin, ComposerInitMixin],
    props: {
        userPrefIsMenuBarOpened: {
            type: Boolean,
            default: false
        },
        messageKey: {
            type: String,
            required: true
        }
    },
    data() {
        return { draggedFilesCount: -1 };
    },
    computed: {
        ...mapState("mail", ["messages", "messageCompose"]),
        message() {
            return this.messages[this.messageKey];
        }
    },
    watch: {
        messageKey: {
            handler: async function (newKey, oldKey) {
                if (oldKey && isInternalIdFaked(ItemUri.item(oldKey))) {
                    // when route changes due to an internalId update, preserve component state
                    return;
                }
                if (!isInternalIdFaked(this.message.remoteRef.internalId)) {
                    await this.initFromRemoteMessage(this.message);
                    await this.updateHtmlComposer();
                    this.focus();
                } else {
                    await this.updateHtmlComposer();
                }
            },
            immediate: true
        }
    },
    methods: {
        ...mapMutations("mail", [REMOVE_MESSAGES, SET_DRAFT_COLLAPSED_CONTENT, SET_DRAFT_EDITOR_CONTENT]),
        async updateEditorContent(newContent) {
            this.SET_DRAFT_EDITOR_CONTENT(newContent);
            this.debouncedSave();
        },
        async expandContent() {
            this.SET_DRAFT_EDITOR_CONTENT(this.messageCompose.editorContent + this.messageCompose.collapsedContent);
            this.SET_DRAFT_COLLAPSED_CONTENT(null);
            await this.updateHtmlComposer();
            this.focus();
        },
        focus() {
            this.$refs["message-content"].focus("start");
        },
        async updateHtmlComposer() {
            await this.$nextTick();
            this.$refs["message-content"].updateContent();
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-composer-content {
    .bm-rich-editor-content .ProseMirror {
        min-height: 12rem;
    }

    .mail-content {
        overflow: auto !important;
    }
}
</style>
