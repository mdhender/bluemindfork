<template>
    <mail-conversation-viewer-item
        class="mail-conversation-viewer-draft-editor draft"
        v-bind="$props"
        :is-draft="true"
        :sticky-bottom="true"
        v-on="$listeners"
    >
        <template slot="head">
            <div class="d-flex flex-column flex-fill">
                <mail-composer-sender
                    v-if="isSenderShown"
                    label-class="font-weight-bold text-neutral"
                    :message="message"
                    @update="identity => setFrom(identity, message)"
                    @check-and-repair="checkAndRepairFrom"
                />
                <div class="to-contact-input">
                    <mail-composer-recipient :message="message" recipient-type="to" />
                    <mail-open-in-popup-with-shift v-slot="action" :href="route" :next="consult">
                        <bm-icon-button
                            variant="compact"
                            class="expand-button"
                            :title="action.label($t('mail.actions.extend'))"
                            :disabled="anyAttachmentInError"
                            :icon="action.icon('extend')"
                            @click="saveAsap().then(() => action.execute(() => $router.navigate(route), $event))"
                        />
                    </mail-open-in-popup-with-shift>
                </div>
            </div>
        </template>

        <template slot="subhead">
            <template v-if="displayedRecipientFields > recipientModes.TO">
                <div class="d-flex conversation-viewer-row flex-nowrap">
                    <mail-conversation-viewer-vertical-line :index="index" :max-index="maxIndex" after-avatar />
                    <div class="cc-contact-input">
                        <mail-composer-recipient :message="message" recipient-type="cc" />
                        <bm-button
                            v-if="!(displayedRecipientFields & recipientModes.BCC)"
                            variant="text"
                            class="bcc-button text-nowrap"
                            @click="displayedRecipientFields |= recipientModes.BCC"
                        >
                            {{ $t("common.bcc") }}
                        </bm-button>
                    </div>
                </div>
            </template>
            <template v-if="displayedRecipientFields & recipientModes.BCC">
                <div class="d-flex conversation-viewer-row flex-nowrap">
                    <mail-conversation-viewer-vertical-line :index="index" :max-index="maxIndex" after-avatar />
                    <mail-composer-recipient :message="message" recipient-type="bcc" />
                </div>
            </template>
            <div class="d-flex conversation-viewer-row flex-nowrap">
                <mail-conversation-viewer-vertical-line :index="index" :max-index="maxIndex" after-avatar />
                <div class="w-100">
                    <bm-dropzone
                        v-show="showConversationDropzone"
                        class="my-2"
                        :accept="['conversation']"
                        :value="message"
                        @dropactivate="showConversationDropzone = true"
                        @dropdeactivate="showConversationDropzone = false"
                    >
                        <div
                            class="bm-dropzone-show-dropzone justify-content-center align-items-center d-flex flex-column"
                        >
                            <mail-composer-attach-zone :text="$tc('mail.new.attachments.eml.drop.zone')" />
                        </div>
                    </bm-dropzone>
                    <mail-composer-attachments
                        v-if="!showConversationDropzone"
                        class="my-4"
                        :dragged-files-count="draggedFilesCount"
                        :message="message"
                        @files-count="draggedFilesCount = $event"
                        @drop-files="$execute('add-attachments', { files: $event, message, maxSize })"
                    />
                </div>
            </div>
        </template>

        <template slot="content">
            <div v-show="!showConversationDropzone" class="w-100">
                <bm-file-drop-zone
                    class="my-2"
                    :should-activate-fn="shouldActivate"
                    @files-count="draggedFilesCount = $event"
                    @drop-files="$execute('add-attachments', { files: $event, message, maxSize })"
                >
                    <template #dropZone>
                        <mail-composer-attach-zone :text="$tc('mail.new.attachments.drop.zone', draggedFilesCount)" />
                    </template>
                    <mail-composer-content
                        ref="content"
                        :message="message"
                        :is-signature-inserted.sync="isSignatureInserted"
                    />
                </bm-file-drop-zone>
            </div>
        </template>
        <template slot="bottom">
            <div class="flex-fill">
                <div class="row">
                    <mail-composer-footer
                        class="col"
                        :message="message"
                        :is-signature-inserted="isSignatureInserted"
                        @toggle-signature="toggleSignature"
                    />
                </div>
            </div>
        </template>
    </mail-conversation-viewer-item>
</template>

<script>
import { mapMutations, mapState } from "vuex";
import { BmButton, BmDropzone, BmFileDropZone, BmIconButton } from "@bluemind/ui-components";
import { messageUtils } from "@bluemind/mail";
import {
    ComposerActionsMixin,
    ComposerInitMixin,
    ComposerMixin,
    EditRecipientsMixin,
    FileDropzoneMixin
} from "~/mixins";
import { AddAttachmentsCommand } from "~/commands";
import { REMOVE_MESSAGES, SET_MESSAGE_COMPOSING } from "~/mutations";
import MailComposerAttachments from "../../MailComposer/MailComposerAttachments";
import MailComposerAttachZone from "../../MailComposer/MailComposerAttachZone";
import MailComposerContent from "../../MailComposer/MailComposerContent";
import MailComposerFooter from "../../MailComposer/MailComposerFooter";
import MailComposerRecipient from "../../MailComposer/MailComposerRecipient";
import MailComposerSender from "../../MailComposer/MailComposerSender";
import MailConversationViewerItem from "./MailConversationViewerItem";
import MailConversationViewerItemMixin from "./MailConversationViewerItemMixin";
import MailConversationViewerVerticalLine from "./MailConversationViewerVerticalLine";
import MailOpenInPopupWithShift from "../../MailOpenInPopupWithShift";
import MessagePathParam from "~/router/MessagePathParam";

const { MessageStatus } = messageUtils;

export default {
    name: "MailConversationViewerDraftEditor",
    components: {
        BmButton,
        BmDropzone,
        BmFileDropZone,
        BmIconButton,
        MailComposerAttachments,
        MailComposerAttachZone,
        MailComposerContent,
        MailComposerFooter,
        MailComposerRecipient,
        MailComposerSender,
        MailConversationViewerItem,
        MailConversationViewerVerticalLine,
        MailOpenInPopupWithShift
    },
    mixins: [
        AddAttachmentsCommand,
        ComposerActionsMixin,
        ComposerInitMixin,
        ComposerMixin,
        EditRecipientsMixin,
        FileDropzoneMixin,
        MailConversationViewerItemMixin
    ],
    data() {
        return { showConversationDropzone: false };
    },
    computed: {
        ...mapState("mail", ["folders"]),
        route() {
            return this.$router.relative({
                name: "mail:message",
                params: { messagepath: MessagePathParam.build("", this.message) }
            });
        }
    },
    mounted() {
        this.$nextTick(() => this.$el.scrollIntoView());
    },
    destroyed() {
        // clean up unsaved new message from conversation
        if (this.message.status === MessageStatus.NEW) {
            this.REMOVE_MESSAGES({ messages: [this.message] });
        }
    },
    methods: {
        ...mapMutations("mail", { REMOVE_MESSAGES }),
        consult() {
            this.$store.commit(`mail/${SET_MESSAGE_COMPOSING}`, { messageKey: this.message.key, composing: false });
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

.mail-conversation-viewer-draft-editor {
    .bm-contact-input-label {
        flex: none;
    }

    .to-contact-input {
        $expand-button-width: $icon-btn-width-compact;

        flex: 1;
        min-width: 0;
        position: relative;

        .contact-input {
            padding-right: $expand-button-width;
        }

        .expand-button {
            position: absolute;
            right: 0;
            top: base-px-to-rem(2);
        }
    }

    .cc-contact-input {
        $bcc-button-width: base-px-to-rem(24);

        flex: 1;
        min-width: 0;
        position: relative;

        .contact-input {
            padding-right: $bcc-button-width + $sp-3;
        }

        .bcc-button {
            position: absolute;
            width: $bcc-button-width;
            right: $sp-3;
            bottom: base-px-to-rem(3);
        }
    }

    .mail-composer-content .bm-rich-editor {
        padding-left: 0;
        padding-right: 0;
    }

    .mail-composer-footer {
        border-top: unset !important;
    }
    .bm-rich-editor .ProseMirror {
        padding: $sp-2 0 $sp-2 $sp-3;
    }

    .contact-input {
        &.expanded-search .delete-autocomplete {
            visibility: hidden;
        }
        .suggestions {
            min-width: base-px-to-rem(288);
        }
    }
}
</style>
