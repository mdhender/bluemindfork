<template>
    <bm-spinner v-if="isLoading" :size="2" class="d-flex justify-content-center" />
    <div v-else>
        <div class="mb-1">{{ $t("common.share_with") }}</div>
        <bm-form-autocomplete-input
            v-model="searchedInput"
            :placeholder="$t('common.search')"
            class="w-50"
            icon="search"
            left-icon
            :items="suggestions"
            @input="onInputUpdate"
            @selected="onSelect"
        >
            <template v-slot="{ item }">
                <span v-if="item.isNewContact">{{ item.value.mail }} ({{ $t("common.external") }})</span>
                <template v-else>
                    <bm-contact :contact="VCardInfoAdaptor.toContact(item)" variant="transparent" />
                    <span v-if="!item.value.source"> ({{ $t("common.external") }}) </span>
                </template>
            </template>
        </bm-form-autocomplete-input>
        <hr />
        <internal-share-management
            :container="container"
            :domain-acl="domainAcl"
            :dir-entries-acl="dirEntriesAcl"
            @dir-entry-acl-changed="onDirEntryAclChange"
            @domain-acl-changed="onDomainAclChange"
        />
        <template v-if="isCalendarType">
            <external-share-management
                :container="container"
                :external-shares="externalShares"
                @remove="removeExternal"
                @publish-mode-change="editPublishMode"
            />
        </template>
        <hr />
    </div>
</template>

<script>
import ExternalShareManagement from "./ExternalShareManagement";
import InternalShareManagement from "./InternalShareManagement";
import { aclToVerb, defaultDirEntryAcl, loadAcl, noRightAcl } from "./helpers/ContainerShareHelper";
import { loadCalendarUrls, sendExternalToServer, urlToAclSubject } from "./helpers/ExternalShareHelper";
import { ContainerType } from "../container";
import { PublishMode } from "@bluemind/calendar.api";
import { getQuery } from "@bluemind/contact";
import { Verb } from "@bluemind/core.container.api";
import { VCardInfoAdaptor } from "@bluemind/contact";
import { EmailValidator } from "@bluemind/email";
import { inject } from "@bluemind/inject";
import { BmContact, BmFormAutocompleteInput, BmSpinner } from "@bluemind/styleguide";
import UUIDHelper from "@bluemind/uuid";

import throttle from "lodash.throttle";

export default {
    name: "ShareManagement",
    components: { BmContact, BmFormAutocompleteInput, BmSpinner, ExternalShareManagement, InternalShareManagement },
    props: {
        container: {
            type: Object,
            required: true
        },
        isMyContainer: {
            type: Boolean,
            required: true
        },
        isMyDefaultCalendar: {
            type: Boolean,
            required: true
        }
    },
    data() {
        return {
            VCardInfoAdaptor,
            isLoading: true,

            // search autocomplete
            searchedInput: "",
            suggestions: [],

            // inside organization
            dirEntriesAcl: [],
            domainAcl: -1,

            // outside organization
            externalShares: []
        };
    },
    computed: {
        isCalendarType() {
            return this.container.type === ContainerType.CALENDAR;
        },
        isMailboxType() {
            return this.container.type === ContainerType.MAILBOX;
        },
        aclReadyForServer() {
            const dirEntries = this.dirEntriesAcl.filter(this.filterNoRightsAcl);
            const res = dirEntries.map(entry => ({
                subject: entry.uid,
                verb: aclToVerb(this.container.type, entry.acl)
            }));
            if (!this.isMyContainer) {
                res.push({ subject: inject("UserSession").userId, verb: Verb.All });
            }

            if (!this.isMailboxType && this.domainAcl !== noRightAcl(this.container.type)) {
                res.push({
                    subject: inject("UserSession").domain,
                    verb: aclToVerb(this.container.type, this.domainAcl)
                });
            }
            if (this.isCalendarType) {
                const externalSharesAcl = this.externalShares.map(share => ({
                    subject: urlToAclSubject(share),
                    verb: Verb.Read
                }));
                res.push(...externalSharesAcl);
            }
            return res;
        },
        freebusyAclReadyForServer() {
            let res = [];
            if (this.isCalendarType) {
                const dirEntries = this.dirEntriesAcl.filter(this.filterNoRightsAcl);
                res = dirEntries.map(entry => ({
                    subject: entry.uid,
                    verb: aclToVerb(this.container.type, entry.acl, true)
                }));
                if (this.domainAcl !== noRightAcl(this.container.type)) {
                    res.push({
                        subject: inject("UserSession").domain,
                        verb: aclToVerb(this.container.type, this.domainAcl, true)
                    });
                }
            }
            return res;
        }
    },
    async created() {
        this.isLoading = true;
        this.searchedInput = "";
        const acl = await loadAcl(this.container, this.isMyDefaultCalendar);
        this.domainAcl = acl.domainAcl;
        this.dirEntriesAcl = acl.dirEntriesAcl;

        if (this.container.type === ContainerType.CALENDAR) {
            this.externalShares = await loadCalendarUrls(this.container.uid);
        }
        this.isLoading = false;
    },
    methods: {
        filterNoRightsAcl(entry) {
            return entry.acl !== noRightAcl(this.container.type);
        },

        // search autocomplete
        onInputUpdate: throttle(async function () {
            if (!this.searchedInput) {
                this.suggestions = [];
                return;
            }
            const userSession = inject("UserSession");
            const vcards = await inject("AddressBooksPersistence").search({
                size: 10,
                query: getQuery(this.searchedInput)
            });
            this.suggestions = vcards.values.filter(vcard => {
                if (!this.isCalendarType && vcard.value.source === null) {
                    return false;
                }
                return (
                    !this.dirEntriesAcl.find(alreadyInList => alreadyInList.uid === vcard.uid) &&
                    vcard.uid !== userSession.userId &&
                    !this.externalShares.find(share => share.vcard?.uid === vcard.uid)
                );
            });

            if (
                this.isCalendarType &&
                !this.searchedInput.endsWith(userSession.domain) &&
                EmailValidator.validateAddress(this.searchedInput)
            ) {
                this.suggestions.push({ value: { mail: this.searchedInput }, isNewContact: true });
            }
        }, 500),
        onSelect(selected) {
            if (selected.isNewContact) {
                this.createContactAndAddExternal(selected);
            } else if (!selected.value.source) {
                this.addExternal(selected);
            } else {
                const newDirEntry = vcardToDirEntry(selected);
                this.dirEntriesAcl.push({ ...newDirEntry, acl: defaultDirEntryAcl(this.container.type) });
                this.saveShares();
            }
            this.suggestions = [];
            this.searchedInput = "";
        },

        // select listener
        onDirEntryAclChange({ dirEntryUid, value }) {
            const index = this.dirEntriesAcl.findIndex(entry => entry.uid === dirEntryUid);
            if (index !== -1) {
                this.dirEntriesAcl[index].acl = value;
                this.saveShares();
            }
        },
        onDomainAclChange(value) {
            this.domainAcl = value;
            this.saveShares();
        },
        async saveShares() {
            await inject("ContainerManagementPersistence", this.container.uid).setAccessControlList(
                this.aclReadyForServer
            );
            if (this.isMyDefaultCalendar) {
                await inject("ContainerManagementPersistence", "freebusy:" + this.container.owner).setAccessControlList(
                    this.freebusyAclReadyForServer
                );
            }
        },

        // external share
        removeExternal(externalToken) {
            const index = this.externalShares.findIndex(share => share.token === externalToken);
            if (index !== -1) {
                // FIXME: problem with axios, need header Content-Type: text/plain for this method
                inject("PublishCalendarPersistence", this.container.uid).disableUrl(
                    '"' + this.externalShares[index].url + '"'
                );
                this.externalShares.splice(index, 1);
            }
        },
        async editPublishMode(externalShare) {
            const index = this.externalShares.findIndex(share => share.token === externalShare.token);
            if (index !== -1) {
                const updatedShare = this.externalShares[index];
                const publishMode =
                    externalShare.publishMode === PublishMode.PUBLIC ? PublishMode.PRIVATE : PublishMode.PUBLIC;
                updatedShare.publishMode = publishMode;
                const newUrl = await sendExternalToServer(publishMode, externalShare.token, this.container.uid);
                const oldUrl = externalShare.url;
                updatedShare.url = newUrl;
                // FIXME: problem with axios, need header Content-Type: text/plain for this method
                inject("PublishCalendarPersistence", this.container.uid).disableUrl('"' + oldUrl + '"');
            }
        },
        async addExternal(vcardInfo) {
            const publishMode = PublishMode.PUBLIC;
            const newUrl = await sendExternalToServer(publishMode, vcardInfo.uid, this.container.uid);
            this.externalShares.push({ publishMode, url: newUrl, vcard: vcardInfo, token: vcardInfo.uid });
        },
        async createContactAndAddExternal(vcardInfo) {
            const vcardUid = UUIDHelper.generate();
            vcardInfo.uid = vcardUid;
            const collectedContactsUid = "book:CollectedContacts_" + inject("UserSession").userId;
            await inject("AddressBookPersistence", collectedContactsUid).create(vcardUid, {
                communications: { emails: [{ value: vcardInfo.value.mail }] }
            });
            this.addExternal(vcardInfo);
        }
    }
};

function vcardToDirEntry(vcard) {
    return {
        uid: vcard.uid,
        value: {
            displayName: vcard.value.formatedName,
            email: vcard.value.mail
        }
    };
}
</script>
