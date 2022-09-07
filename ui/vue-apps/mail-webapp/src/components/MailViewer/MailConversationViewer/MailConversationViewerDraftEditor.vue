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
                    class="mb-3"
                    label-class="font-weight-bold text-neutral"
                    :message="message"
                    @update="identity => setFrom(identity, message)"
                    @check-and-repair="checkAndRepairFrom"
                />
                <div class="to-contact-input">
                    <bm-contact-input
                        ref="to"
                        variant="underline"
                        :contacts="message.to"
                        :autocomplete-results="autocompleteResultsTo"
                        :validate-address-fn="validateAddress"
                        @search="searchedPattern => onSearch('to', searchedPattern)"
                        @update:contacts="updateTo"
                    >
                        {{ $t("common.to") }}
                    </bm-contact-input>
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
                        <bm-contact-input
                            variant="underline"
                            :contacts="message.cc"
                            :autocomplete-results="autocompleteResultsCc"
                            :validate-address-fn="validateAddress"
                            @search="searchedPattern => onSearch('cc', searchedPattern)"
                            @update:contacts="updateCc"
                        >
                            {{ $t("common.cc") }}
                        </bm-contact-input>
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
                    <bm-contact-input
                        variant="underline"
                        class="flex-fill"
                        :contacts="message.bcc"
                        :autocomplete-results="autocompleteResultsBcc"
                        :validate-address-fn="validateAddress"
                        @search="searchedPattern => onSearch('bcc', searchedPattern)"
                        @update:contacts="updateBcc"
                    >
                        {{ $t("common.bcc") }}
                    </bm-contact-input>
                </div>
            </template>
            <div class="d-flex conversation-viewer-row flex-nowrap">
                <mail-conversation-viewer-vertical-line :index="index" :max-index="maxIndex" after-avatar />
                <div class="flex-fill">
                    <mail-composer-attachments
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
            <div class="flex-fill">
                <mail-composer-content
                    ref="content"
                    :message="message"
                    :is-signature-inserted.sync="isSignatureInserted"
                />
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
import { BmButton, BmContactInput, BmIconButton } from "@bluemind/styleguide";
import { messageUtils } from "@bluemind/mail";
import { ComposerActionsMixin, ComposerInitMixin, ComposerMixin, EditRecipientsMixin } from "~/mixins";
import { AddAttachmentsCommand } from "~/commands";
import MailComposerAttachments from "../../MailComposer/MailComposerAttachments";
import MailComposerContent from "../../MailComposer/MailComposerContent";
import MailComposerFooter from "../../MailComposer/MailComposerFooter";
import MailComposerSender from "../../MailComposer/MailComposerSender";
import MailConversationViewerItem from "./MailConversationViewerItem";
import MailConversationViewerItemMixin from "./MailConversationViewerItemMixin";
import MailConversationViewerVerticalLine from "./MailConversationViewerVerticalLine";
import { REMOVE_MESSAGES, SET_MESSAGE_COMPOSING } from "~/mutations";
import MessagePathParam from "~/router/MessagePathParam";
import MailOpenInPopupWithShift from "../../MailOpenInPopupWithShift";

const { MessageStatus } = messageUtils;

export default {
    name: "MailConversationViewerDraftEditor",
    components: {
        BmButton,
        BmContactInput,
        BmIconButton,
        MailComposerAttachments,
        MailComposerContent,
        MailComposerFooter,
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
        consult() {
            this.$store.commit(`mail/${SET_MESSAGE_COMPOSING}`, { messageKey: this.message.key, composing: false });
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-conversation-viewer-draft-editor {
    .bm-contact-input-label {
        flex: none;
    }

    .to-contact-input {
        $expand-button-width: $icon-btn-width-compact;

        flex: 1;
        min-width: 0;
        position: relative;

        .bm-contact-input {
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

        .bm-contact-input {
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
}
</style>
