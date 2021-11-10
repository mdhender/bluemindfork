<template>
    <div>
        <bm-label-icon icon="organization" icon-size="lg" class="font-weight-bold mb-1" :inline="false">
            {{ $t("preferences.manage_shares.inside_my_organization") }}
        </bm-label-icon>
        <template v-if="domainAcl === -1 && dirEntriesAcl.length === 0">
            <div class="ml-4 mt-3 font-italic">{{ noAclSet }}</div>
        </template>
        <template v-else>
            <bm-row v-if="!isMailboxType" class="align-items-center">
                <div class="col-6">{{ $t("preferences.manage_shares.all_users_in_my_organization") }}</div>
                <div class="col-6">
                    <bm-form-select :value="domainAcl" :options="shareOptions(true)" @input="onDomainAclChange" />
                </div>
            </bm-row>
            <template v-for="dirEntry in dirEntriesAcl">
                <bm-row :key="dirEntry.uid" class="align-items-center mt-2">
                    <!-- FIXME: group may not be displayed as user -->
                    <div class="col-6"><bm-contact :contact="dirEntryToContact(dirEntry)" variant="transparent" /></div>
                    <div class="col-6">
                        <bm-form-select
                            :value="dirEntry.acl"
                            :options="shareOptions()"
                            @input="value => onDirEntryAclChange(dirEntry.uid, value)"
                        />
                    </div>
                </bm-row>
            </template>
        </template>
    </div>
</template>

<script>
import { ContainerType } from "../container";
import { getOptions } from "./helpers/ContainerShareHelper";
import { inject } from "@bluemind/inject";
import { BmContact, BmFormSelect, BmLabelIcon, BmRow } from "@bluemind/styleguide";

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
        dirEntryToContact(entry) {
            return { address: entry.value.email, dn: entry.value.displayName };
        },
        onDirEntryAclChange(dirEntryUid, value) {
            this.$emit("dir-entry-acl-changed", { dirEntryUid, value });
        },
        onDomainAclChange(newValue) {
            this.$emit("domain-acl-changed", newValue);
        },
        shareOptions(isPlural = false) {
            const count = isPlural ? 0 : 1;
            return getOptions(this.container.type, count, inject("i18n"), this.isMyDefaultCalendar);
        }
    }
};
</script>
