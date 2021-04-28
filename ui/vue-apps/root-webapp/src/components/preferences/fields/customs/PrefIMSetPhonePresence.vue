<template>
    <bm-form-radio-group
        v-model="localUserSettings[setting]"
        class="pref-im-set-phone-presence"
        @change="onRadioChange"
    >
        <div class="mb-2">{{ $t("preferences.telephony.update_phone_status") }}</div>
        <bm-form-radio :value="doNothingValue" :aria-label="$t('common.do_nothing')">
            {{ $t("common.do_nothing") }}
        </bm-form-radio>
        <bm-form-radio :value="answeringMachineValue" :aria-label="$t('preferences.telephony.answering_machine')">
            {{ $t("preferences.telephony.answering_machine") }}
        </bm-form-radio>
        <bm-form-radio :value="phoneNumber" :aria-label="$t('preferences.telephony.forward_calls')">
            {{ $t("preferences.telephony.forward_calls") }}
        </bm-form-radio>
        <bm-form-input
            v-model="phoneNumber"
            aria-describedby="phone-number-input-feedback"
            type="tel"
            :disabled="isInputDisabled"
            :state="isPhoneNumberValid"
            @input="onPhoneUpdate"
        />
        <bm-form-invalid-feedback id="phone-number-input-feedback" :state="isPhoneNumberValid">
            {{ $t("preferences.telephony.invalid_phone_number") }}
        </bm-form-invalid-feedback>
    </bm-form-radio-group>
</template>

<script>
import { isValidPhoneNumber } from "libphonenumber-js";
import { mapMutations, mapState } from "vuex";

import { BmFormInput, BmFormInvalidFeedback, BmFormRadio, BmFormRadioGroup } from "@bluemind/styleguide";

import PrefFieldMixin from "../../mixins/PrefFieldMixin";

export default {
    name: "PrefIMSetPhonePresence",
    components: {
        BmFormInput,
        BmFormInvalidFeedback,
        BmFormRadio,
        BmFormRadioGroup
    },
    mixins: [PrefFieldMixin],
    data() {
        return {
            doNothingValue: "false",
            answeringMachineValue: "dnd",
            phoneNumber: ""
        };
    },
    computed: {
        ...mapState("session", { settings: ({ settings }) => settings.remote }),
        isInputDisabled() {
            return (
                this.localUserSettings[this.setting] === this.doNothingValue ||
                this.localUserSettings[this.setting] === this.answeringMachineValue
            );
        },
        isPhoneNumberValid() {
            if (this.isInputDisabled) {
                return null;
            }
            return isValidPhoneNumber(this.phoneNumber, this.settings.lang.toUpperCase());
        }
    },
    watch: {
        localUserSettings(value, old) {
            if (
                value[this.setting] !== old[this.setting] &&
                value[this.setting] !== this.doNothingValue &&
                value[this.setting] !== this.answeringMachineValue
            ) {
                this.phoneNumber = value[this.setting];
            }
        }
    },
    mounted() {
        if (!this.isInputDisabled) {
            this.phoneNumber = this.localUserSettings[this.setting];
        }
    },
    methods: {
        ...mapMutations("session", ["ADD_LOCAL_HAS_ERROR", "REMOVE_LOCAL_HAS_ERROR"]),
        onRadioChange() {
            this.phoneNumber = "";
            if (!this.isInputDisabled) {
                this.ADD_LOCAL_HAS_ERROR(this.$options.name);
            } else {
                this.REMOVE_LOCAL_HAS_ERROR(this.$options.name);
            }
        },
        onPhoneUpdate(value) {
            this.localUserSettings[this.setting] = value;
            if (this.isPhoneNumberValid) {
                this.REMOVE_LOCAL_HAS_ERROR(this.$options.name);
            } else {
                this.ADD_LOCAL_HAS_ERROR(this.$options.name);
            }
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
