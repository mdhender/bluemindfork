<template>
    <div class="mail-composer-recipients pr-1">
        <div class="d-flex align-items-center to-contact-input">
            <mail-composer-recipient
                ref="toField"
                :message="message"
                recipient-type="to"
                @update:contacts="updateMailRecipients($event, 'to')"
                @open-picker="selectedContactsType = 'to'"
            >
                <div class="end-buttons">
                    <bm-button
                        v-if="!showCc"
                        v-key-nav-group:recipient-button
                        variant="text"
                        tabindex="-1"
                        @click="showAndFocusRecipientField('cc')"
                        @keydown.tab.prevent="focusRecipientField('to')"
                    >
                        {{ $t("common.cc") }}
                    </bm-button>
                    <bm-button
                        v-if="!showCc && !showBcc"
                        v-key-nav-group:recipient-button
                        variant="text"
                        tabindex="-1"
                        @click="showAndFocusRecipientField('bcc')"
                        @keydown.tab.prevent="focusRecipientField('to')"
                    >
                        {{ $t("common.bcc") }}
                    </bm-button>
                    <slot />
                </div>
            </mail-composer-recipient>
        </div>
        <div v-if="showCc" class="d-flex align-items-center cc-contact-input">
            <mail-composer-recipient
                ref="ccField"
                recipient-type="cc"
                :message="message"
                @update:contacts="updateMailRecipients($event, 'cc')"
                @open-picker="selectedContactsType = 'cc'"
            >
                <div class="end-buttons">
                    <bm-button
                        v-if="!showBcc"
                        v-key-nav-group:recipient-button
                        variant="text"
                        class="bcc-button text-nowrap"
                        tabindex="-1"
                        @click="showAndFocusRecipientField('bcc')"
                        @keydown.tab.prevent="focusRecipientField('to')"
                    >
                        {{ $t("common.bcc") }}
                    </bm-button>
                </div>
            </mail-composer-recipient>
        </div>
        <mail-composer-recipient
            v-if="showBcc"
            ref="bccField"
            :message="message"
            recipient-type="bcc"
            @update:contacts="updateMailRecipients($event, 'bcc')"
            @open-picker="selectedContactsType = 'bcc'"
        />
        <mail-composer-recipient-modal
            :selected.sync="selectedContacts"
            :recipient-contacts-type="selectedContactsType"
        />
    </div>
</template>

<script>
import { mapMutations } from "vuex";
import capitalize from "lodash.capitalize";
import isEqual from "lodash.isequal";
import { RecipientAdaptor } from "@bluemind/contact";
import { BmButton, KeyNavGroup } from "@bluemind/ui-components";
import { EditRecipientsMixin, ComposerActionsMixin } from "~/mixins";
import MailComposerRecipient from "./MailComposerRecipient";
import MailComposerRecipientModal from "../RecipientPicker/MailComposerRecipientModal";
import { MAX_RECIPIENTS } from "../../utils";
import { SET_MESSAGE_BCC, SET_MESSAGE_CC, SET_MESSAGE_TO } from "~/mutations";
import { mailTipUtils } from "@bluemind/mail";
const { getMailTipContext } = mailTipUtils;

export default {
    name: "MailComposerRecipients",
    components: { BmButton, MailComposerRecipient, MailComposerRecipientModal },
    directives: { KeyNavGroup },
    mixins: [EditRecipientsMixin, ComposerActionsMixin],
    data() {
        return { selectedContactsType: undefined };
    },
    computed: {
        selectedContacts: {
            get() {
                return RecipientAdaptor.toContacts(this.message[this.selectedContactsType]);
            },
            set(contacts) {
                const updatedRecipients = RecipientAdaptor.fromContacts(contacts?.values ?? contacts);
                const recipientType = contacts?.recipientType ?? this.selectedContactsType;

                if (!isEqual(this.message[recipientType], updatedRecipients)) {
                    this[`SET_MESSAGE_${recipientType.toUpperCase()}`]({
                        messageKey: this.message.key,
                        [recipientType]: updatedRecipients
                    });
                    this.$execute("get-mail-tips", { context: getMailTipContext(this.message), message: this.message });
                }
            }
        }
    },
    watch: {
        selectedContacts: {
            handler(selection, previousSelection) {
                if (!isEqual(selection, previousSelection)) {
                    this.debouncedSave();
                }
            }
        },
        maxRecipientsExceeded: {
            handler: function (exceeded) {
                const alertInComposerUid = "MAX_RECIPIENTS";
                const alertInPickerUid = "PICKER_MAX_RECIPIENTS";
                if (exceeded) {
                    this.ERROR(buildMaxRecipientsExceededAlert(alertInComposerUid, "right-panel"));
                    this.ERROR(buildMaxRecipientsExceededAlert(alertInPickerUid, "recipient-picker"));
                } else {
                    this.REMOVE({ uid: alertInComposerUid });
                    this.REMOVE({ uid: alertInPickerUid });
                }
            },
            immediate: true
        }
    },
    methods: {
        ...mapMutations("mail", { SET_MESSAGE_TO, SET_MESSAGE_CC, SET_MESSAGE_BCC }),
        async showAndFocusRecipientField(recipientType) {
            this[`show${capitalize(recipientType)}`] = true;
            await this.$nextTick();
            this.focusRecipientField(recipientType);
        },
        focusRecipientField(recipientType) {
            this.$refs[`${recipientType}Field`]?.$el.querySelector("input").focus();
        },
        updateMailRecipients(contacts, type) {
            this.selectedContactsType = type;
            this.selectedContacts = { recipientType: type, values: contacts };
        }
    }
};

function buildMaxRecipientsExceededAlert(uid, area) {
    return {
        alert: { name: "mail.max_recipients_exceeded", uid, payload: { max: MAX_RECIPIENTS } },
        options: { area }
    };
}
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.mail-composer-recipients {
    .to-contact-input,
    .cc-contact-input {
        flex: 1;
        min-width: 0;
    }

    .end-buttons {
        display: flex;
        gap: $sp-4;
        align-items: flex-start;
        flex: none;
        order: 12;
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
