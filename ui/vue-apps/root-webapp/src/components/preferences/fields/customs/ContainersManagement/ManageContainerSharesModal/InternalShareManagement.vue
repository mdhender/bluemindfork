<template>
    <div class="internal-share-management">
        <bm-label-icon icon="buildings" class="h3 mb-5" :inline="false">
            {{ $t("preferences.manage_shares.inside_my_organization") }}
        </bm-label-icon>
        <template v-if="!domainRight && !Object.keys(userRights).length">
            <div class="ml-4 mt-3 font-italic">{{ noShares }}</div>
        </template>
        <template v-else>
            <bm-row v-if="domainRight" class="share-entry-body">
                <div class="share-entry-col share-user">
                    {{ $t("preferences.manage_shares.all_users_in_my_organization") }}
                </div>
                <bm-form-select
                    :value="domainRight"
                    :options="shareOptions()"
                    :auto-min-width="false"
                    right
                    class="share-entry-col"
                    @input="right => $emit('domain-right-changed', right)"
                />
            </bm-row>
            <template v-for="[user, right] of Object.entries(userRights)">
                <bm-row :key="user" class="share-entry-body">
                    <contact
                        :contact="contacts[user]"
                        class="share-entry-col share-user"
                        transparent
                        show-address
                        bold-dn
                    />
                    <bm-form-select
                        :value="right"
                        :options="shareOptions()"
                        :auto-min-width="false"
                        right
                        class="share-entry-col"
                        @input="right => $emit('user-right-changed', { user, right })"
                    />
                </bm-row>
            </template>
        </template>
    </div>
</template>

<script>
import { DirEntryAdaptor } from "@bluemind/contact";
import { Contact } from "@bluemind/business-components";
import { inject } from "@bluemind/inject";
import { BmFormSelect, BmLabelIcon, BmRow } from "@bluemind/ui-components";
import i18n from "@bluemind/i18n";
import { ContainerHelper, ContainerType } from "../container";

export default {
    name: "InternalShareManagement",
    components: { BmFormSelect, BmLabelIcon, BmRow, Contact },
    props: {
        container: { type: Object, required: true },
        domainRight: { type: Number, default: undefined },
        userRights: { type: Object, required: true }
    },
    data() {
        return { contacts: [] };
    },
    computed: {
        noShares() {
            return this.$t("preferences.manage_shares.no_acl_set", {
                name: this.container.name,
                type: this.$t("common.container_type_with_definite_article." + this.container.type)
            });
        }
    },
    watch: {
        userRights: {
            handler: async function (value) {
                this.contacts = value
                    ? (await inject("DirectoryPersistence").getMultiple(Object.keys(value))).reduce(
                          (contactByUser, dirEntry) => {
                              contactByUser[dirEntry.uid] = DirEntryAdaptor.toContact(dirEntry);
                              return contactByUser;
                          },
                          {}
                      )
                    : [];
            },
            immediate: true
        }
    },
    methods: {
        shareOptions() {
            return ContainerHelper.use(this.container.type).getOptions(i18n, this.container);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.internal-share-management {
    .share-entry-body {
        margin-bottom: $sp-2;

        .share-user {
            margin: $sp-1 0;
        }
    }
}
</style>
