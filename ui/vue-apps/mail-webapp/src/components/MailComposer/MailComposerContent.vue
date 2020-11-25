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
            <template v-if="userPrefTextOnly">
                <bm-form-textarea
                    ref="message-content"
                    :value="messageCompose.editorContent"
                    :rows="10"
                    :max-rows="10000"
                    :aria-label="$t('mail.new.content.aria')"
                    class="mail-content"
                    no-resize
                    @input="updateEditorContent"
                />
                <bm-button
                    v-if="messageCompose.collapsedContent"
                    variant="outline-dark"
                    class="align-self-start"
                    @click="expandContent"
                >
                    <bm-icon icon="3dots" size="sm" />
                </bm-button>
            </template>
            <bm-rich-editor
                v-else
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
import { BmButton, BmFileDropZone, BmIcon, BmFormTextarea, BmRichEditor } from "@bluemind/styleguide";

import { REMOVE_MESSAGES, SET_DRAFT_COLLAPSED_CONTENT, SET_DRAFT_EDITOR_CONTENT } from "~mutations";
import { isInternalIdFaked } from "../../model/draft";
import ComposerActionsMixin from "../ComposerActionsMixin";
import ComposerInitMixin from "../ComposerInitMixin";

export default {
    name: "MailComposerContent",
    components: {
        BmButton,
        BmFileDropZone,
        BmIcon,
        BmFormTextarea,
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
                if (oldKey) {
                    // when route changes due to an internalId update, preserve component state
                    if (isInternalIdFaked(ItemUri.item(oldKey))) {
                        this.REMOVE_MESSAGES([oldKey]); // delete obsolete message
                        return;
                    }
                    this.cleanComposer();
                }
                if (!isInternalIdFaked(this.message.remoteRef.internalId)) {
                    await this.initFromRemoteMessage(this.message);
                    this.updateHtmlComposer();
                }
                await this.focus();
            },
            immediate: true
        }
    },
    destroyed() {
        this.cleanComposer();
    },
    methods: {
        ...mapMutations("mail", [REMOVE_MESSAGES, SET_DRAFT_COLLAPSED_CONTENT, SET_DRAFT_EDITOR_CONTENT]),
        async updateEditorContent(newContent) {
            this.SET_DRAFT_EDITOR_CONTENT(newContent);
            this.save();
        },
        async expandContent() {
            this.SET_DRAFT_EDITOR_CONTENT(this.messageCompose.editorContent + this.messageCompose.collapsedContent);
            this.SET_DRAFT_COLLAPSED_CONTENT(null);
            await this.setCursorInEditor();
        },
        async setCursorInEditor() {
            if (this.userPrefTextOnly) {
                this.$refs["message-content"].focus();
                this.$refs["message-content"].setSelectionRange(0, 0);
            } else {
                await this.updateHtmlComposer();
                this.$refs["message-content"].focus("start");
            }
        },
        async updateHtmlComposer() {
            await this.$nextTick();
            this.$refs["message-content"].updateContent();
        },
        cleanComposer() {
            this.SET_DRAFT_EDITOR_CONTENT("");
            this.SET_DRAFT_COLLAPSED_CONTENT(null);
        },
        async focus() {
            if (this.message.to.length > 0) {
                await this.setCursorInEditor();
            } else {
                await this.$nextTick();
                this.$refs.recipients.focus();
            }
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

    .ProseMirror,
    .mail-content {
        padding: $sp-2 $sp-3;
    }
}
</style>
