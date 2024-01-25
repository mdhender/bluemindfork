<template>
    <bm-modal
        v-model="show"
        size="lg"
        variant="advanced"
        scrollable
        height="lg"
        :title="isNewIdentity ? $t('preferences.mail.identities.create') : $t('preferences.mail.identities.update')"
        body-class="manage-identity-modal-body"
        @hidden="modalStatus = 'NOT-LOADED'"
        @show="showAliases = false"
    >
        <template v-if="modalStatus === 'LOADED'">
            <div class="head-part">
                <div class="avatar-part">
                    <bm-avatar size="md" class="mobile-only" :alt="identity.name || '?'" />
                    <bm-avatar size="xl" class="desktop-only" :alt="identity.name || '?'" />
                    <template v-if="originalIdentity.isDefault">
                        <div><bm-icon icon="star-fill" size="xl" /></div>
                        <div class="caption">{{ $t("preferences.mail.identities.default") }}</div>
                    </template>
                </div>
                <div class="form-part">
                    <bm-form-group
                        :label="$t('common.display_name')"
                        label-for="displayname"
                        :description="$t('preferences.mail.identities.form.display_name.info')"
                    >
                        <bm-form-input id="displayname" v-model="identity.displayname" type="email" required />
                    </bm-form-group>
                    <bm-form-group
                        :label="$t('common.label')"
                        label-for="label"
                        :description="$t('preferences.mail.identities.form.label.info')"
                    >
                        <bm-form-input
                            id="label"
                            v-model="identity.name"
                            type="email"
                            required
                            :placeholder="$t('preferences.mail.identities.form.label.placeholder')"
                        />
                    </bm-form-group>
                    <div
                        v-if="!originalIdentity.isDefault"
                        class="mb-3 change-default"
                        @click="identity.isDefault = !identity.isDefault"
                    >
                        <bm-icon :icon="identity.isDefault ? 'star-fill' : 'star'" class="mr-1" size="lg" />
                        <div>{{ $t("preferences.mail.identities.make_default") }}</div>
                    </div>
                </div>
            </div>
            <div class="my-4">
                <div class="mb-1">{{ $t("common.email_address") }}</div>
                <bm-combo-box
                    v-if="possibleIdentitiesStatus === 'LOADED'"
                    v-model="emailInput"
                    :items="emailFilteredChoices"
                    :max-results="10"
                    @input="newInput => (emailInput = newInput)"
                    @selected="selected => checkComboSelection(selected)"
                    @close="checkComboSelection(emailInput)"
                    @submit="checkComboSelection(emailInput)"
                    @submitExtra="showAliases = !showAliases"
                >
                    <template #extra>
                        <div class="bold show-aliases">
                            {{
                                showAliases
                                    ? $t("preferences.mail.identities.aliases.hide")
                                    : $t("preferences.mail.identities.aliases.show")
                            }}
                        </div>
                    </template>
                </bm-combo-box>
                <bm-form-input v-else v-model="identity.email" disabled />
                <div v-if="possibleIdentitiesStatus === 'ERROR'" class="text-warning mt-1 word-break">
                    {{ $t("preferences.mail.identities.possible_identities_error") }} <br />
                    {{ $t("common.application.bootstrap.error.solution1") }} <br />
                    {{ $t("common.application.bootstrap.error.solution2") }}
                </div>
            </div>
            <div v-if="canUseOtherSentFolder" class="mb-5">
                <bm-form-checkbox v-model="identity.sentFolder" value="" :unchecked-value="SENT_FOLDER">
                    {{
                        $tc("preferences.mail.identities.use_sentbox", 0, {
                            address: identity.email + " (" + identity.displayname + ")"
                        })
                    }}
                </bm-form-checkbox>
            </div>
            <div class="rich-editor-with-label">
                {{ $t("common.signature") }}
                <bm-rich-editor
                    ref="rich-editor"
                    :init-value="identity.signature"
                    :dark-mode="IS_COMPUTED_THEME_DARK"
                    :default-font-family="composer_default_font"
                    :extra-font-families="EXTRA_FONT_FAMILIES"
                    show-toolbar
                    has-border
                    class="mt-1"
                    name="personal-signature"
                    @input="onInput"
                />
            </div>
        </template>
        <bm-spinner v-else-if="modalStatus === 'NOT-LOADED'" class="m-auto py-4" />
        <div v-else class="word-break text-warning">
            {{ $t("preferences.mail.identities.identity_error") }}<br />
            {{ $t("common.application.bootstrap.error.solution") }}
        </div>
        <template #modal-footer>
            <template v-if="modalStatus === 'LOADED'">
                <bm-icon-button
                    v-if="!isNewIdentity"
                    variant="compact"
                    icon="trash"
                    class="mr-auto mobile-only"
                    :disabled="identity.isDefault"
                    @click="remove"
                />
                <bm-button
                    v-if="!isNewIdentity"
                    variant="outline"
                    icon="trash"
                    class="mr-auto desktop-only"
                    :disabled="identity.isDefault"
                    @click="remove"
                >
                    {{ $t("preferences.mail.identities.delete") }}
                </bm-button>
                <bm-button variant="text" @click="cancel">{{ $t("common.cancel") }}</bm-button>
                <bm-button v-if="isNewIdentity" variant="fill-accent" :disabled="!isFormValid" @click="add">
                    {{ $t("common.add") }}
                </bm-button>
                <bm-button v-else variant="fill-accent" :disabled="!hasAnyChange || !isFormValid" @click="save">
                    {{ $t("common.save") }}
                </bm-button>
            </template>
            <template v-else><div /></template>
        </template>
    </bm-modal>
</template>

<script>
import cloneDeep from "lodash.clonedeep";
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";
import { Verb } from "@bluemind/core.container.api";
import { EmailValidator } from "@bluemind/email";
import { signatureUtils, folderUtils } from "@bluemind/mail";
import { removeDuplicatedIds, sanitizeHtml } from "@bluemind/html-utils";
import { inject } from "@bluemind/inject";
import BmRoles from "@bluemind/roles";
import {
    BmAvatar,
    BmButton,
    BmComboBox,
    BmFormCheckbox,
    BmFormGroup,
    BmFormInput,
    BmIcon,
    BmIconButton,
    BmModal,
    BmRichEditor,
    BmSpinner
} from "@bluemind/ui-components";
import UUIDGenerator from "@bluemind/uuid";
import { SUCCESS } from "@bluemind/alert.store";
import { SAVE_ALERT } from "../../Alerts/defaultAlerts";
const { DEFAULT_FOLDERS } = folderUtils;

export default {
    name: "ManageIdentityModal",
    components: {
        BmAvatar,
        BmButton,
        BmComboBox,
        BmFormCheckbox,
        BmFormGroup,
        BmFormInput,
        BmIcon,
        BmIconButton,
        BmModal,
        BmRichEditor,
        BmSpinner
    },
    props: {
        possibleIdentities: {
            type: Array,
            required: true
        },
        possibleIdentitiesStatus: {
            type: String,
            required: true
        }
    },
    data() {
        return {
            show: false,
            modalStatus: "NOT-LOADED",

            isNewIdentity: false,
            id: "",
            identity: {},
            originalIdentity: {},

            emailInput: "",
            canCreateExternalIdentity: false,
            SENT_FOLDER: DEFAULT_FOLDERS.SENT,

            showAliases: false
        };
    },
    computed: {
        ...mapState("root-app", ["identities"]),
        ...mapState("settings", ["composer_default_font"]),
        ...mapGetters("root-app", ["DEFAULT_IDENTITY"]),
        ...mapGetters("settings", ["IS_COMPUTED_THEME_DARK", "EXTRA_FONT_FAMILIES"]),
        availableAddresses() {
            return this.showAliases
                ? this.possibleIdentities.map(({ email }) => email)
                : this.possibleIdentities.flatMap(({ email, emailIsDefault }) => (emailIsDefault ? email : []));
        },
        hasAnyChange() {
            return JSON.stringify(this.identity) !== JSON.stringify(this.originalIdentity);
        },
        emailFilteredChoices() {
            if (this.emailInput && this.isExternalIdentity(this.emailInput)) {
                return [this.$t("preferences.mail.identities.external_address", { email: this.emailInput })];
            }
            if (this.emailInput) {
                return this.availableAddresses.filter(address =>
                    address.toUpperCase().includes(this.emailInput.toUpperCase())
                );
            }
            return this.availableAddresses;
        },
        isFormValid() {
            return (
                (this.isExternalIdentity(this.identity.email) ||
                    this.possibleIdentities.find(identity => identity.email === this.identity.email)) &&
                this.identity.displayname !== "" &&
                this.identity.name !== ""
            );
        },
        isMyMailbox() {
            const userId = inject("UserSession").userId;
            return this.identity.mailboxUid === userId;
        },
        canUseOtherSentFolder() {
            return this.identity.mailboxUid && !this.isMyMailbox && this.identity.rights.includes(Verb.Write);
        }
    },
    methods: {
        ...mapMutations("root-app", ["ADD_IDENTITY", "REMOVE_IDENTITY", "UPDATE_IDENTITY"]),
        ...mapActions("alert", { SUCCESS }),
        onInput(content) {
            this.identity.signature = signatureUtils.trimSignature(content);
        },
        async open(identityDescription) {
            this.modalStatus = "NOT-LOADED";
            this.show = true;
            let identity = createEmpty(this.DEFAULT_IDENTITY);
            if (this.isMyMailbox) {
                identity.sentFolder = DEFAULT_FOLDERS.SENT;
            }
            this.originalIdentity = {};
            this.emailInput = "";
            this.id = "";
            this.isNewIdentity = !identityDescription;
            this.canCreateExternalIdentity = inject("UserSession").roles.includes(BmRoles.CAN_CREATE_EXTERNAL_IDENTITY);

            if (identityDescription) {
                this.id = identityDescription.id;
                try {
                    identity = await inject("UserMailIdentitiesPersistence").get(identityDescription.id);
                    if (identity.format === "PLAIN") {
                        identity.signature = "<pre>" + identity.signature + "</pre>";
                    }
                    identity.rights = await fetchRights(identity.mailboxUid);
                    this.modalStatus = "LOADED";
                } catch {
                    this.modalStatus = "ERROR";
                }
                this.originalIdentity = cloneDeep(identity);
                this.emailInput = identity.email;
            } else {
                this.modalStatus = "LOADED";
            }

            this.identity = identity;
        },
        async remove() {
            await inject("UserMailIdentitiesPersistence").delete(this.id);
            this.show = false;
            this.REMOVE_IDENTITY(this.id);
            this.SUCCESS(SAVE_ALERT);
        },
        cancel() {
            this.show = false;
        },
        async add() {
            this.identity.signature = removeDuplicatedIds(sanitizeHtml(this.identity.signature));

            const id = UUIDGenerator.generate();
            await inject("UserMailIdentitiesPersistence").create(id, this.identity);

            if (this.identity.isDefault) {
                await inject("UserMailIdentitiesPersistence").setDefault(id);
                const oldDefault = { ...this.DEFAULT_IDENTITY };
                oldDefault.isDefault = false;
                this.UPDATE_IDENTITY(oldDefault);
            }
            this.ADD_IDENTITY(toIdentityDescription(id, this.identity));

            this.show = false;
            this.SUCCESS(SAVE_ALERT);
        },
        async save() {
            this.identity.signature = removeDuplicatedIds(sanitizeHtml(this.identity.signature));

            await inject("UserMailIdentitiesPersistence").update(this.id, this.identity);
            if (!this.originalIdentity.isDefault && this.identity.isDefault) {
                await inject("UserMailIdentitiesPersistence").setDefault(this.id);
                const oldDefault = { ...this.DEFAULT_IDENTITY };
                oldDefault.isDefault = false;
                this.UPDATE_IDENTITY(oldDefault);
            }
            this.UPDATE_IDENTITY(toIdentityDescription(this.id, this.identity));

            this.show = false;
            this.SUCCESS(SAVE_ALERT);
        },
        async checkComboSelection(email) {
            if (this.isExternalIdentity(this.emailInput)) {
                this.identity.email = this.emailInput;
                this.identity.mailboxUid = null;
                this.identity.signature = "";
                this.identity.rights = [];
                this.updateSignatureEditorContent();
            } else {
                const selectedIdentity = this.possibleIdentities.find(identity => identity.email === email);
                if (selectedIdentity) {
                    this.emailInput = email;
                    this.identity.email = email;
                    this.identity.mailboxUid = selectedIdentity.mbox;

                    // selectedIdentity is an element of fetched availables identities
                    // but its signature is always null as its id, displayname and others.
                    // so we try to find first matching identity in identitiesDesc with mbox param
                    const matchingIdentityDesc = this.identities.find(
                        identity => identity.mbox === selectedIdentity.mbox
                    );
                    if (matchingIdentityDesc) {
                        this.identity.signature = matchingIdentityDesc.signature;
                        this.updateSignatureEditorContent();
                    }
                    this.identity.rights = await fetchRights(selectedIdentity.mbox);
                } else {
                    // if an invalid address is submitted, reset to previous value
                    this.emailInput = this.identity.email;
                    this.identity.rights = [];
                }
            }
        },
        isExternalIdentity(email) {
            if (this.canCreateExternalIdentity && EmailValidator.validateAddress(email)) {
                const emailDomain = email.split("@")[1];
                const possibleDomains = this.availableAddresses.map(address => address.split("@")[1]);
                return !possibleDomains.includes(emailDomain);
            }
            return false;
        },
        async updateSignatureEditorContent() {
            this.$refs["rich-editor"].setContent(this.identity.signature);
        }
    }
};

// identity model functions
function createEmpty({ email, mbox }) {
    return {
        email,
        format: "HTML",
        signature: "",
        displayname: "",
        name: "",
        isDefault: false,
        sentFolder: "",
        mailboxUid: mbox,
        rights: []
    };
}

function toIdentityDescription(id, identity) {
    return {
        id,
        name: identity.name,
        email: identity.email,
        displayname: identity.displayname,
        isDefault: identity.isDefault,
        signature: identity.signature,
        mbox: identity.mailboxUid
    };
}

async function fetchRights(mailboxUid) {
    return (await inject("ContainersPersistence").get(`mailbox:acls-${mailboxUid}`))?.verbs;
}
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";

.manage-identity-modal-body {
    display: flex;
    flex-direction: column;

    .icon-star-fill {
        color: $secondary-fg;
    }

    .head-part {
        width: 100%;
        display: flex;
        flex-direction: column;
        @include from-lg {
            flex-direction: row;
        }
        gap: $sp-5 $sp-7;

        .avatar-part {
            flex: none;

            display: flex;
            flex-direction: row;
            @include from-lg {
                flex-direction: column;
            }
            align-items: center;
            gap: $sp-3 $sp-5;
        }

        .form-part {
            flex: 1;
            @include from-lg {
                padding-top: base-px-to-rem(8);
            }

            .form-group {
                margin-bottom: $sp-4;
            }
        }
    }

    .rich-editor-with-label {
        flex: 1;
        display: flex;
        flex-direction: column;

        .bm-rich-editor {
            flex: 1;
        }
    }

    .word-break {
        word-break: break-word;
    }

    .change-default {
        cursor: pointer;
        display: flex;
        align-items: center;
    }

    .bm-form-autocomplete-input {
        .extra-separator {
            display: none;
        }
        .list-group-item .show-aliases {
            color: $secondary-fg;
        }
        .list-group-item:hover .show-aliases {
            color: $secondary-fg-hi1;
        }
    }
}
</style>
