<template>
    <div class="pref-manage-identities">
        <!-- FIXME: uncomment this when doing https://forge.bluemind.net/jira/browse/FEATWEBML-101
        <bm-form-checkbox v-model="localUserSettings[setting]" class="mb-4" value="true" unchecked-value="false">
            {{ $t("preferences.mail.identities.always_show_from") }}
        </bm-form-checkbox> -->
        <bm-table
            :items="identities"
            :fields="fields"
            :per-page="perPage"
            :current-page="currentPage"
            sort-by="isDefault"
            sort-desc
        >
            <template #cell(isDefault)="row">
                <div :title="$t('preferences.mail.identities.default')">
                    <bm-icon v-if="row.value" icon="star-fill" size="lg" />
                </div>
            </template>
            <template #cell(displayname)="row">
                <bm-contact :contact="getContact(row.item)" display-full variant="transparent" />
            </template>
            <template #cell(name)="row">{{ row.value }}</template>
            <template #cell(action)="row">
                <bm-button variant="outline-secondary" @click="openModal(row.item)">
                    {{ $t("common.manage") }}
                </bm-button>
            </template>
        </bm-table>
        <bm-pagination v-model="currentPage" :total-rows="totalRows" :per-page="perPage" class="d-inline-flex" />
        <bm-button variant="outline-secondary" class="float-right" @click="openModal()">
            {{ $t("preferences.mail.identities.create") }}
        </bm-button>
        <manage-identity-modal
            ref="manage-identity"
            :possible-identities="possibleIdentities"
            :possible-identities-status="possibleIdentitiesStatus"
        />
    </div>
</template>

<script>
import { mapState } from "vuex";
import { inject } from "@bluemind/inject";
import { BmButton, BmContact, BmIcon, BmPagination, BmTable } from "@bluemind/styleguide";

import ManageIdentityModal from "./ManageIdentityModal";
import BaseField from "../../mixins/BaseField";

export default {
    name: "PrefManageIdentities",
    components: {
        BmButton,
        BmContact,
        BmIcon,
        BmPagination,
        BmTable,
        ManageIdentityModal
    },
    mixins: [BaseField],
    data() {
        return {
            possibleIdentities: [],
            possibleIdentitiesStatus: "NOT-LOADED",

            currentPage: 1,
            perPage: 5,
            fields: [
                {
                    key: "isDefault",
                    headerTitle: this.$t("preferences.mail.identities.default"),
                    label: "",
                    class: "text-center align-middle"
                },
                {
                    key: "displayname",
                    label: this.$t("common.identity")
                },
                {
                    key: "name",
                    label: this.$t("common.label")
                },
                {
                    key: "action",
                    headerTitle: this.$t("common.action"),
                    label: "",
                    class: "text-right"
                }
            ]
        };
    },
    computed: {
        ...mapState("root-app", ["identities"]),
        totalRows() {
            return this.identities.length;
        }
    },
    methods: {
        getContact(identity) {
            return {
                address: identity.email,
                dn: identity.displayname
            };
        },
        async openModal(identity) {
            this.$refs["manage-identity"].open(identity);
            if (this.possibleIdentitiesStatus !== "LOADED") {
                this.possibleIdentitiesStatus = "LOADING";
                try {
                    this.possibleIdentities = await inject("UserMailIdentitiesPersistence").getAvailableIdentities();
                    this.possibleIdentitiesStatus = "LOADED";
                } catch {
                    this.possibleIdentitiesStatus = "ERROR";
                }
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.pref-manage-identities {
    .fa-star-fill {
        color: $primary;
    }
}
</style>
