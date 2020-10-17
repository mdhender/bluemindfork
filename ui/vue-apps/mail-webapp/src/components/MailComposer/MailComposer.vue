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
                        :is-reply-or-forward="isReplyOrForward"
                        @save-draft="saveDraft"
                    />
                    <bm-form-input
                        :value="message.subject"
                        class="mail-composer-subject d-flex align-items-center"
                        :placeholder="$t('mail.new.subject.placeholder')"
                        :aria-label="$t('mail.new.subject.aria')"
                        type="text"
                        @input="updateSubject"
                        @keydown.enter.native.prevent
                    />
                </div>
                <bm-row class="d-block m-0"><hr class="bg-dark m-0" /></bm-row>
                <bm-row class="mt-1 mb-2">
                    <bm-col cols="12">
                        <bm-file-drop-zone
                            class="z-index-110 attachments"
                            file-type-regex="image/(jpeg|jpg|png|gif)"
                            @dropFiles="addAttachments($event)"
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
                    class="z-index-110 as-attachments"
                    file-type-regex="^(?!.*image/(jpeg|jpg|png|gif)).*$"
                    at-least-one-match
                    @filesCount="draggedFilesCount = $event"
                    @dropFiles="addAttachments($event)"
                >
                    <template #dropZone>
                        <h2 class="text-center p-2">{{ $tc("mail.new.attachments.drop.zone", draggedFilesCount) }}</h2>
                        <bm-icon icon="arrow-up" size="2x" />
                    </template>
                    <bm-file-drop-zone class="z-index-110" inline file-type-regex="image/(jpeg|jpg|png|gif)">
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
                            class="h-100"
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
                    @toggle-text-format="userPrefIsMenuBarOpened = !userPrefIsMenuBarOpened"
                    @delete="deleteDraft"
                    @send="send"
                    @add-attachments="addAttachments"
                />
            </template>
        </mail-composer-panel>
    </bm-form>
</template>

<script>
import { mapGetters, mapActions, mapMutations, mapState } from "vuex";
import debounce from "lodash/debounce";

import { InlineImageHelper, MimeType } from "@bluemind/email";
import { sanitizeHtml } from "@bluemind/html-utils";
import { inject } from "@bluemind/inject";
import {
    BmButton,
    BmCol,
    BmFormInput,
    BmForm,
    BmFormTextarea,
    BmIcon,
    BmRichEditor,
    BmRow,
    BmTooltip,
    BmFileDropZone
} from "@bluemind/styleguide";

import MailAttachmentsBlock from "../MailAttachment/MailAttachmentsBlock";
import MailComposerRecipients from "./MailComposerRecipients";
import MailComposerFooter from "./MailComposerFooter";
import MailComposerPanel from "./MailComposerPanel";
import actionTypes from "../../store/actionTypes";
import mutationTypes from "../../store/mutationTypes";
import { fetch, isEmpty, MessageForwardAttributeSeparator, MessageReplyAttributeSeparator } from "../../model/message";
import PlayWithInlinePartsByCapabilities from "../../store/messages/helpers/PlayWithInlinePartsByCapabilities";

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
    directives: { BmTooltip },
    props: {
        messageKey: {
            type: String,
            required: true
        }
    },
    data() {
        return {
            debouncedSave: debounce(
                () =>
                    this.save({
                        userPrefTextOnly: this.userPrefTextOnly,
                        draftKey: this.messageKey,
                        myDraftsFolderKey: this.MY_DRAFTS.key,
                        messageCompose: this.messageCompose
                    }),
                1500 // FIXME: was useful for testing purpose but for prod ? 10s ?
            ),
            userPrefIsMenuBarOpened: false, // TODO: initialize this with user setting
            userPrefTextOnly: false, // TODO: initialize this with user setting
            draggedFilesCount: -1,
            isReplyOrForward: false,
            lockUpdateHtmlComposer: false,
            blobsUrl: []
        };
    },
    computed: {
        ...mapGetters("mail-webapp", ["lastRecipients"]),
        ...mapState("mail", ["messages", "messageCompose"]),
        ...mapGetters("mail", ["MY_DRAFTS", "MY_OUTBOX", "MY_SENT", "MY_MAILBOX_KEY"]),
        message() {
            return this.messages[this.messageKey];
        },
        panelTitle() {
            return this.message.subject ? this.message.subject : this.$t("mail.main.new");
        }
    },
    watch: {
        messageKey: {
            handler: async function () {
                this.cleanComposer();
                await this.initEditorContent();
                if (this.message.to.length > 0) {
                    this.setCursorInEditor();
                } else {
                    this.$refs.recipients.focus();
                }
            },
            immediate: true
        },
        "messageCompose.editorContent"() {
            if (!this.lockUpdateHtmlComposer) {
                this.updateHtmlComposer();
            }
            this.lockUpdateHtmlComposer = false;
        }
    },
    created: function () {
        this.deleteAllSelectedMessages();
    },
    destroyed: function () {
        if (isEmpty(this.message, this.messageCompose.editorContent)) {
            this.debouncedSave.cancel();
            this.purge(this.messageKey);
        }
        this.cleanComposer();
    },
    methods: {
        ...mapActions("mail", { save: actionTypes.SAVE_MESSAGE }),
        ...mapActions("mail", [actionTypes.SEND_MESSAGE, actionTypes.ADD_ATTACHMENTS]),
        ...mapActions("mail-webapp", ["purge"]),
        ...mapMutations("mail-webapp", ["deleteAllSelectedMessages"]),
        ...mapMutations("mail", [
            mutationTypes.SET_DRAFT_EDITOR_CONTENT,
            mutationTypes.SET_DRAFT_COLLAPSED_CONTENT,
            mutationTypes.SET_MESSAGE_SUBJECT
        ]),
        updateSubject(subject) {
            this.SET_MESSAGE_SUBJECT({ messageKey: this.messageKey, subject });
            this.saveDraft();
        },
        async initEditorContent() {
            let newContent = await this.computeContent();
            this.SET_DRAFT_EDITOR_CONTENT(newContent);
            this.updateHtmlComposer();
        },
        async computeContent() {
            let newContent;
            if (this.userPrefTextOnly) {
                const textContent = this.message.partContentByMimeType[MimeType.TEXT_PLAIN];
                newContent =
                    textContent || textContent === ""
                        ? textContent
                        : await PlayWithInlinePartsByCapabilities.getTextFromStructure(this.message);
            } else {
                const htmlContent = this.message.partContentByMimeType[MimeType.TEXT_HTML];
                if (htmlContent || htmlContent === "") {
                    newContent = htmlContent;
                } else {
                    const result = await PlayWithInlinePartsByCapabilities.getHtmlFromStructure(this.message);
                    newContent = await this.handleInlineImages(result.html, result.inlineImageParts);
                }

                newContent = sanitizeHtml(newContent);
                newContent = this.handleSeparator(newContent);
            }
            return newContent;
        },
        handleSeparator(content) {
            const doc = new DOMParser().parseFromString(content, "text/html");
            const separator =
                doc.querySelector("div[" + MessageReplyAttributeSeparator + "]") ||
                doc.querySelector("div[" + MessageForwardAttributeSeparator + "]");

            if (separator) {
                this.isReplyOrForward = true;
                this.SET_DRAFT_COLLAPSED_CONTENT(separator.outerHTML);
                separator.remove();
                return doc.body.innerHTML;
            }
            return content;
        },
        async handleInlineImages(html, inlineImageParts) {
            const partsWithCid = inlineImageParts.filter(part => part.contentId);
            const fakeHtmlPartAddress = "dontcare";
            const partContentsByAddress = {
                [fakeHtmlPartAddress]: html
            };
            const service = inject("MailboxItemsPersistence", this.message.folderRef.uid);
            await Promise.all(
                partsWithCid.map(async part => {
                    const content = await fetch(this.message.remoteRef.imapUid, service, part, false);
                    partContentsByAddress[part.address] = content;
                    return Promise.resolve();
                })
            );

            this.blobsUrl = InlineImageHelper.insertInlineImages(
                [{ address: fakeHtmlPartAddress }],
                partsWithCid,
                partContentsByAddress
            ).blobsUrl;
            return partContentsByAddress[fakeHtmlPartAddress];
        },
        cleanComposer() {
            this.SET_DRAFT_EDITOR_CONTENT("");
            this.SET_DRAFT_COLLAPSED_CONTENT(null);
            this.blobsUrl.forEach(url => URL.revokeObjectURL(url));
        },
        async expandContent() {
            this.SET_DRAFT_EDITOR_CONTENT(this.messageCompose.editorContent + this.messageCompose.collapsedContent);
            this.SET_DRAFT_COLLAPSED_CONTENT(null);
            this.setCursorInEditor();
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
            this.lockUpdateHtmlComposer = true;
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
            this.debouncedSave.cancel();
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
        saveDraft() {
            this.debouncedSave.cancel();
            this.debouncedSave();
        },
        async deleteDraft() {
            this.debouncedSave.cancel();
            if (isEmpty(this.message, this.messageCompose.editorContent)) {
                this.purge(this.messageKey);
                this.$router.navigate("v:mail:home");
                return;
            }
            const confirm = await this.$bvModal.msgBoxConfirm(this.$t("mail.draft.delete.confirm.content"), {
                title: this.$t("mail.draft.delete.confirm.title"),
                okTitle: this.$t("common.delete"),
                cancelVariant: "outline-secondary",
                cancelTitle: this.$t("common.cancel"),
                centered: true,
                hideHeaderClose: false
            });
            if (confirm) {
                // delete the draft then close the composer
                this.purge(this.messageKey);
                this.$router.navigate("v:mail:home");
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-composer {
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
