<template>
    <div class="mail-composer-recipients pr-1">
        <div class="d-flex align-items-center to-contact-input">
            <mail-composer-recipient
                ref="toField"
                :message="message"
                recipient-type="to"
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
                </div>
            </mail-composer-recipient>
        </div>
        <div v-if="showCc" class="d-flex align-items-center cc-contact-input">
            <mail-composer-recipient
                ref="ccField"
                :message="message"
                recipient-type="cc"
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
            @open-picker="selectedContactsType = 'bcc'"
        />

        <mail-composer-recipient-modal :selected.sync="selectedContacts" />
    </div>
</template>

<script>
import capitalize from "lodash.capitalize";
import isEqual from "lodash.isequal";
import { RecipientAdaptor } from "@bluemind/contact";
import { BmButton, KeyNavGroup } from "@bluemind/ui-components";
import { EditRecipientsMixin } from "~/mixins";
import MailComposerRecipient from "./MailComposerRecipient";
import MailComposerRecipientModal from "./MailComposerRecipientModal.vue";
import { MAX_RECIPIENTS } from "../../utils";

export default {
    name: "MailComposerRecipients",
    components: { BmButton, MailComposerRecipient, MailComposerRecipientModal },
    directives: { KeyNavGroup },
    mixins: [EditRecipientsMixin],
    data() {
        return { selectedContactsType: undefined };
    },
    computed: {
        recipientCount() {
            return this.message.to.length + this.message.cc.length + this.message.bcc.length;
        },
        maxRecipientsExceeded() {
            return this.recipientCount > MAX_RECIPIENTS;
        },
        selectedContacts: {
            get() {
                return RecipientAdaptor.toContacts(this.message[this.selectedContactsType]);
            },
            set(value) {
                const newValue = RecipientAdaptor.fromContacts(value);
                if (!isEqual(this.message[this.selectedContactsType], newValue)) {
                    this.message[this.selectedContactsType] = newValue;
                }
            }
        }
    },
    watch: {
        maxRecipientsExceeded: {
            handler: function (exceeded) {
                exceeded
                    ? this.ERROR({
                          alert: {
                              name: "mail.max_recipients_exceeded",
                              uid: "MAX_RECIPIENTS",
                              payload: { max: MAX_RECIPIENTS, count: this.recipientCount }
                          },
                          options: { area: "right-panel" }
                      })
                    : this.REMOVE({ uid: "MAX_RECIPIENTS" });
            },
            immediate: true
        }
    },
    methods: {
        async showAndFocusRecipientField(recipientType) {
            this[`show${capitalize(recipientType)}`] = true;
            await this.$nextTick();
            this.focusRecipientField(recipientType);
        },
        focusRecipientField(recipientType) {
            this.$refs[`${recipientType}Field`]?.$el.querySelector("input").focus();
        }
    }
};
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
