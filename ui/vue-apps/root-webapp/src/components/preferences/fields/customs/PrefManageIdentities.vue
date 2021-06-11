<template>
    <div class="pref-manage-identities">
        <!-- FIXME: uncomment this when doing https://forge.bluemind.net/jira/browse/FEATWEBML-101
        <bm-form-checkbox v-model="localUserSettings[setting]" class="mb-4" value="true" unchecked-value="false">
            {{ $t("preferences.mail.identities.always_show_from") }}
        </bm-form-checkbox> -->
        <div class="row mb-1 px-2 text-secondary">
            <bm-col cols="1" />
            <bm-col cols="6">{{ $t("common.identity") }}</bm-col>
            <bm-col cols="3">{{ $t("common.label") }}</bm-col>
        </div>
        <bm-list-group class="border-top border-bottom">
            <bm-list-group-item
                v-for="(identity, index) in identities"
                :key="identity.id"
                class="row d-flex align-items-center"
                :class="{ 'bg-extra-light': index % 2 === 0 }"
            >
                <bm-col cols="1" class="d-flex justify-content-center">
                    <bm-icon v-if="identity.isDefault" icon="star-fill" size="lg" />
                </bm-col>
                <bm-col cols="6">
                    <bm-contact :contact="getContact(identity)" display-full variant="transparent" />
                </bm-col>
                <bm-col cols="3">{{ identity.name }}</bm-col>
                <bm-col cols="2" class="d-flex justify-content-center">
                    <bm-button variant="outline-secondary" @click="openModal(identity)">
                        {{ $t("common.manage") }}
                    </bm-button>
                </bm-col>
            </bm-list-group-item>
        </bm-list-group>
        <bm-button variant="outline-secondary" class="my-3" @click="openModal()">
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
import { BmButton, BmCol, BmContact, BmIcon, BmListGroup, BmListGroupItem } from "@bluemind/styleguide";

import ManageIdentityModal from "./ManageIdentityModal";
import PrefFieldMixin from "../../mixins/PrefFieldMixin";

export default {
    name: "PrefManageIdentities",
    components: {
        BmButton,
        BmCol,
        BmContact,
        BmIcon,
        BmListGroup,
        BmListGroupItem,
        ManageIdentityModal
    },
    mixins: [PrefFieldMixin],
    data() {
        return { possibleIdentities: [], possibleIdentitiesStatus: "NOT-LOADED" };
    },
    computed: {
        ...mapState("root-app", ["identities"])
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
    .list-group-item {
        border-bottom: none !important;
    }

    .fa-star-fill {
        color: $primary;
    }
}
</style>
