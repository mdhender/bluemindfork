<template>
    <div
        class="mail-composer-footer-toolbar justify-content-between align-items-center d-lg-flex"
        :class="{ 'd-none': !isPopup, 'd-flex': isPopup }"
    >
        <div class="main-buttons">
            <bm-button
                v-if="isDraft"
                type="submit"
                variant="fill-accent"
                icon="send"
                :disabled="disableSend"
                @click.prevent="send"
            >
                {{ $t("common.send") }}
            </bm-button>
            <bm-button
                v-else
                type="submit"
                variant="fill-accent"
                :title="$t('mail.actions.end_template_edition.aria')"
                @click.prevent="endEdition"
            >
                {{ $t("mail.actions.end_template_edition.label") }}
            </bm-button>
            <bm-button
                variant="text"
                class="ml-6"
                icon="trash"
                :disabled="isSaving || isSending"
                @click.prevent="deleteDraft"
            >
                {{ $t("common.delete") }}
            </bm-button>
        </div>
        <div class="d-flex align-items-center toolbar">
            <div
                v-if="errorOccuredOnSave"
                class="save-message save-message-error"
                :title="$t('mail.compose.save.error_reason')"
            >
                <bm-icon icon="exclamation-circle" />{{ saveMessage }}
            </div>
            <div v-else class="save-message">{{ saveMessage }}</div>
            <bm-extension id="webapp.mail" path="composer.footer.toolbar" :message="message" />
            <bm-icon-button
                v-if="!userPrefTextOnly"
                variant="compact"
                size="lg"
                icon="text-format"
                :aria-label="textFormatterLabel"
                :title="textFormatterLabel"
                :disabled="isSending"
                @click="toggleTextFormattingToolbar"
            />
            <input
                ref="attachInputRef"
                tabindex="-1"
                aria-hidden="true"
                type="file"
                multiple
                hidden
                @change="$execute('add-attachments', { files: $event.target.files, message, maxSize })"
                @click.stop="closeFilePicker()"
            />
            <bm-icon-button
                variant="compact"
                size="lg"
                icon="paper-clip"
                :aria-label="$tc('mail.actions.attach.aria')"
                :title="$tc('mail.actions.attach.aria')"
                :disabled="isSending"
                @click="openFilePicker()"
            />
            <bm-icon-dropdown ref="3dots-dropdown" dropup right no-caret variant="compact" size="lg" icon="3dots-v">
                <bm-dropdown-item-toggle
                    :disabled="hasCorporateSignature"
                    :checked="isSignatureInserted"
                    @click="$emit('toggle-signature')"
                >
                    {{ $t("mail.compose.toolbar.insert_signature") }}
                </bm-dropdown-item-toggle>
                <bm-dropdown-item-button icon="documents" @click="openTemplateChooser">
                    {{ $t("mail.compose.toolbar.use_template") }}
                </bm-dropdown-item-button>
                <bm-dropdown-item :disabled="isSenderShown" @click="showSender">
                    {{ $t("mail.actions.show_sender") }}
                </bm-dropdown-item>
            </bm-icon-dropdown>
        </div>
    </div>
</template>

<script>
import { ContactValidator } from "@bluemind/contact";
import { BmExtension } from "@bluemind/extensions.vue";
import {
    BmButton,
    BmIcon,
    BmIconButton,
    BmIconDropdown,
    BmDropdownItem,
    BmDropdownItemButton,
    BmDropdownItemToggle
} from "@bluemind/styleguide";
import { draftUtils, messageUtils } from "@bluemind/mail";

import { ComposerActionsMixin, FormattedDateMixin } from "~/mixins";
import { AddAttachmentsCommand } from "~/commands";
import {
    SET_SHOW_FORMATTING_TOOLBAR,
    SET_TEMPLATE_CHOOSER_TARGET,
    SET_TEMPLATE_CHOOSER_VISIBLE,
    SHOW_SENDER
} from "~/mutations";
import { IS_SENDER_SHOWN } from "~/getters";
import { mapGetters, mapMutations, mapState } from "vuex";

const { MessageStatus } = messageUtils;
const { isNewMessage } = draftUtils;

export default {
    name: "MailComposerToolbar",
    components: {
        BmButton,
        BmIconButton,
        BmIconDropdown,
        BmDropdownItem,
        BmDropdownItemButton,
        BmDropdownItemToggle,
        BmExtension,
        BmIcon
    },
    mixins: [AddAttachmentsCommand, ComposerActionsMixin, FormattedDateMixin],
    props: {
        message: {
            type: Object,
            required: true
        },
        isSignatureInserted: {
            type: Boolean,
            required: true
        }
    },
    computed: {
        ...mapGetters("mail", { IS_SENDER_SHOWN }),
        ...mapState("mail", ["isPopup"]),
        userSettings() {
            return this.$store.state.settings;
        },
        hasCorporateSignature() {
            return this.$store.state.mail.messageCompose.corporateSignature !== null;
        },
        hasRecipient() {
            return this.message.to.length > 0 || this.message.cc.length > 0 || this.message.bcc.length > 0;
        },
        isSending() {
            return this.message.status === MessageStatus.SENDING;
        },
        isSaving() {
            return this.message.status === MessageStatus.SAVING;
        },
        errorOccuredOnSave() {
            return this.message.status === MessageStatus.SAVE_ERROR;
        },
        saveMessage() {
            const kind = this.isDraft ? "draft" : "template";
            if (this.isSaving) {
                return this.$t(`mail.compose.save.${kind}.inprogress`);
            } else if (this.errorOccuredOnSave) {
                return this.$t(`mail.compose.save.${kind}.error`);
            } else if (isNewMessage(this.message)) {
                return "";
            } else {
                return this.saveMessageWithDate;
            }
        },
        saveMessageWithDate() {
            const kind = this.isDraft ? "draft" : "template";
            const formatted = this.formatMessageDate(this.message);
            return formatted.time
                ? this.$t(`mail.compose.save.${kind}.date_time`, formatted)
                : this.$t(`mail.compose.save.${kind}.date`, formatted);
        },
        showFormattingToolbar() {
            return this.$store.state.mail.messageCompose.showFormattingToolbar;
        },
        textFormatterLabel() {
            return this.showFormattingToolbar
                ? this.$tc("mail.actions.textformat.hide.aria")
                : this.$tc("mail.actions.textformat.show.aria");
        },
        anyRecipientInError() {
            return this.message.to
                .concat(this.message.cc)
                .concat(this.message.bcc)
                .some(contact => !ContactValidator.validateContact(contact));
        },

        disableSend() {
            return (
                this.errorOccuredOnSave ||
                this.isSending ||
                !this.hasRecipient ||
                this.anyRecipientInError ||
                this.anyAttachmentInError
            );
        },
        isSenderShown() {
            return this.IS_SENDER_SHOWN(this.userSettings);
        }
    },
    methods: {
        ...mapMutations("mail", {
            SET_SHOW_FORMATTING_TOOLBAR,
            SET_TEMPLATE_CHOOSER_TARGET,
            SET_TEMPLATE_CHOOSER_VISIBLE,
            SHOW_SENDER
        }),
        closeFilePicker() {
            this.$refs.attachInputRef.value = "";
        },
        openFilePicker() {
            this.$refs.attachInputRef.click();
        },
        openTemplateChooser() {
            this.SET_TEMPLATE_CHOOSER_VISIBLE(true);
            this.SET_TEMPLATE_CHOOSER_TARGET(this.message.key);
        },
        showSender() {
            this.SHOW_SENDER(true);
            this.$refs["3dots-dropdown"].hide(false);
        },
        toggleTextFormattingToolbar() {
            this.SET_SHOW_FORMATTING_TOOLBAR(!this.showFormattingToolbar);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_type";
@import "~@bluemind/styleguide/css/_variables";

.mail-composer-footer-toolbar {
    flex-wrap: wrap;
    gap: $sp-5;
    padding: $sp-5 + $sp-2 $sp-4;

    border-top: 1px solid $neutral-fg-lo3;

    .main-buttons {
        flex: none;
    }
    .toolbar {
        flex: 1;
        justify-content: flex-end;
    }

    .save-message {
        @extend %caption;
        margin-right: $sp-5;
        color: $neutral-fg-lo1;
        &.save-message-error {
            color: $danger-fg;
            display: flex;
            gap: $sp-3;
            align-items: center;
        }
    }
}
</style>
