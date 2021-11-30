<template>
    <bm-form-radio-group v-model="value" class="pref-im-set-phone-presence">
        <div class="mb-2">{{ $t("preferences.telephony.update_phone_status") }}</div>
        <template>
            <bm-form-radio :value="doNothingValue" :aria-label="$t('common.do_nothing')">
                {{ $t("common.do_nothing") }}
            </bm-form-radio>
            <bm-form-radio :value="answeringMachineValue" :aria-label="$t('preferences.telephony.answering_machine')">
                {{ $t("preferences.telephony.answering_machine") }}
            </bm-form-radio>
            <bm-form-radio :value="phone" :aria-label="$t('preferences.telephony.forward_calls')">
                {{ $t("preferences.telephony.forward_calls") }}
            </bm-form-radio>
            <bm-form-input
                v-model="phone"
                aria-describedby="phone-number-input-feedback"
                type="tel"
                :disabled="isInputDisabled"
                :state="isValid"
            />
            <bm-form-invalid-feedback id="phone-number-input-feedback" :state="isValid">
                {{ $t("preferences.telephony.invalid_phone_number") }}
            </bm-form-invalid-feedback>
        </template>
    </bm-form-radio-group>
</template>

<script>
import { isValidPhoneNumber } from "libphonenumber-js";
import { mapState } from "vuex";

import { BmFormInput, BmFormInvalidFeedback, BmFormRadio, BmFormRadioGroup } from "@bluemind/styleguide";

import OneSettingField from "../../mixins/OneSettingField";

export default {
    name: "PrefIMSetPhonePresence",
    components: {
        BmFormInput,
        BmFormInvalidFeedback,
        BmFormRadio,
        BmFormRadioGroup
    },
    mixins: [OneSettingField],
    data() {
        return {
            doNothingValue: "false",
            answeringMachineValue: "dnd"
        };
    },
    computed: {
        ...mapState("session", { settings: ({ settings }) => settings.remote }),
        phone: {
            get() {
                return this.isInputDisabled ? "" : this.value;
            },
            set(value) {
                this.value = value;
            }
        },
        isInputDisabled() {
            return this.value === this.doNothingValue || this.value === this.answeringMachineValue;
        },
        isValid() {
            if (this.isInputDisabled) {
                return true;
            }
            return isValidPhoneNumber(this.phone, this.settings.lang.toUpperCase());
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.pref-im-set-phone-presence {
    .bm-form-radio {
        margin-bottom: $sp-2;
    }
}
</style>
