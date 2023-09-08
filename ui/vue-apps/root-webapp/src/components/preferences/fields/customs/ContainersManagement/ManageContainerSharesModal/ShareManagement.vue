<template>
    <bm-spinner v-if="isLoading" class="d-flex justify-content-center" />
    <div v-else class="share-management">
        <bm-form-group class="section">
            <label for="share-management-search-input" class="mb-1">{{ $t("common.share_with") }}</label>
            <bm-form-autocomplete-input
                id="share-management-search-input"
                v-model="searchedInput"
                :placeholder="$t('common.search')"
                class="search-input"
                icon="search"
                left-icon
                :items="suggestions"
                @input="onInputUpdate"
                @selected="onSelect"
            >
                <template #default="{ item }">
                    <contact :contact="item" transparent show-address bold-dn />
                    <span v-if="!item.urn"> ({{ $t("common.external") }}) </span>
                </template>
            </bm-form-autocomplete-input>
        </bm-form-group>
        <hr />
        <internal-share-management
            class="section"
            :container="container"
            :domain-acl="domainAcl"
            :dir-entries-acl="dirEntriesAcl"
            :is-my-default-calendar="isMyDefaultCalendar"
            @dir-entry-acl-changed="onDirEntryAclChange"
            @domain-acl-changed="onDomainAclChange"
        />
        <template v-if="isCalendarType">
            <hr />
            <external-share-management
                class="section"
                :container="container"
                :external-shares="externalShares"
                @remove="removeExternal"
                @publish-mode-change="editPublishMode"
            />
        </template>
    </div>
</template>

<script>
import { mapActions } from "vuex";
import throttle from "lodash.throttle";
import ExternalShareManagement from "./ExternalShareManagement";
import InternalShareManagement from "./InternalShareManagement";
import { loadAcl } from "./ContainerShareHelper";
import { loadCalendarUrls, sendExternalToServer, urlToAclSubject } from "./ExternalShareHelper";
import { ContainerHelper, ContainerType } from "../container";
import { PublishMode } from "@bluemind/calendar.api";
import { searchVCardsHelper, DirEntryAdaptor, VCardInfoAdaptor, VCardAdaptor } from "@bluemind/contact";
import { Verb } from "@bluemind/core.container.api";
import { BaseDirEntry } from "@bluemind/directory.api";
import { EmailValidator } from "@bluemind/email";
import { inject } from "@bluemind/inject";
import { BmFormGroup, BmFormAutocompleteInput, BmSpinner } from "@bluemind/ui-components";
import { Contact } from "@bluemind/business-components";
import UUIDHelper from "@bluemind/uuid";
import { SUCCESS } from "@bluemind/alert.store";
import { SAVE_ALERT_MODAL } from "../../../../Alerts/defaultAlerts";

export default {
    name: "ShareManagement",
    components: {
        BmFormGroup,
        BmFormAutocompleteInput,
        BmSpinner,
        Contact,
        ExternalShareManagement,
        InternalShareManagement
    },
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
            domainAcl: [],

            // outside organization
            externalShares: []
        };
    },
    computed: {
        helper() {
            return ContainerHelper.use(this.container.type);
        },
        isCalendarType() {
            return this.container.type === ContainerType.CALENDAR;
        },
        isMailboxType() {
            return this.container.type === ContainerType.MAILBOX;
        },
        aclReadyForServer() {
            const res = this.dirEntriesAcl.flatMap(({ acl }) => acl);
            if (!this.isMyContainer) {
                res.push({ subject: inject("UserSession").userId, verb: Verb.All });
            }

            if (!this.isMailboxType && this.domainAcl?.length) {
                res.push(...this.domainAcl);
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
                res.push(...this.dirEntriesAcl.flatMap(({ acl }) => acl));
                if (this.domainAcl.length) {
                    res.push(...this.domainAcl);
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
        ...mapActions("alert", { SUCCESS }),

        // search autocomplete
        onInputUpdate: throttle(async function () {
            if (!this.searchedInput) {
                this.suggestions = [];
                return;
            }
            const directorySuggestions = await this.loadDirectorySuggestions();
            if (this.isCalendarType) {
                const addressbooksSuggestions = await this.loadAddressbooksSuggestions();
                this.suggestions = this.mergeSuggestions(directorySuggestions, addressbooksSuggestions);
            } else {
                this.suggestions = directorySuggestions;
            }
        }, 500),
        async loadDirectorySuggestions() {
            const dirEntries = await inject("DirectoryPersistence").search({
                nameOrEmailFilter: this.searchedInput,
                kindsFilter: [BaseDirEntry.Kind.USER, BaseDirEntry.Kind.GROUP],
                size: 10
            });
            return dirEntries.values.filter(this.filterSearchResults).map(DirEntryAdaptor.toContact);
        },
        async loadAddressbooksSuggestions() {
            const vcards = await inject("AddressBooksPersistence").search(searchVCardsHelper(this.searchedInput, 10));
            const filtered = vcards.values
                .filter(
                    vcardInfo =>
                        !vcardInfo.value.source ||
                        vcardInfo.value.source.includes("/users/") ||
                        vcardInfo.value.source.includes("/groups/")
                )
                .filter(this.filterSearchResults)
                .map(VCardInfoAdaptor.toContact);
            if (
                !this.searchedInput.endsWith(inject("UserSession").domain) &&
                EmailValidator.validateAddress(this.searchedInput)
            ) {
                filtered.push({ address: this.searchedInput, isNewContact: true });
            }
            return filtered;
        },
        mergeSuggestions(directorySuggestions, addressbooksSuggestions) {
            return directorySuggestions.concat(
                addressbooksSuggestions.filter(
                    suggestion => directorySuggestions.findIndex(dir => dir.uid === suggestion.uid) === -1
                )
            );
        },
        filterSearchResults({ uid }) {
            return (
                !this.dirEntriesAcl.find(alreadyInList => alreadyInList.uid === uid) &&
                uid !== inject("UserSession").userId &&
                !this.externalShares.find(share => share.vcard?.uid === uid)
            );
        },
        onSelect(selected) {
            if (selected.isNewContact) {
                this.createContactAndAddExternal(selected);
            } else if (!selected.isInternal) {
                this.addExternal(selected);
            } else {
                const dirEntry = DirEntryAdaptor.toDirEntry(selected);
                this.dirEntriesAcl.push({
                    ...dirEntry,
                    acl: this.helper.buildDefaultDirEntryAcl(dirEntry)
                });
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
            this.SUCCESS(SAVE_ALERT_MODAL);
        },

        // external share
        removeExternal(externalToken) {
            const index = this.externalShares.findIndex(share => share.token === externalToken);
            if (index !== -1) {
                inject("PublishCalendarPersistence", this.container.uid).disableUrl(this.externalShares[index].url);
                this.externalShares.splice(index, 1);
                this.SUCCESS(SAVE_ALERT_MODAL);
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
                inject("PublishCalendarPersistence", this.container.uid).disableUrl(oldUrl);
                this.SUCCESS(SAVE_ALERT_MODAL);
            }
        },
        async addExternal(contact) {
            const vCardInfo = VCardInfoAdaptor.toVCardInfo(contact);
            const publishMode = PublishMode.PUBLIC;
            const newUrl = await sendExternalToServer(publishMode, vCardInfo.uid, this.container.uid);
            this.externalShares.push({ publishMode, url: newUrl, vcard: vCardInfo, token: vCardInfo.uid });
            this.SUCCESS(SAVE_ALERT_MODAL);
        },
        async createContactAndAddExternal(contact) {
            const vcardUid = UUIDHelper.generate();
            contact.uid = vcardUid;
            const collectedContactsUid = "book:CollectedContacts_" + inject("UserSession").userId;
            const vCard = VCardAdaptor.toVCard(contact);
            await inject("AddressBookPersistence", collectedContactsUid).create(vcardUid, vCard);
            this.addExternal(contact);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";

.share-management {
    .section {
        margin: 0 $sp-5;
        @include from-lg {
            margin: 0 $sp-6;
        }
    }

    #share-management-search-input .bm-form-input {
        max-width: base-px-to-rem(300);
    }

    .share-entry-body {
        display: flex;
        align-items: center;
        gap: 0 $sp-3;

        .share-entry-col {
            flex: none;
            width: 100%;
            min-width: 0;
            @include from-lg {
                flex: 1;
            }
        }
    }
    hr {
        margin-top: $sp-6;
        margin-bottom: $sp-6;
        border-top: 1px solid $neutral-fg-lo3;
    }
}
</style>
