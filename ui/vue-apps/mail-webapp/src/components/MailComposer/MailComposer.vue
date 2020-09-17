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
                    <mail-composer-recipients :message="message" @save-draft="saveDraft" />
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
                            <mail-attachments-block
                                v-if="message.attachments > 0"
                                :attachments="message.attachments"
                                editable
                                expanded
                            />
                        </bm-file-drop-zone>
                    </bm-col>
                </bm-row>
                <bm-file-drop-zone
                    class="z-index-110 as-attachments"
                    file-type-regex="^(?!.*image/(jpeg|jpg|png|gif)).*$"
                    at-least-one-match
                    @dropFiles="addAttachments($event)"
                    @filesCount="draggedFilesCount = $event"
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
                                v-if="collapsedContent"
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
                    v-if="userPrefTextOnly && collapsedContent"
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
                    @toggleTextFormat="userPrefIsMenuBarOpened = !userPrefIsMenuBarOpened"
                    @delete="deleteDraft"
                    @send="send"
                />
            </template>
        </mail-composer-panel>
    </bm-form>
</template>

<script>
import { mapGetters, mapActions, mapMutations, mapState } from "vuex";
import debounce from "lodash/debounce";

import { MimeType, PartsHelper } from "@bluemind/email";
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
                        editorContent: this.messageCompose.editorContent
                    }),
                1500
            ),
            userPrefIsMenuBarOpened: false, // TODO: initialize this with user setting
            userPrefTextOnly: false, // TODO: initialize this with user setting
            collapsedContent: null, // FIXME: init if a separator is detected in content
            draggedFilesCount: -1
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
                await this.initEditorContent();
                if (this.message.to.length > 0) {
                    this.$refs["message-content"].focus();
                } else {
                    this.$refs.to.focus();
                }
            },
            immediate: true
        }
    },
    created: function () {
        this.deleteAllSelectedMessages();
    },
    methods: {
        ...mapActions("mail", { save: actionTypes.SAVE_MESSAGE }),
        ...mapActions("mail", [actionTypes.SEND_MESSAGE]),
        ...mapActions("mail-webapp", ["addAttachments", "purge"]),
        ...mapMutations("mail-webapp", ["deleteAllSelectedMessages"]),
        ...mapMutations("mail", [
            mutationTypes.SET_DRAFT_EDITOR_CONTENT,
            mutationTypes.SET_MESSAGE_RECIPIENTS,
            mutationTypes.SET_MESSAGE_SUBJECT
        ]),
        updateSubject(subject) {
            this.SET_MESSAGE_SUBJECT({ messageKey: this.messageKey, subject });
            this.saveDraft();
        },
        async initEditorContent() {
            /**
             * FIXME (EN FAIRE UN TICKET)
             * Actually composer is very strict because it accepts only structure generated by himself : classic alternative case with text plain or html (inlined images are supported).
             * But it would be better if it supports draft coming from other clients. So we need to support a lot of cases :
             *      - if message got just a text plain part
             *      - if message contains only a html part and if userPrefTextOnly == true then we must convert HTML part into text part
             *      - if message contains only a plain text part and userPrefTextOnly == false then we must convert plain text part into HTML
             *      - all other complex cases are not supported yet.. (for example mixed part with an html followed by an image, followed by a text plain, etc)
             */

            let newContent;
            if (this.userPrefTextOnly) {
                const textPlainPart = this.message.inlinePartsByCapabilities.find(
                    part => part.capabilities === MimeType.TEXT_PLAIN
                ).parts[0];
                newContent = await fetchPart(textPlainPart, this.message);
            } else {
                let parts = this.message.inlinePartsByCapabilities.find(
                    part => part.capabilities[0] === MimeType.TEXT_HTML
                ).parts;
                const partsContent = await Promise.all(parts.map(part => fetchPart(part, this.message)));
                parts = parts.map((part, index) => ({ ...part, content: partsContent[index] }));
                const htmlPart = parts.find(part => part.mime === MimeType.TEXT_HTML);
                PartsHelper.insertInlineImages(
                    [htmlPart],
                    parts.filter(part => MimeType.isImage(part) && part.contentId)
                );
                newContent = htmlPart.content;
            }
            this.SET_DRAFT_EDITOR_CONTENT(newContent);
            this.setCursorInEditor();
        },
        async expandContent() {
            this.SET_DRAFT_EDITOR_CONTENT(this.messageCompose.editorContent + this.collapsedContent);
            this.collapsedContent = null;
            this.setCursorInEditor();
        },
        async setCursorInEditor() {
            await this.$nextTick();
            if (this.userPrefTextOnly) {
                this.$refs["message-content"].focus();
                this.$refs["message-content"].setSelectionRange(0, 0);
            } else {
                this.$refs["message-content"].updateContent();
                this.$refs["message-content"].focus("start");
            }
        },
        async updateEditorContent(newContent) {
            this.SET_DRAFT_EDITOR_CONTENT(newContent);
            this.saveDraft();
        },
        send() {
            this.debouncedSave.cancel();
            this.SEND_MESSAGE({
                userPrefTextOnly: this.userPrefTextOnly,
                draftKey: this.messageKey,
                myMailboxKey: this.MY_MAILBOX_KEY,
                outboxId: this.MY_OUTBOX.id,
                myDraftsFolder: this.MY_DRAFTS,
                sentFolder: this.MY_SENT,
                editorContent: this.messageCompose.editorContent
            });
            this.$router.navigate("v:mail:home");
        },
        saveDraft() {
            this.debouncedSave.cancel();
            this.debouncedSave();
        },
        async deleteDraft() {
            this.debouncedSave.cancel();
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

// FIXME ? move it in message model file, will also be needed by MailViewer
async function fetchPart(part, message) {
    const stream = await inject("MailboxItemsPersistence", message.folderRef.uid).fetch(
        message.remoteRef.imapUid,
        part.address,
        part.encoding,
        part.mime,
        part.charset
    );
    if (MimeType.isText(part) || MimeType.isHtml(part) || MimeType.isCalendar(part)) {
        return new Promise(resolve => {
            const reader = new FileReader();
            reader.readAsText(stream, part.encoding);
            reader.addEventListener("loadend", e => {
                resolve(e.target.result);
            });
        });
    }
    return stream;
}
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
