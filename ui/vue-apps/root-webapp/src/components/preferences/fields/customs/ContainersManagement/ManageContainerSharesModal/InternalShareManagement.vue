<template>
    <div class="internal-share-management">
        <bm-label-icon icon="organization" icon-size="lg" class="font-weight-bold mb-1" :inline="false">
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
                    :value="domainAcl"
                    :options="shareOptions(true)"
                    :auto-min-width="false"
                    class="share-entry-col"
                    @input="onDomainAclChange"
                />
            </bm-row>
            <template v-for="dirEntry in dirEntriesAcl">
                <bm-row :key="dirEntry.uid" class="share-entry-body">
                    <bm-contact
                        :contact="dirEntryToContact(dirEntry)"
                        class="share-entry-col share-user"
                        transparent
                        show-address
                        bold-dn
                    />
                    <bm-form-select
                        :value="dirEntry.acl"
                        :options="shareOptions()"
                        :auto-min-width="false"
                        class="share-entry-col"
                        @input="value => onDirEntryAclChange(dirEntry.uid, value)"
                    />
                </bm-row>
            </template>
        </template>
    </div>
</template>

<script>
import { DirEntryAdaptor } from "@bluemind/contact";
import { inject } from "@bluemind/inject";
import { BmContact, BmFormSelect, BmLabelIcon, BmRow } from "@bluemind/styleguide";
import { ContainerHelper, ContainerType } from "../container";

export default {
    name: "InternalShareManagement",
    components: { BmContact, BmFormSelect, BmLabelIcon, BmRow },
    props: {
        container: {
            type: Object,
            required: true
        },
        domainAcl: {
            type: Number,
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
        onDirEntryAclChange(dirEntryUid, value) {
            this.$emit("dir-entry-acl-changed", { dirEntryUid, value });
        },
        onDomainAclChange(newValue) {
            this.$emit("domain-acl-changed", newValue);
        },
        shareOptions(isPlural = false) {
            const count = isPlural ? 0 : 1;
            return ContainerHelper.use(this.container.type).getOptions(inject("i18n"), count, this.isMyDefaultCalendar);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.internal-share-management {
    .share-entry-body {
        margin-bottom: $sp-2;

        .share-user {
            margin: $sp-1 0;
        }
    }
}
</style>
