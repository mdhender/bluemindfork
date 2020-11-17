<template>
    <bm-form class="mail-composer p-lg-3 flex-grow-1 d-flex">
        <mail-composer-panel class="flex-grow-1">
            <template #header>
                <h3 class="d-none d-lg-flex text-nowrap text-truncate card-header px-2 py-1">
                    {{ panelTitle }}
                </h3>
            </template>
            <template #body>
                <div class="pl-3">
                    <mail-composer-recipients
                        ref="recipients"
                        :message="message"
                        :is-reply-or-forward="!!messageCompose.collapsedContent"
                        @save-draft="saveDraft"
                    />
                    <bm-form-input
                        :value="message.subject.trim()"
                        class="mail-composer-subject d-flex align-items-center"
                        :placeholder="$t('mail.new.subject.placeholder')"
                        :aria-label="$t('mail.new.subject.aria')"
                        type="text"
                        @input="updateSubject"
                        @keydown.enter.native.prevent
                    />
                </div>
                <hr class="mail-composer-splitter m-0" />
                <bm-row class="mb-2">
                    <bm-col cols="12">
                        <bm-file-drop-zone
                            class="z-index-110 attachments"
                            file-type-regex="image/(jpeg|jpg|png|gif)"
                            @drop-files="addAttachments($event)"
                        >
                            <template #dropZone>
                                <bm-icon icon="paper-clip" size="2x" />
                                <h2 class="text-center p-2">
                                    {{ $tc("mail.new.attachments.images.drop.zone", draggedFilesCount) }}
                                </h2>
                            </template>
                            <mail-attachments-block v-if="message.attachments.length > 0" :message="message" expanded />
                        </bm-file-drop-zone>
                    </bm-col>
                </bm-row>
                <bm-file-drop-zone
                    class="z-index-110 as-attachments flex-grow-1"
                    file-type-regex="^(?!.*image/(jpeg|jpg|png|gif)).*$"
                    at-least-one-match
                    @files-count="draggedFilesCount = $event"
                    @drop-files="addAttachments($event)"
                >
                    <template #dropZone>
                        <h2 class="text-center p-2">{{ $tc("mail.new.attachments.drop.zone", draggedFilesCount) }}</h2>
                        <bm-icon icon="arrow-up" size="2x" />
                    </template>
                    <bm-file-drop-zone
                        class="z-index-110 flex-grow-1"
                        inline
                        file-type-regex="image/(jpeg|jpg|png|gif)"
                    >
                        <template #dropZone>
                            <bm-icon class="text-dark" icon="file-type-image" size="2x" />
                            <h2 class="text-center p-2">{{ $tc("mail.new.images.drop.zone", draggedFilesCount) }}</h2>
                        </template>
                        <bm-form-textarea
                            v-if="userPrefTextOnly"
                            ref="message-content"
                            :value="messageCompose.editorContent"
                            :rows="10"
                            :max-rows="10000"
                            :aria-label="$t('mail.new.content.aria')"
                            class="mail-content"
                            no-resize
                            @input="updateEditorContent"
                        />
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
                <bm-button
                    v-if="userPrefTextOnly && messageCompose.collapsedContent"
                    variant="outline-dark"
                    class="align-self-start"
                    @click="expandContent"
                >
                    <bm-icon icon="3dots" size="sm" />
                </bm-button>
            </template>
            <template #footer>
                <mail-composer-footer
                    :message="message"
                    :user-pref-text-only="userPrefTextOnly"
                    :user-pref-is-menu-bar-opened="userPrefIsMenuBarOpened"
                    :signature="signature"
                    :is-signature-inserted="isSignatureInserted"
                    @toggle-text-format="userPrefIsMenuBarOpened = !userPrefIsMenuBarOpened"
                    @delete="deleteDraft"
                    @send="send"
                    @add-attachments="addAttachments"
                    @toggle-signature="toggleSignature"
                />
            </template>
        </mail-composer-panel>
    </bm-form>
</template>

<script>
import { mapGetters, mapActions, mapMutations, mapState } from "vuex";

import { InlineImageHelper, MimeType } from "@bluemind/email";
import { sanitizeHtml } from "@bluemind/html-utils";
import ItemUri from "@bluemind/item-uri";
import {
    BmButton,
    BmCol,
    BmFormInput,
    BmForm,
    BmFormTextarea,
    BmIcon,
    BmRichEditor,
    BmRow,
    BmFileDropZone
} from "@bluemind/styleguide";

import MailAttachmentsBlock from "../MailAttachment/MailAttachmentsBlock";
import MailComposerRecipients from "./MailComposerRecipients";
import MailComposerFooter from "./MailComposerFooter";
import MailComposerPanel from "./MailComposerPanel";
import { updateKey } from "../../model/message";
import { getPartsFromCapabilities } from "../../model/part";
import { getEditorContent, COMPOSER_CAPABILITIES, handleSeparator, isInternalIdFaked } from "../../model/draft";
import { addSignature, isHtmlSignaturePresent, isTextSignaturePresent, removeSignature } from "../../model/signature";
import {
    ADD_MESSAGES,
    REMOVE_MESSAGES,
    SET_DRAFT_COLLAPSED_CONTENT,
    SET_DRAFT_EDITOR_CONTENT,
    SET_MESSAGE_SUBJECT,
    SET_SAVED_INLINE_IMAGES,
    UNSELECT_ALL_MESSAGES
} from "~mutations";
import { ADD_ATTACHMENTS, FETCH_ACTIVE_MESSAGE_INLINE_PARTS, SAVE_MESSAGE, SEND_MESSAGE } from "~actions";
import { MY_DRAFTS, MY_OUTBOX, MY_SENT, MY_MAILBOX_KEY } from "~getters";

export default {
    name: "MailComposer",
    components: {
        BmButton,
        BmCol,
        BmFileDropZone,
        BmFormInput,
        BmForm,
        BmFormTextarea,
        BmIcon,
        MailComposerPanel,
        BmRichEditor,
        BmRow,
        MailComposerFooter,
        MailComposerRecipients,
        MailAttachmentsBlock
    },
    props: {
        messageKey: {
            type: String,
            required: true
        }
    },
    data() {
        return {
            userPrefIsMenuBarOpened: false, // TODO: initialize this with user setting
            userPrefTextOnly: false, // TODO: initialize this with user setting
            draggedFilesCount: -1
        };
    },
    computed: {
        ...mapState("mail", ["messages", "messageCompose", "activeMessage"]),
        ...mapState("session", { settings: "userSettings" }),
        ...mapGetters("mail", { MY_DRAFTS, MY_OUTBOX, MY_SENT, MY_MAILBOX_KEY }),
        message() {
            return this.messages[this.messageKey];
        },
        signature() {
            return this.messageCompose.signature;
        },
        panelTitle() {
            return this.message.subject.trim() ? this.message.subject : this.$t("mail.main.new");
        },
        isSignatureInserted() {
            return (
                this.signature &&
                (this.userPrefTextOnly
                    ? isTextSignaturePresent(this.messageCompose.editorContent, this.signature)
                    : isHtmlSignaturePresent(this.messageCompose.editorContent, this.signature))
            );
        },
        isMessageOnlyLocal() {
            return isInternalIdFaked(this.message.remoteRef.internalId);
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
                await this.initEditorContent();
                await this.focus();
            },
            immediate: true
        }
    },
    created() {
        this.UNSELECT_ALL_MESSAGES();
    },
    destroyed() {
        this.cleanComposer();
    },
    methods: {
        ...mapActions("mail", { ADD_ATTACHMENTS, FETCH_ACTIVE_MESSAGE_INLINE_PARTS, SAVE_MESSAGE, SEND_MESSAGE }),
        ...mapActions("mail-webapp", ["purge"]),
        ...mapMutations("mail", {
            ADD_MESSAGES,
            REMOVE_MESSAGES,
            SET_DRAFT_COLLAPSED_CONTENT,
            SET_DRAFT_EDITOR_CONTENT,
            SET_MESSAGE_SUBJECT,
            SET_SAVED_INLINE_IMAGES,
            UNSELECT_ALL_MESSAGES
        }),
        updateSubject(subject) {
            this.SET_MESSAGE_SUBJECT({ messageKey: this.messageKey, subject });
            this.saveDraft();
        },
        async initEditorContent() {
            if (!this.isMessageOnlyLocal) {
                const parts = getPartsFromCapabilities(this.message, COMPOSER_CAPABILITIES);

                await this.FETCH_ACTIVE_MESSAGE_INLINE_PARTS({
                    folderUid: this.message.folderRef.uid,
                    imapUid: this.message.remoteRef.imapUid,
                    inlines: parts.filter(part => MimeType.isHtml(part) || MimeType.isText(part))
                });

                let content = getEditorContent(
                    this.userPrefTextOnly,
                    parts,
                    this.message,
                    this.activeMessage.partsDataByAddress
                );
                if (!this.userPrefTextOnly) {
                    const partsWithCid = parts.filter(part => MimeType.isImage(part) && part.contentId);

                    const insertionResult = await InlineImageHelper.insertAsUrl(
                        [content],
                        partsWithCid,
                        this.message.folderRef.uid,
                        this.message.remoteRef.imapUid
                    );
                    this.SET_SAVED_INLINE_IMAGES(insertionResult.imageInlined);
                    content = insertionResult.contentsWithImageInserted[0];
                    content = sanitizeHtml(content);
                }
                const editorData = handleSeparator(content);

                if (this.signature && this.settings.insert_signature === "true") {
                    editorData.content = addSignature(editorData.content, this.userPrefTextOnly, this.signature);
                }

                this.SET_DRAFT_COLLAPSED_CONTENT(editorData.collapsed);
                this.SET_DRAFT_EDITOR_CONTENT(editorData.content);
                this.updateHtmlComposer();
            }
        },
        toggleSignature() {
            if (!this.isSignatureInserted) {
                this.SET_DRAFT_EDITOR_CONTENT(
                    addSignature(this.messageCompose.editorContent, this.userPrefTextOnly, this.signature)
                );
            } else {
                this.SET_DRAFT_EDITOR_CONTENT(
                    removeSignature(this.messageCompose.editorContent, this.userPrefTextOnly, this.signature)
                );
            }
        },
        cleanComposer() {
            this.SET_DRAFT_EDITOR_CONTENT("");
            this.SET_DRAFT_COLLAPSED_CONTENT(null);
        },
        async expandContent() {
            this.SET_DRAFT_EDITOR_CONTENT(this.messageCompose.editorContent + this.messageCompose.collapsedContent);
            this.SET_DRAFT_COLLAPSED_CONTENT(null);
            await this.setCursorInEditor();
        },
        async focus() {
            if (this.message.to.length > 0) {
                await this.setCursorInEditor();
            } else {
                await this.$nextTick();
                this.$refs.recipients.focus();
            }
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
        async updateEditorContent(newContent) {
            this.SET_DRAFT_EDITOR_CONTENT(newContent);
            this.saveDraft();
        },
        addAttachments(files) {
            this.ADD_ATTACHMENTS({
                messageKey: this.messageKey,
                files,
                userPrefTextOnly: this.userPrefTextOnly,
                myDraftsFolderKey: this.MY_DRAFTS.key,
                messageCompose: this.messageCompose
            });
        },
        send() {
            this.SEND_MESSAGE({
                userPrefTextOnly: this.userPrefTextOnly,
                draftKey: this.messageKey,
                myMailboxKey: this.MY_MAILBOX_KEY,
                outboxId: this.MY_OUTBOX.remoteRef.internalId,
                myDraftsFolder: this.MY_DRAFTS,
                sentFolder: this.MY_SENT,
                messageCompose: this.messageCompose
            });
            this.$router.navigate("v:mail:home");
        },
        async saveDraft() {
            const wasMessageOnlyLocal = this.isMessageOnlyLocal;
            await this.SAVE_MESSAGE({
                userPrefTextOnly: this.userPrefTextOnly,
                draftKey: this.messageKey,
                myDraftsFolderKey: this.MY_DRAFTS.key,
                messageCompose: this.messageCompose,
                debounceTime: 3000
            });
            if (wasMessageOnlyLocal) {
                const message = updateKey(this.message, this.message.remoteRef.internalId, this.MY_DRAFTS);
                this.ADD_MESSAGES([message]);

                this.$router.navigate({ name: "v:mail:message", params: { message: message.key } });
            }
        },
        async deleteDraft() {
            if (this.isMessageOnlyLocal) {
                this.$router.navigate("v:mail:home");
            } else {
                const confirm = await this.$bvModal.msgBoxConfirm(this.$t("mail.draft.delete.confirm.content"), {
                    title: this.$t("mail.draft.delete.confirm.title"),
                    okTitle: this.$t("common.delete"),
                    cancelVariant: "outline-secondary",
                    cancelTitle: this.$t("common.cancel"),
                    centered: true,
                    hideHeaderClose: false,
                    autoFocusButton: "ok"
                });
                if (confirm) {
                    this.purge(this.messageKey);
                    this.$router.navigate("v:mail:home");
                }
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-composer {
    word-break: break-word !important;

    .row {
        min-height: fit-content;
    }

    .mail-composer-splitter {
        border-top-color: $alternate-light;
    }

    .mail-composer-subject input,
    .bm-contact-input input,
    textarea {
        border: none;
    }

    input:focus,
    textarea:focus {
        box-shadow: none;
    }

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

    .mail-composer-subject {
        min-height: 2.5rem;
    }

    .bm-file-drop-zone.attachments .bm-dropzone-active-content {
        min-height: 7em;
    }

    .bm-file-drop-zone.as-attachments.bm-dropzone-active,
    .bm-file-drop-zone.as-attachments.bm-dropzone-hover {
        background: url("~@bluemind/styleguide/assets/attachment.png") no-repeat center center;
    }

    .mail-composer-panel-footer {
        display: none;
    }

    @include media-breakpoint-up(lg) {
        .mail-composer-panel-footer {
            display: block;
        }
    }
}
</style>
