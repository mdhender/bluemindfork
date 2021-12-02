<template>
    <bm-modal
        :id="$attrs['id']"
        ref="pref-ext-account-modal-bm-modal"
        class="pref-ext-account-modal"
        centered
        :title="
            externalAccount_.isNew
                ? $t('preferences.account.external_accounts.modal.title.create')
                : $t('preferences.account.external_accounts.modal.title.edit')
        "
        :cancel-title="$t('common.cancel')"
        :ok-title="externalAccount_.isNew ? $t('common.create') : $t('common.edit')"
        :ok-disabled="okDisabled"
        @ok="save"
        @shown="init"
    >
        <div v-if="externalAccount.identifier" class="d-flex justify-content-center">
            <img
                :src="externalAccount_.logo && externalAccount_.logo.src"
                :alt="externalAccount_.identifier"
                :title="externalAccount_.description"
            />
        </div>
        <bm-form class="mt-4" @submit.prevent="submit">
            <bm-form-group
                id="login-group"
                :label="$t('common.login')"
                label-for="login"
                :description="$t('preferences.account.external_accounts.modal.login.desc')"
            >
                <bm-form-input id="login" ref="login-input" v-model="externalAccount_.login" required />
            </bm-form-group>
            <bm-form-group
                id="password-group"
                :label="$t('common.password')"
                label-for="password"
                :description="$t('preferences.account.external_accounts.modal.password.desc')"
            >
                <bm-form-input
                    id="password"
                    ref="password-input"
                    v-model="externalAccount_.credentials"
                    :type="showPassword ? 'text' : 'password'"
                    required
                    actionable-icon
                    icon="eye"
                    :placeholder="
                        externalAccount_.isNew
                            ? undefined
                            : $t('preferences.account.external_accounts.modal.password.placeholder')
                    "
                    @icon-click="showPassword = !showPassword"
                />
            </bm-form-group>
            <bm-form-group
                id="authentication-test-group"
                :label="$t('preferences.account.external_accounts.modal.authentication.test')"
                label-for="authentication-test"
            >
                <bm-button
                    id="authentication-test"
                    variant="outline-secondary"
                    :disabled="
                        testStatus === TestStatus.IN_PROGRESS ||
                        !externalAccount_.login ||
                        !externalAccount_.credentials
                    "
                    @click="testAccount"
                >
                    {{ $t("common.test") }}
                </bm-button>
            </bm-form-group>
            <bm-spinner v-if="testStatus === TestStatus.IN_PROGRESS" :size="0.15" />
            <bm-label-icon
                v-else-if="testStatus !== TestStatus.IDLE"
                :icon="testStatus === TestStatus.VERIFIED ? 'check-circle' : 'exclamation-circle'"
                :class="testStatus === TestStatus.VERIFIED ? 'text-success' : 'text-danger'"
            >
                {{
                    testStatus === TestStatus.VERIFIED
                        ? $t("preferences.account.external_accounts.modal.authentication.verified")
                        : $t("preferences.account.external_accounts.modal.authentication.failed")
                }}
            </bm-label-icon>
        </bm-form>
    </bm-modal>
</template>

<script>
import cloneDeep from "lodash.clonedeep";
import { BmButton, BmForm, BmFormGroup, BmFormInput, BmLabelIcon, BmModal, BmSpinner } from "@bluemind/styleguide";

const TestStatus = { IDLE: "IDLE", IN_PROGRESS: "IN_PROGRESS", VERIFIED: "VERIFIED", REJECTED: "REJECTED" };

export default {
    name: "PrefExtAccountModal",
    components: { BmButton, BmForm, BmFormGroup, BmFormInput, BmLabelIcon, BmModal, BmSpinner },

    props: {
        externalAccount: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            externalAccount_: {},
            showPassword: false,
            testStatus: undefined,
            TestStatus
        };
    },
    computed: {
        okDisabled() {
            return !this.externalAccount_.login || (this.externalAccount_.isNew && !this.externalAccount_.credentials);
        }
    },
    methods: {
        init() {
            this.externalAccount_ = cloneDeep(this.externalAccount);
            this.showPassword = false;
            this.testStatus = TestStatus.IDLE;
            if (this.externalAccount_.isNew) {
                this.$refs["login-input"].focus();
            }
        },
        show() {
            this.$refs["pref-ext-account-modal-bm-modal"].show();
        },
        hide() {
            this.$refs["pref-ext-account-modal-bm-modal"].hide();
        },
        save() {
            this.$emit("updateExternalAccount", { ...this.externalAccount_ });
        },
        submit() {
            if (!this.okDisabled) {
                this.save();
                this.hide();
            }
        },
        testAccount() {
            this.testStatus = TestStatus.IN_PROGRESS;
            setTimeout(() => {
                if (Math.round(Math.random())) {
                    this.testStatus = TestStatus.VERIFIED;
                } else {
                    this.testStatus = TestStatus.REJECTED;
                }
            }, 1000);
        }
    }
};
</script>
