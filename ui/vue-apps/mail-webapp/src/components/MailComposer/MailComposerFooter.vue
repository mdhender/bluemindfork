<template>
    <div class="mail-composer-footer p-2 border-top justify-content-between align-items-center">
        <div>
            <bm-button v-if="isDraft" type="submit" variant="primary" :disabled="disableSend" @click.prevent="send">
                {{ $t("common.send") }}
            </bm-button>
            <bm-button
                v-else
                type="submit"
                variant="primary"
                :title="$t('mail.actions.end_template_edition.aria')"
                @click.prevent="endEdition"
            >
                {{ $t("mail.actions.end_template_edition.label") }}
            </bm-button>
            <bm-button
                variant="simple-dark"
                class="ml-2"
                :disabled="isSaving || isSending"
                @click.prevent="deleteDraft"
            >
                {{ $t("common.delete") }}
            </bm-button>
        </div>
        <div class="d-flex align-items-center toolbar">
            <span v-if="errorOccuredOnSave" class="pr-2 text-danger" :title="$t('mail.compose.save.error_reason')">
                <!-- trick: modify the viewBox attribute to have a correct vertical alignment -->
                <!-- eslint-disable-next-line vue/attribute-hyphenation -->
                <bm-icon icon="exclamation-circle" class="mr-1" viewBox="0 -1 12 12" />{{ saveMessage }}
            </span>
            <span v-else class="text-muted pr-2">{{ saveMessage }}</span>
            <bm-extension id="webapp.mail" path="composer.footer.toolbar" :message="message" />
            <bm-button
                v-if="!userPrefTextOnly"
                variant="simple-dark"
                :aria-label="textFormatterLabel"
                :title="textFormatterLabel"
                :disabled="isSending"
                @click="$emit('toggle-text-format')"
            >
                <bm-icon icon="text-format" size="lg" />
            </bm-button>
            <input
                ref="attachInputRef"
                tabindex="-1"
                aria-hidden="true"
                type="file"
                multiple
                hidden
                @change="addAttachments($event.target.files)"
                @click.stop
            />
            <bm-button
                variant="simple-dark"
                :aria-label="$tc('mail.actions.attach.aria')"
                :title="$tc('mail.actions.attach.aria')"
                :disabled="isSending"
                @click="openFilePicker()"
            >
                <bm-icon icon="paper-clip" size="lg" />
            </bm-button>
            <bm-dropdown ref="3dots-dropdown" dropup right no-caret variant="simple-dark">
                <template #button-content>
                    <bm-icon icon="3dots-v" size="lg" />
                </template>
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
            </bm-dropdown>
        </div>
    </div>
</template>

<script>
import { EmailValidator } from "@bluemind/email";
import { BmExtension } from "@bluemind/extensions";
import {
    BmButton,
    BmIcon,
    BmDropdown,
    BmDropdownItem,
    BmDropdownItemButton,
    BmDropdownItemToggle
} from "@bluemind/styleguide";

import { ComposerActionsMixin, FormattedDateMixin } from "~/mixins";
import { MessageStatus } from "~/model/message";
import { isNewMessage } from "~/model/draft";
import { SET_TEMPLATE_CHOOSER_TARGET, SET_TEMPLATE_CHOOSER_VISIBLE, SHOW_SENDER } from "~/mutations";
import { IS_SENDER_SHOWN } from "~/getters";
import { mapGetters, mapMutations, mapState } from "vuex";

export default {
    name: "MailComposerFooter",
    components: {
        BmButton,
        BmDropdown,
        BmDropdownItem,
        BmDropdownItemButton,
        BmDropdownItemToggle,
        BmExtension,
        BmIcon
    },
    mixins: [ComposerActionsMixin, FormattedDateMixin],
    props: {
        userPrefIsMenuBarOpened: {
            type: Boolean,
            default: false
        },
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
        ...mapState("session", { userSettings: ({ settings }) => settings.remote }),
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
        textFormatterLabel() {
            return this.userPrefIsMenuBarOpened
                ? this.$tc("mail.actions.textformat.hide.aria")
                : this.$tc("mail.actions.textformat.show.aria");
        },
        anyRecipientInError() {
            return this.message.to
                .concat(this.message.cc)
                .concat(this.message.bcc)
                .some(contact => !EmailValidator.validateAddress(contact.address));
        },
        disableSend() {
            return this.errorOccuredOnSave || this.isSending || !this.hasRecipient || this.anyRecipientInError;
        },
        isSenderShown() {
            return this.IS_SENDER_SHOWN(this.userSettings);
        }
    },
    methods: {
        ...mapMutations("mail", { SET_TEMPLATE_CHOOSER_TARGET, SET_TEMPLATE_CHOOSER_VISIBLE, SHOW_SENDER }),
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
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-composer-footer {
    .toolbar .btn {
        padding: $sp-2;
    }

    display: none;

    @include media-breakpoint-up(lg) {
        display: flex;
    }
}
</style>
