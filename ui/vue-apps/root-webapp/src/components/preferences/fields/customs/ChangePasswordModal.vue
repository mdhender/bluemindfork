<template>
    <bm-modal
        v-model="show"
        centered
        :title="$t('preferences.security.change_password')"
        :cancel-title="$t('common.cancel')"
        :ok-title="$t('common.save')"
        :ok-disabled="disableSave"
        @ok="save"
        @cancel="cancel"
    >
        <bm-form class="mt-4">
            <bm-form-group
                id="old-password-group"
                :label="$t('preferences.security.change_password.old')"
                label-for="old-password"
                :invalid-feedback="$t('preferences.security.change_password.old.invalid')"
                :state="isOldPasswordValid"
            >
                <bm-form-input
                    id="old-password"
                    v-model="oldPassword"
                    :type="showOldPassword ? 'text' : 'password'"
                    :state="isOldPasswordValid"
                    required
                    actionable-icon
                    icon="eye"
                    @icon-click="showOldPassword = !showOldPassword"
                />
            </bm-form-group>
            <bm-form-group
                id="new-password-group"
                label-for="new-password"
                :label="$t('preferences.security.change_password.new')"
                :invalid-feedback="invalidNewPassword"
                :state="isNewPasswordValid"
            >
                <bm-form-input
                    id="new-password"
                    v-model="newPassword"
                    :type="showNewPassword ? 'text' : 'password'"
                    :state="isNewPasswordValid"
                    required
                    actionable-icon
                    icon="eye"
                    @icon-click="showNewPassword = !showNewPassword"
                />
            </bm-form-group>
            <bm-form-group
                id="confirmed-password-group"
                :label="$t('preferences.security.change_password.confirm_new')"
                label-for="confirm-new-password"
                :invalid-feedback="$t('preferences.security.change_password.confirm_new.invalid')"
                :state="isConfirmedPasswordValid"
            >
                <bm-form-input
                    id="confirm-new-password"
                    v-model="newConfirmedPassword"
                    :type="showConfirmedPassword ? 'text' : 'password'"
                    :state="isConfirmedPasswordValid"
                    required
                    actionable-icon
                    icon="eye"
                    @icon-click="showConfirmedPassword = !showConfirmedPassword"
                />
            </bm-form-group>
        </bm-form>
    </bm-modal>
</template>

<script>
import { mapActions } from "vuex";
import { inject } from "@bluemind/inject";
import { BmForm, BmFormGroup, BmFormInput, BmModal } from "@bluemind/ui-components";
import { SUCCESS } from "@bluemind/alert.store";
import { SAVE_ALERT } from "../../Alerts/defaultAlerts";

export default {
    name: "ChangePasswordModal",
    components: { BmForm, BmFormGroup, BmFormInput, BmModal },
    data() {
        return {
            show: false,

            oldPassword: "",
            newPassword: "",
            newConfirmedPassword: "",

            showOldPassword: false,
            showNewPassword: false,
            showConfirmedPassword: false,

            isOldPasswordValid: null
        };
    },
    computed: {
        isNewPasswordValid() {
            if (!this.newPassword) {
                return null;
            }
            return this.newPassword !== this.oldPassword && isASCII(this.newPassword) === true;
        },
        isConfirmedPasswordValid() {
            if (!this.newConfirmedPassword) {
                return null;
            }
            return this.newPassword === this.newConfirmedPassword;
        },
        disableSave() {
            return this.isOldPasswordValid === false || !this.isNewPasswordValid || !this.isConfirmedPasswordValid;
        },
        invalidNewPassword() {
            if (this.newPassword) {
                const areAllCharactersValid = this.newPassword && isASCII(this.newPassword);
                if (this.newPassword === this.oldPassword) {
                    return this.$t("preferences.security.change_password.same_password");
                } else if (areAllCharactersValid !== true) {
                    return this.$t("common.invalid.character", { character: areAllCharactersValid });
                }
            }
            return "";
        }
    },
    watch: {
        oldPassword() {
            this.isOldPasswordValid = null;
        }
    },
    methods: {
        ...mapActions("alert", { SUCCESS }),
        async open() {
            this.show = true;
        },
        async save(event) {
            event.preventDefault();
            try {
                const userId = inject("UserSession").userId;
                await inject("UserPersistence").setPassword(userId, {
                    currentPassword: this.oldPassword,
                    newPassword: this.newPassword
                });
                this.show = false;
                this.SUCCESS(SAVE_ALERT);

                this.cancel();
            } catch (error) {
                if (error.message.includes("password is not valid")) {
                    this.isOldPasswordValid = false;
                }
            }
        },
        cancel() {
            this.oldPassword = "";
            this.newPassword = "";
            this.newConfirmedPassword = "";
        }
    }
};

function isASCII(str) {
    const charactersArray = Array.from(str);
    let i = 0;
    while (i < charactersArray.length) {
        // eslint-disable-next-line no-control-regex
        if (!/^[\x00-\x7F]*$/.test(charactersArray[i])) {
            return charactersArray[i];
        }
        i++;
    }
    return true;
}
</script>
