<template>
    <div class="pref-thread">
        <div class="pref-thread-recipients">
            <div :class="{ disabled }">{{ $t("preferences.mail.thread.recipients_order") }}</div>
            <bm-form-radio-group v-model="recipientsOrder" class="d-flex flex-wrap flex-column py-5">
                <bm-form-radio
                    v-for="choice in recipients_order_choices"
                    :key="choice.value"
                    :value="choice.value"
                    :aria-label="choice.name"
                    :disabled="disabled"
                >
                    {{ choice.name }}
                </bm-form-radio>
            </bm-form-radio-group>
        </div>
        <div class="pref-thread-messages">
            <div :class="{ disabled }">{{ $t("preferences.mail.thread.messages_order") }}</div>
            <bm-form-radio-group v-model="messagesOrder" class="d-flex flex-wrap flex-column py-5">
                <bm-form-radio
                    v-for="choice in messages_order_choices"
                    :key="choice.value"
                    :value="choice.value"
                    :aria-label="choice.name"
                    :disabled="disabled"
                >
                    {{ choice.name }}
                </bm-form-radio>
            </bm-form-radio-group>
        </div>
    </div>
</template>
<script>
import { BmFormRadio, BmFormRadioGroup } from "@bluemind/ui-components";
import MultipleSettingsField from "../../mixins/MultipleSettingsField";

import PrefFieldChoice from "../PrefFieldChoice.vue";
import i18n from "@bluemind/i18n";
import store from "@bluemind/store";
import { mapState } from "vuex";

const MAIL_THREAD_RECIPIENTS_ORDER = "mail_thread_recipients_order";
const MAIL_THREAD_MESSAGES_ORDER = "mail_thread_messages_order";

export default {
    name: "PrefThread",
    components: { BmFormRadio, BmFormRadioGroup },
    mixins: [MultipleSettingsField],
    props: {
        settings: {
            type: Array,
            required: false,
            default: () => [MAIL_THREAD_RECIPIENTS_ORDER, MAIL_THREAD_MESSAGES_ORDER]
        }
    },
    data() {
        return {
            recipients_order_choices: [
                { name: i18n.t("preferences.mail.thread.recipients_order.old_first"), value: "ASC" },
                { name: i18n.t("preferences.mail.thread.recipients_order.new_first"), value: "DESC" }
            ],
            messages_order_choices: [
                { name: i18n.t("preferences.mail.thread.messages_order.old_first"), value: "ASC" },
                { name: i18n.t("preferences.mail.thread.messages_order.new_first"), value: "DESC" }
            ]
        };
    },
    computed: {
        ...mapState("settings", ["mail_thread"]),
        recipientsOrder: {
            get() {
                return this.value[MAIL_THREAD_RECIPIENTS_ORDER] ? this.value[MAIL_THREAD_RECIPIENTS_ORDER] : "ASC";
            },
            set(newValue) {
                store.dispatch("settings/SAVE_SETTING", {
                    setting: MAIL_THREAD_RECIPIENTS_ORDER,
                    value: newValue
                });
            }
        },
        messagesOrder: {
            get() {
                return this.value[MAIL_THREAD_MESSAGES_ORDER] ? this.value[MAIL_THREAD_MESSAGES_ORDER] : "ASC";
            },
            set(newValue) {
                store.dispatch("settings/SAVE_SETTING", {
                    setting: MAIL_THREAD_MESSAGES_ORDER,
                    value: newValue
                });
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";
.pref-thread {
    .pref-thread-messages {
        margin-top: $sp-3;
    }
    .pref-thread-recipients {
        margin-top: $sp-6;
    }
    .disabled {
        color: $neutral-fg-disabled;
    }
    .pref-thread-recipients,
    .pref-thread-messages {
        .bm-form-radio {
            padding-top: $sp-4;
            padding-bottom: $sp-4;
        }
    }
}
</style>
