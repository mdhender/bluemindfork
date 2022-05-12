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
                v-if="needsCredentials"
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
                    autocomplete="new-password"
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
                    :disabled="testStatus === TestStatus.IN_PROGRESS || !externalAccount_.login || !hasCredentials"
                    @click="testAccount(externalAccount_)"
                >
                    {{ $t("common.test") }}
                </bm-button>
            </bm-form-group>
            <bm-spinner v-if="testStatus === TestStatus.IN_PROGRESS" :size="0.15" />
            <bm-label-icon
                v-else-if="testStatus !== TestStatus.IDLE"
                :icon="testAccountResultIcon"
                :class="testAccountResultIconClass"
            >
                {{ testAccountResultText }}
            </bm-label-icon>
        </bm-form>
    </bm-modal>
</template>

<script>
import cloneDeep from "lodash.clonedeep";
import { inject } from "@bluemind/inject";
import { BmButton, BmForm, BmFormGroup, BmFormInput, BmLabelIcon, BmModal, BmSpinner } from "@bluemind/styleguide";

const TestStatus = {
    IDLE: Symbol("IDLE"),
    IN_PROGRESS: Symbol("IN_PROGRESS"),
    NOT_SUPPORTED: Symbol("NOT_SUPPORTED"),
    REJECTED: Symbol("REJECTED"),
    VERIFIED: Symbol("VERIFIED")
};

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
            return !this.externalAccount_.login || (this.externalAccount_.isNew && !this.hasCredentials);
        },
        testAccountResultIcon() {
            return this.testStatus === TestStatus.VERIFIED
                ? "check-circle"
                : this.testStatus === TestStatus.REJECTED
                ? "exclamation-circle-fill"
                : "info-circle";
        },
        testAccountResultIconClass() {
            return this.testStatus === TestStatus.VERIFIED
                ? "text-success"
                : this.testStatus === TestStatus.REJECTED
                ? "text-danger"
                : "text-info";
        },
        testAccountResultText() {
            return this.testStatus === TestStatus.VERIFIED
                ? this.$t("preferences.account.external_accounts.modal.authentication.verified")
                : this.testStatus === TestStatus.REJECTED
                ? this.$t("preferences.account.external_accounts.modal.authentication.failed")
                : this.$t("preferences.account.external_accounts.modal.authentication.not_supported");
        },
        needsCredentials() {
            return this.externalAccount.authKind !== "NONE";
        },
        hasCredentials() {
            return !this.needsCredentials || this.externalAccount_.credentials;
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
        async testAccount(externalAccount) {
            this.testStatus = TestStatus.IN_PROGRESS;
            try {
                const result = await testAccount(externalAccount);
                switch (result) {
                    case ConnectionTestStatus.OK:
                        this.testStatus = TestStatus.VERIFIED;
                        break;
                    case ConnectionTestStatus.NOK:
                        this.testStatus = TestStatus.REJECTED;
                        break;
                    case ConnectionTestStatus.NOT_SUPPORTED:
                    default:
                        this.testStatus = TestStatus.NOT_SUPPORTED;
                }
            } catch (e) {
                this.testStatus = TestStatus.IDLE;
                throw e;
            }
        }
    }
};

const ConnectionTestStatus = { OK: "OK", NOK: "NOK", NOT_SUPPORTED: "NOT_SUPPORTED" };
async function testAccount(externalAccount) {
    return await inject("ExternalSystemPersistence").testConnection(externalAccount.identifier, {
        login: externalAccount.login,
        credentials: externalAccount.credentials
    });
}
</script>
