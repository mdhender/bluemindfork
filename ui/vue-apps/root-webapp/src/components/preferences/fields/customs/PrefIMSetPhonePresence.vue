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
            <div class="forward-calls-to">
                <bm-form-radio :value="phone" :aria-label="$t('preferences.telephony.forward_calls')">
                    {{ $t("preferences.telephony.forward_calls") }}
                </bm-form-radio>
                <bm-form-group
                    label-for="forward-calls"
                    :state="forwardCallsInputState"
                    :invalid-feedback="$t('preferences.telephony.invalid_phone_number')"
                >
                    <bm-form-input
                        id="forward-calls"
                        v-model="phone"
                        aria-describedby="phone-number-input-feedback"
                        type="tel"
                        :disabled="isInputDisabled"
                        :state="forwardCallsInputState"
                    />
                </bm-form-group>
            </div>
        </template>
    </bm-form-radio-group>
</template>

<script>
import { isValidPhoneNumber } from "libphonenumber-js";

import { BmFormGroup, BmFormInput, BmFormRadio, BmFormRadioGroup } from "@bluemind/ui-components";

import OneSettingField from "../../mixins/OneSettingField";

export default {
    name: "PrefIMSetPhonePresence",
    components: { BmFormGroup, BmFormInput, BmFormRadio, BmFormRadioGroup },
    mixins: [OneSettingField],
    data() {
        return {
            doNothingValue: "false",
            answeringMachineValue: "dnd"
        };
    },
    computed: {
        userLang() {
            return this.$store.state.settings.lang;
        },
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
        forwardCallsInputState() {
            if (this.isInputDisabled) {
                return null;
            }
            return isValidPhoneNumber(this.phone, this.userLang.toUpperCase());
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

.pref-im-set-phone-presence {
    .bm-form-radio {
        display: flex;
        align-items: center;
        height: $input-height;
    }

    .forward-calls-to {
        display: flex;
        flex-wrap: wrap;
        gap: 0 $sp-5;

        .bm-form-radio {
            white-space: nowrap;

            .custom-control-inline {
                margin-right: 0;
            }
        }

        .form-group {
            margin-bottom: 0;
            width: 100%;
            max-width: base-px-to-rem(300);
        }
    }
}
</style>
