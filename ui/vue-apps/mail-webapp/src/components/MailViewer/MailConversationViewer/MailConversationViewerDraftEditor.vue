<template>
    <mail-conversation-viewer-item
        class="mail-conversation-viewer-draft-editor draft"
        v-bind="$props"
        :is-draft="true"
        v-on="$listeners"
    >
        <template slot="head">
            <div class="col pl-3 align-self-center">
                <bm-contact-input
                    ref="to"
                    :contacts="message.to"
                    :autocomplete-results="autocompleteResultsTo"
                    :validate-address-fn="validateAddress"
                    @search="searchedPattern => onSearch('to', searchedPattern)"
                    @update:contacts="updateTo"
                >
                    <span class="font-weight-bold text-secondary">{{ $t("common.to") }}</span>
                </bm-contact-input>
            </div>
            <div>
                <template v-if="!(displayedRecipientFields & recipientModes.CC)">
                    <bm-button variant="simple-secondary" @click="displayedRecipientFields |= recipientModes.CC">
                        {{ $t("common.cc") }}
                    </bm-button>
                    <bm-button variant="simple-secondary" @click="displayedRecipientFields |= recipientModes.BCC">
                        {{ $t("common.bcc") }}
                    </bm-button>
                </template>
                <bm-button
                    variant="simple-secondary"
                    :aria-label="$t('mail.actions.extend')"
                    :title="$t('mail.actions.extend')"
                    @click="openExtendedEditing"
                >
                    <bm-icon icon="extend" size="lg" />
                    <span class="d-lg-none">{{ $t("mail.actions.extend") }}</span>
                </bm-button>
            </div>
        </template>

        <template slot="subhead">
            <mail-conversation-viewer-field-sep :index="index" :max-index="maxIndex" />
            <template v-if="displayedRecipientFields & recipientModes.CC">
                <div class="row pl-5">
                    <mail-conversation-viewer-vertical-line :index="index" :max-index="maxIndex" after-avatar />
                    <bm-contact-input
                        class="col pl-3"
                        :contacts="message.cc"
                        :autocomplete-results="autocompleteResultsCc"
                        :validate-address-fn="validateAddress"
                        @search="searchedPattern => onSearch('cc', searchedPattern)"
                        @update:contacts="updateCc"
                    >
                        <span class="font-weight-bold text-secondary">{{ $t("common.cc") }}</span>
                    </bm-contact-input>
                    <bm-button
                        v-if="!(displayedRecipientFields & recipientModes.BCC)"
                        variant="simple-dark"
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
                        class="col pl-3"
                        :contacts="message.bcc"
                        :autocomplete-results="autocompleteResultsBcc"
                        :validate-address-fn="validateAddress"
                        @search="searchedPattern => onSearch('bcc', searchedPattern)"
                        @update:contacts="updateBcc"
                    >
                        <span class="font-weight-bold text-secondary">{{ $t("common.bcc") }}</span>
                    </bm-contact-input>
                </div>
                <mail-conversation-viewer-field-sep :index="index" :max-index="maxIndex" />
            </template>
            <div class="row pl-5">
                <mail-conversation-viewer-vertical-line :index="index" :max-index="maxIndex" after-avatar />
                <mail-composer-attachments
                    class="col pl-3"
                    :dragged-files-count="draggedFilesCount"
                    :message="message"
                    @files-count="draggedFilesCount = $event"
                    @drop-files="addAttachments($event)"
                />
            </div>
        </template>

        <template slot="content">
            <div class="col pl-3 flex-grow-1">
                <mail-composer-content
                    ref="content"
                    :user-pref-is-menu-bar-opened="userPrefIsMenuBarOpened"
                    :message="message"
                />
            </div>
        </template>
        <template slot="bottom">
            <div class="col pl-3">
                <div class="row">
                    <div class="col py-0"><hr /></div>
                </div>
                <div class="row">
                    <mail-composer-footer
                        class="col"
                        :message="message"
                        :message-key="message.key"
                        :signature="signature"
                        :is-signature-inserted="isSignatureInserted"
                        @toggle-text-format="userPrefIsMenuBarOpened = !userPrefIsMenuBarOpened"
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
import { ComposerActionsMixin, ComposerInitMixin, ComposerMixin, EditRecipientsMixin } from "~/mixins";
import MailComposerAttachments from "../../MailComposer/MailComposerAttachments";
import MailComposerContent from "../../MailComposer/MailComposerContent";
import MailComposerFooter from "../../MailComposer/MailComposerFooter";
import MailConversationViewerItem from "./MailConversationViewerItem";
import MailConversationViewerItemMixin from "./MailConversationViewerItemMixin";
import MailConversationViewerFieldSep from "./MailConversationViewerFieldSep";
import MailConversationViewerVerticalLine from "./MailConversationViewerVerticalLine";
import { REMOVE_MESSAGES } from "~/mutations";
import { MessageStatus } from "~/model/message";

export default {
    name: "MailConversationViewerDraftEditor",
    components: {
        BmButton,
        BmContactInput,
        BmIcon,
        MailComposerAttachments,
        MailComposerContent,
        MailComposerFooter,
        MailConversationViewerItem,
        MailConversationViewerFieldSep,
        MailConversationViewerVerticalLine
    },
    mixins: [
        ComposerActionsMixin,
        ComposerInitMixin,
        ComposerMixin,
        EditRecipientsMixin,
        MailConversationViewerItemMixin
    ],
    computed: {
        ...mapState("mail", ["folders"])
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
            this.$router.navigate({
                name: "v:mail:message",
                params: { message: this.message, folder: this.folders[this.conversation.folderRef.key].path }
            });
        }
    }
};
</script>

<style lang="scss">
.mail-conversation-viewer-draft-editor {
    .bm-contact-input-label {
        padding-left: unset !important;
    }
    .mail-composer-footer {
        border-top: unset !important;
    }
}
</style>
