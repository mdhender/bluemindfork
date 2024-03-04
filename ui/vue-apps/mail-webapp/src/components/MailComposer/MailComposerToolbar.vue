<template>
    <div
        class="mail-composer-toolbar justify-content-between align-items-center d-lg-flex flex-fill"
        :class="{ 'd-none': !isPopup, 'd-flex': isPopup }"
    >
        <div class="main-buttons">
            <bm-button
                v-if="isDraft"
                type="submit"
                variant="fill-accent"
                icon="send"
                :disabled="isSendingDisabled"
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
        <div class="d-flex align-items-center">
            <div
                v-if="errorOccuredOnSave"
                class="save-message save-message-error"
                :title="$t('mail.compose.save.error_reason')"
            >
                <bm-icon icon="exclamation-circle" />
                <div>{{ saveMessage }}</div>
            </div>
            <div v-else class="save-message">{{ saveMessage }}</div>
            <bm-toolbar menu-icon="3dots-v" :min-items="4" menu-icon-size="lg" menu-icon-variant="compact">
                <bm-toolbar-icon-button
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
                    @change="execAddAttachments({ files: $event.target.files, message })"
                    @click.stop="closeFilePicker()"
                />
                <bm-toolbar-icon-button
                    extension="composer.footer.toolbar"
                    extension-id="webapp.mail"
                    variant="compact"
                    size="lg"
                    icon="paper-clip"
                    :aria-label="$tc('mail.actions.attach.aria')"
                    :title="$tc('mail.actions.attach.aria')"
                    :disabled="isSending"
                    :message="message"
                    @click="openFilePicker()"
                />
                <template #menu>
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
                    <bm-dropdown-item-toggle
                        :checked="isDeliveryStatusRequested"
                        @click="$emit('toggle-delivery-status')"
                    >
                        {{ $t("mail.compose.toolbar.delivery_status") }}
                    </bm-dropdown-item-toggle>
                    <bm-dropdown-item-toggle
                        :checked="isDispositionNotificationRequested"
                        @click="$emit('toggle-disposition-notification')"
                    >
                        {{ $t("mail.compose.toolbar.disposition_notification") }}
                    </bm-dropdown-item-toggle>
                </template>
            </bm-toolbar>
        </div>
    </div>
</template>

<script>
import { BmExtension } from "@bluemind/extensions.vue";
import {
    BmToolbar,
    BmToolbarIconButton,
    BmButton,
    BmIcon,
    BmIconButton,
    BmIconDropdown,
    BmDropdownItem,
    BmDropdownItemButton,
    BmDropdownItemToggle
} from "@bluemind/ui-components";
import { draftUtils, messageUtils } from "@bluemind/mail";

import { ComposerActionsMixin, FormattedDateMixin } from "~/mixins";
import { useAddAttachmentsCommand } from "~/commands";
import {
    SET_SHOW_FORMATTING_TOOLBAR,
    SET_TEMPLATE_CHOOSER_TARGET,
    SET_TEMPLATE_CHOOSER_VISIBLE,
    SHOW_SENDER
} from "~/mutations";
import { IS_SENDER_SHOWN, SIGNATURE } from "~/getters";
import { mapGetters, mapMutations, mapState } from "vuex";

const { MessageStatus } = messageUtils;
const { isNewMessage } = draftUtils;

export default {
    name: "MailComposerToolbar",
    components: {
        BmToolbar,
        BmToolbarIconButton,
        BmButton,
        BmDropdownItem,
        BmDropdownItemButton,
        BmDropdownItemToggle,
        BmIcon
    },
    mixins: [ComposerActionsMixin, FormattedDateMixin],
    props: {
        message: { type: Object, required: true },
        isSignatureInserted: { type: Boolean, required: true },
        isDeliveryStatusRequested: { type: Boolean, required: true },
        isDispositionNotificationRequested: { type: Boolean, required: true }
    },
    setup() {
        const { execAddAttachments } = useAddAttachmentsCommand();
        return { execAddAttachments };
    },
    computed: {
        ...mapGetters("mail", { IS_SENDER_SHOWN }),
        ...mapState("mail", ["isPopup"]),
        userSettings() {
            return this.$store.state.settings;
        },
        hasCorporateSignature() {
            return Boolean(this.$store.getters[`mail/${SIGNATURE}`]?.uid);
        },
        isSaving() {
            return this.message.status === MessageStatus.SAVING;
        },
        saveMessage() {
            const kind = this.isDraft ? "draft" : "template";
            if (this.isSaving) {
                return this.$t(`mail.compose.save.${kind}.inprogress`);
            }
            if (this.errorOccuredOnSave) {
                return this.$t(`mail.compose.save.${kind}.error`);
            }
            if (isNewMessage(this.message)) {
                return "";
            }

            return this.saveMessageWithDate;
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
            // FIXME
            this.$refs["3dots-dropdown"].hide(false);
        },
        toggleTextFormattingToolbar() {
            this.SET_SHOW_FORMATTING_TOOLBAR(!this.showFormattingToolbar);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/typography";
@import "~@bluemind/ui-components/src/css/utils/variables";

.mail-composer-toolbar {
    flex-wrap: wrap;
    gap: $sp-5;
    padding: $sp-5 + $sp-2 $sp-4;

    border-top: 1px solid $neutral-fg-lo3;

    .main-buttons {
        flex: none;
    }

    .save-message {
        @include caption;
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
