<template>
    <bm-form class="mail-composer m-lg-3 flex-grow-1 d-flex flex-column bg-white">
        <h3 class="d-none d-lg-flex text-nowrap text-truncate card-header px-2 py-1">
            {{ panelTitle }}
        </h3>
        <mail-composer-recipients
            ref="recipients"
            class="pl-3"
            :message="message"
            :is-reply-or-forward="!!messageCompose.collapsedContent"
            @save-draft="save"
        />
        <bm-form-input
            :value="message.subject.trim()"
            class="mail-composer-subject pl-3 d-flex align-items-center"
            :placeholder="$t('mail.new.subject.placeholder')"
            :aria-label="$t('mail.new.subject.aria')"
            type="text"
            @input="updateSubject"
            @keydown.enter.native.prevent
        />
        <hr class="mail-composer-splitter m-0" />
        <bm-file-drop-zone
            class="z-index-110 attachments mb-2"
            file-type-regex="image/(jpeg|jpg|png|gif)"
            @files-count="draggedFilesCount = $event"
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
        <mail-composer-content :user-pref-is-menu-bar-opened="userPrefIsMenuBarOpened" :message-key="messageKey" />
        <mail-composer-footer
            :message-key="messageKey"
            :user-pref-is-menu-bar-opened="userPrefIsMenuBarOpened"
            :signature="signature"
            :is-signature-inserted="isSignatureInserted"
            @toggle-text-format="userPrefIsMenuBarOpened = !userPrefIsMenuBarOpened"
            @toggle-signature="toggleSignature"
        />
    </bm-form>
</template>

<script>
import { mapMutations, mapState } from "vuex";

import { BmFormInput, BmForm, BmIcon, BmFileDropZone } from "@bluemind/styleguide";

import ComposerActionsMixin from "../ComposerActionsMixin";
import MailAttachmentsBlock from "../MailAttachment/MailAttachmentsBlock";
import MailComposerContent from "./MailComposerContent";
import MailComposerRecipients from "./MailComposerRecipients";
import MailComposerFooter from "./MailComposerFooter";

import { addSignature, isHtmlSignaturePresent, isTextSignaturePresent, removeSignature } from "../../model/signature";
import { SET_DRAFT_EDITOR_CONTENT, SET_MESSAGE_SUBJECT, UNSELECT_ALL_MESSAGES } from "~mutations";

export default {
    name: "MailComposer",
    components: {
        BmFileDropZone,
        BmFormInput,
        BmForm,
        BmIcon,
        MailComposerFooter,
        MailComposerContent,
        MailComposerRecipients,
        MailAttachmentsBlock
    },
    mixins: [ComposerActionsMixin],
    props: {
        messageKey: {
            type: String,
            required: true
        }
    },
    data() {
        return {
            userPrefIsMenuBarOpened: false, // TODO: initialize this with user setting
            draggedFilesCount: -1
        };
    },
    computed: {
        ...mapState("mail", ["messages", "messageCompose"]),
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
        }
    },
    created() {
        this.UNSELECT_ALL_MESSAGES();
    },
    methods: {
        ...mapMutations("mail", {
            SET_DRAFT_EDITOR_CONTENT,
            SET_MESSAGE_SUBJECT,
            UNSELECT_ALL_MESSAGES
        }),
        updateSubject(subject) {
            this.SET_MESSAGE_SUBJECT({ messageKey: this.messageKey, subject });
            this.save();
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
}
</style>
