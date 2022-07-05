<template>
    <mail-conversation-viewer-item
        class="mail-conversation-viewer-draft-editor draft"
        v-bind="$props"
        :is-draft="true"
        v-on="$listeners"
    >
        <template slot="head">
            <div class="col pl-3 align-self-center flex-fill">
                <mail-composer-sender
                    v-if="isSenderShown"
                    label-class="font-weight-bold text-neutral"
                    :message="message"
                    @update="identity => setFrom(identity, message)"
                    @check-and-repair="checkAndRepairFrom"
                />
                <bm-contact-input
                    ref="to"
                    :contacts="message.to"
                    :autocomplete-results="autocompleteResultsTo"
                    :validate-address-fn="validateAddress"
                    @search="searchedPattern => onSearch('to', searchedPattern)"
                    @update:contacts="updateTo"
                >
                    <span class="font-weight-bold text-neutral">{{ $t("common.to") }}</span>
                </bm-contact-input>
            </div>
            <div class="align-self-center">
                <template v-if="!(displayedRecipientFields & recipientModes.CC)">
                    <bm-button variant="simple-neutral" @click="displayedRecipientFields |= recipientModes.CC">
                        {{ $t("common.cc") }}
                    </bm-button>
                    <bm-button variant="simple-neutral" @click="displayedRecipientFields |= recipientModes.BCC">
                        {{ $t("common.bcc") }}
                    </bm-button>
                </template>
                <mail-open-in-popup-with-shift v-slot="action" :href="route">
                    <bm-button
                        variant="simple-neutral"
                        :title="action.label($t('mail.actions.extend'))"
                        :disabled="anyAttachmentInError"
                        @click="action.execute(openExtendedEditing)"
                    >
                        <bm-icon :icon="action.icon('extend')" size="lg" />
                        <span class="d-lg-none">{{ $t("mail.actions.extend") }}</span>
                    </bm-button>
                </mail-open-in-popup-with-shift>
            </div>
        </template>

        <template slot="subhead">
            <mail-conversation-viewer-field-sep :index="index" :max-index="maxIndex" />
            <template v-if="displayedRecipientFields & recipientModes.CC">
                <div class="row pl-5 flex-nowrap">
                    <mail-conversation-viewer-vertical-line :index="index" :max-index="maxIndex" after-avatar />
                    <bm-contact-input
                        class="col-11 pl-3 flex-fill"
                        :contacts="message.cc"
                        :autocomplete-results="autocompleteResultsCc"
                        :validate-address-fn="validateAddress"
                        @search="searchedPattern => onSearch('cc', searchedPattern)"
                        @update:contacts="updateCc"
                    >
                        <span class="font-weight-bold text-neutral">{{ $t("common.cc") }}</span>
                    </bm-contact-input>
                    <bm-button
                        v-if="!(displayedRecipientFields & recipientModes.BCC)"
                        variant="simple-neutral"
                        @click="displayedRecipientFields |= recipientModes.BCC"
                    >
                        {{ $t("common.bcc") }}
                    </bm-button>
                </div>
                <mail-conversation-viewer-field-sep :index="index" :max-index="maxIndex" />
            </template>
            <template v-if="displayedRecipientFields & recipientModes.BCC">
                <div class="row pl-5">
                    <mail-conversation-viewer-vertical-line :index="index" :max-index="maxIndex" after-avatar />
                    <bm-contact-input
                        class="col-11 pl-3 flex-fill"
                        :contacts="message.bcc"
                        :autocomplete-results="autocompleteResultsBcc"
                        :validate-address-fn="validateAddress"
                        @search="searchedPattern => onSearch('bcc', searchedPattern)"
                        @update:contacts="updateBcc"
                    >
                        <span class="font-weight-bold text-neutral">{{ $t("common.bcc") }}</span>
                    </bm-contact-input>
                </div>
                <mail-conversation-viewer-field-sep :index="index" :max-index="maxIndex" />
            </template>
            <div class="row pl-5">
                <mail-conversation-viewer-vertical-line :index="index" :max-index="maxIndex" after-avatar />
                <div class="col-11">
                    <mail-composer-attachments
                        :dragged-files-count="draggedFilesCount"
                        :message="message"
                        @files-count="draggedFilesCount = $event"
                        @drop-files="$execute('add-attachments', { files: $event, message, maxSize })"
                    />
                </div>
            </div>
        </template>

        <template slot="content">
            <div class="col-11 flex-grow-1">
                <mail-composer-content
                    ref="content"
                    :message="message"
                    :is-signature-inserted.sync="isSignatureInserted"
                />
            </div>
        </template>
        <template slot="bottom">
            <div class="col-11">
                <div class="row">
                    <div class="col py-0"><hr /></div>
                </div>
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
import { BmButton, BmContactInput, BmIcon } from "@bluemind/styleguide";
import { messageUtils } from "@bluemind/mail";
import { ComposerActionsMixin, ComposerInitMixin, ComposerMixin, EditRecipientsMixin } from "~/mixins";
import { AddAttachmentsCommand } from "~/commands";
import MailComposerAttachments from "../../MailComposer/MailComposerAttachments";
import MailComposerContent from "../../MailComposer/MailComposerContent";
import MailComposerFooter from "../../MailComposer/MailComposerFooter";
import MailComposerSender from "../../MailComposer/MailComposerSender";
import MailConversationViewerItem from "./MailConversationViewerItem";
import MailConversationViewerItemMixin from "./MailConversationViewerItemMixin";
import MailConversationViewerFieldSep from "./MailConversationViewerFieldSep";
import MailConversationViewerVerticalLine from "./MailConversationViewerVerticalLine";
import { REMOVE_MESSAGES } from "~/mutations";
import MessagePathParam from "~/router/MessagePathParam";
import MailOpenInPopupWithShift from "../../MailOpenInPopupWithShift";

const { MessageStatus } = messageUtils;

export default {
    name: "MailConversationViewerDraftEditor",
    components: {
        BmButton,
        BmContactInput,
        BmIcon,
        MailComposerAttachments,
        MailComposerContent,
        MailComposerFooter,
        MailComposerSender,
        MailConversationViewerItem,
        MailConversationViewerFieldSep,
        MailConversationViewerVerticalLine,
        MailOpenInPopupWithShift
    },
    mixins: [
        AddAttachmentsCommand,
        ComposerActionsMixin,
        ComposerInitMixin,
        ComposerMixin,
        EditRecipientsMixin,
        MailConversationViewerItemMixin
    ],
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
        async openExtendedEditing() {
            await this.saveAsap();
            this.$router.navigate(this.route);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-conversation-viewer-draft-editor {
    .bm-contact-input-label {
        padding-left: unset !important;
    }
    .mail-composer-footer {
        border-top: unset !important;
    }
    .bm-rich-editor .ProseMirror {
        padding: $sp-2 0 $sp-2 $sp-3;
    }
    .toolbar-menu.full-toolbar {
        border-top-color: $neutral-fg-lo3;
    }
}
</style>
