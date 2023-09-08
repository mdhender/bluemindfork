<template>
    <div class="internal-share-management">
        <bm-label-icon icon="buildings" class="h3 mb-5" :inline="false">
            {{ $t("preferences.manage_shares.inside_my_organization") }}
        </bm-label-icon>
        <template v-if="domainAcl === -1 && dirEntriesAcl.length === 0">
            <div class="ml-4 mt-3 font-italic">{{ noAclSet }}</div>
        </template>
        <template v-else>
            <bm-row v-if="!isMailboxType" class="share-entry-body">
                <div class="share-entry-col share-user">
                    {{ $t("preferences.manage_shares.all_users_in_my_organization") }}
                </div>
                <bm-form-select
                    :value="aclToOption(domainAcl)"
                    :options="shareOptions(true)"
                    :auto-min-width="false"
                    right
                    class="share-entry-col"
                    @input="value => onDomainAclChange(domainAcl, domainUid, value)"
                />
            </bm-row>
            <template v-for="dirEntry in dirEntriesAcl">
                <bm-row :key="dirEntry.uid" class="share-entry-body">
                    <contact
                        :contact="dirEntryToContact(dirEntry)"
                        class="share-entry-col share-user"
                        transparent
                        show-address
                        bold-dn
                    />
                    <bm-form-select
                        :value="aclToOption(dirEntry.acl)"
                        :options="shareOptions()"
                        :auto-min-width="false"
                        right
                        class="share-entry-col"
                        @input="value => onDirEntryAclChange(dirEntry.acl, dirEntry.uid, value)"
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
        container: {
            type: Object,
            required: true
        },
        domainAcl: {
            type: Array,
            required: true
        },
        dirEntriesAcl: {
            type: Array,
            required: true
        },
        isMyDefaultCalendar: {
            type: Boolean,
            required: true
        }
    },
    data() {
        return { domainUid: inject("UserSession").domain };
    },
    computed: {
        noAclSet() {
            return this.$t("preferences.manage_shares.no_acl_set", {
                name: this.container.name,
                type: this.$t("common.container_type_with_definite_article." + this.container.type)
            });
        },
        isMailboxType() {
            return this.container.type === ContainerType.MAILBOX;
        }
    },
    methods: {
        dirEntryToContact: DirEntryAdaptor.toContact,
        onDirEntryAclChange(acl, dirEntryUid, value) {
            this.$emit("dir-entry-acl-changed", { dirEntryUid, value: this.aclFromOption(acl, dirEntryUid, value) });
        },
        onDomainAclChange(domainAcl, domainUid, value) {
            this.$emit("domain-acl-changed", this.aclFromOption(domainAcl, domainUid, value));
        },
        shareOptions(isPlural = false) {
            const count = isPlural ? 0 : 1;
            return ContainerHelper.use(this.container.type).getOptions(i18n, count, this.isMyDefaultCalendar);
        },
        aclToOption(acl) {
            return ContainerHelper.use(this.container.type).aclToOption(acl);
        },
        aclFromOption(acl, uid, option) {
            return ContainerHelper.use(this.container.type).updateAcl(acl, uid, option);
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
