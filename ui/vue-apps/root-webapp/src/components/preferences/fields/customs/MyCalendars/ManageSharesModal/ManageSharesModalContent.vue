<template>
    <div>
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
                    <bm-contact :contact="vcardInfoToContact(item)" variant="transparent" />
                    <span v-if="!item.value.source"> ({{ $t("common.external") }}) </span>
                </template>
            </template>
        </bm-form-autocomplete-input>
        <hr />
        <bm-label-icon icon="organization" icon-size="lg" class="font-weight-bold mb-1" :inline="false">
            {{ $t("preferences.calendar.my_calendars.inside_my_organization") }}
        </bm-label-icon>
        <bm-row class="align-items-center">
            <div class="col-6">{{ $t("preferences.calendar.my_calendars.all_users_in_my_organization") }}</div>
            <div class="col-6">
                <bm-form-select v-model="selectedDomainAcl_" :options="shareOptions(true)" @input="saveShares" />
            </div>
        </bm-row>
        <template v-for="dirEntry in dirEntriesAcl_">
            <bm-row :key="dirEntry.uid" class="align-items-center mt-2">
                <!-- FIXME ? group may be not displayed as user ?? -->
                <div class="col-6"><bm-contact :contact="dirEntryToContact(dirEntry)" variant="transparent" /></div>
                <div class="col-6">
                    <bm-form-select v-model="dirEntry.acl" :options="shareOptions()" @input="saveShares" />
                </div>
            </bm-row>
        </template>
        <hr />
        <bm-label-icon icon="user" icon-size="lg" class="font-weight-bold mb-2" :inline="false">
            {{ $t("preferences.calendar.my_calendars.outside_my_organization") }}
        </bm-label-icon>
        <template v-for="(external, index) in externalShares">
            <bm-row :key="external.token" class="align-items-center mt-2">
                <div class="col-6">
                    <bm-contact
                        v-if="external.vcard"
                        :contact="vcardInfoToContact(external.vcard)"
                        variant="transparent"
                    />
                    <span v-else class="font-size-lg">{{ displayedLabel(external) }}</span>
                    <div class="row mr-3 align-items-center">
                        <div class="text-secondary text-truncate col-8">{{ external.url }}</div>
                        <div class="col-4 pl-2">
                            <bm-button v-if="activeCopyBtn === index" variant="success">
                                <bm-label-icon icon="check">{{ $t("common.copied") }}</bm-label-icon>
                            </bm-button>
                            <bm-button
                                v-else
                                variant="outline-secondary"
                                @click="copyLinkInClipboard(external.url, index)"
                            >
                                <bm-label-icon icon="copy">{{ $t("common.copy") }}</bm-label-icon>
                            </bm-button>
                        </div>
                    </div>
                </div>
                <div class="col-6 d-flex">
                    <bm-form-select
                        :value="external.publishMode"
                        :options="publishModeOptions"
                        @input="editPublishMode(external)"
                    />
                    <bm-button v-if="canRemoveLink(external)" variant="inline-secondary" @click="removeLink(external)">
                        <bm-icon icon="trash" size="lg" />
                    </bm-button>
                    <bm-button v-else variant="inline-secondary" @click="regenerateLink">
                        <bm-icon icon="loop" size="lg" />
                    </bm-button>
                </div>
            </bm-row>
        </template>
        <hr />
    </div>
</template>

<script>
import throttle from "lodash.throttle";
import { CalendarAcl, calendarAclToVerb, urlToAclSubject } from "./CalendarAclHelper";
import { PublishMode } from "@bluemind/calendar.api";
import { getQuery } from "@bluemind/contact";
import { Verb } from "@bluemind/core.container.api";
import { EmailValidator } from "@bluemind/email";
import { inject } from "@bluemind/inject";
import {
    BmButton,
    BmContact,
    BmFormAutocompleteInput,
    BmFormSelect,
    BmIcon,
    BmLabelIcon,
    BmRow
} from "@bluemind/styleguide";

export default {
    name: "ManageSharesModalContent",
    components: { BmButton, BmContact, BmFormAutocompleteInput, BmFormSelect, BmIcon, BmLabelIcon, BmRow },
    props: {
        calendar: {
            type: Object,
            required: true
        },
        dirEntriesAcl: {
            type: Array,
            required: true
        },
        domainAcl: {
            type: Number,
            required: true
        },
        externalShares: {
            type: Array,
            required: true
        }
    },
    data() {
        return {
            // for search autocomplete
            searchedInput: "",
            suggestions: [],

            // inside organization
            selectedDomainAcl_: this.domainAcl,
            dirEntriesAcl_: this.dirEntriesAcl,

            // outside organization
            activeCopyBtn: -1,
            publishModeOptions: [
                {
                    text: this.$t("preferences.calendar.my_calendars.publish_link_public_mode"),
                    value: PublishMode.PUBLIC
                },
                {
                    text: this.$t("preferences.calendar.my_calendars.publish_link_private_mode"),
                    value: PublishMode.PRIVATE
                }
            ]
        };
    },
    computed: {
        isDefaultCalendar() {
            return this.calendar.uid && this.calendar.uid === "calendar:Default:" + inject("UserSession").userId;
        },
        aclReadyForServer() {
            const dirEntries = this.dirEntriesAcl_.filter(entry => entry.acl !== CalendarAcl.CANT_INVITE_ME);
            const res = dirEntries.map(entry => ({ subject: entry.uid, verb: calendarAclToVerb(entry.acl) }));
            if (this.selectedDomainAcl_ !== CalendarAcl.CANT_INVITE_ME) {
                res.push({ subject: inject("UserSession").domain, verb: calendarAclToVerb(this.selectedDomainAcl_) });
            }
            const externalSharesAcl = this.externalShares.map(share => ({
                subject: urlToAclSubject(share),
                verb: Verb.Read
            }));
            res.push(...externalSharesAcl);
            return res;
        },
        freebusyAclReadyForServer() {
            const dirEntries = this.dirEntriesAcl_.filter(entry => entry.acl >= CalendarAcl.CAN_SEE_MY_AVAILABILITY);
            const res = dirEntries.map(entry => ({ subject: entry.uid, verb: calendarAclToVerb(entry.acl, true) }));
            if (this.selectedDomainAcl_ >= CalendarAcl.CAN_SEE_MY_AVAILABILITY) {
                res.push({
                    subject: inject("UserSession").domain,
                    verb: calendarAclToVerb(this.selectedDomainAcl_, true)
                });
            }
            return res;
        }
    },
    methods: {
        shareOptions(isPlural = false) {
            const count = isPlural ? 0 : 1;
            const options = [
                {
                    text: this.$tc("preferences.calendar.my_calendars.cant_invite_me_to_a_meeting", count),
                    value: CalendarAcl.CANT_INVITE_ME
                },
                {
                    text: this.$tc("preferences.calendar.my_calendars.can_invite_me_to_a_meeting", count),
                    value: CalendarAcl.CAN_INVITE_ME
                },
                {
                    text: this.$tc(
                        "preferences.calendar.my_calendars.can_invite_me_to_a_meeting_and_see_my_events",
                        count
                    ),
                    value: CalendarAcl.CAN_SEE_MY_EVENTS
                },
                {
                    text: this.$tc("preferences.calendar.my_calendars.can_edit_my_events", count),
                    value: CalendarAcl.CAN_EDIT_MY_EVENTS
                },
                {
                    text: this.$tc("preferences.calendar.my_calendars.can_edit_my_events_and_manage_shares", count),
                    value: CalendarAcl.CAN_MANAGE_SHARES
                }
            ];
            if (this.isDefaultCalendar) {
                options.splice(2, 0, {
                    text: this.$tc(
                        "preferences.calendar.my_calendars.can_invite_me_to_a_meeting_and_see_my_availability",
                        count
                    ),
                    value: CalendarAcl.CAN_SEE_MY_AVAILABILITY
                });
            }
            return options;
        },
        dirEntryToContact(entry) {
            return { address: entry.value.email, dn: entry.value.displayName };
        },
        vcardInfoToContact(vcard) {
            return { address: vcard.value.mail, dn: vcard.value.formatedName };
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
            this.suggestions = vcards.values.filter(
                vcard =>
                    !this.dirEntriesAcl_.find(alreadyInList => alreadyInList.uid === vcard.uid) &&
                    vcard.uid !== userSession.userId &&
                    !this.externalShares.find(share => share.vcard?.uid === vcard.uid)
            );

            if (
                !this.searchedInput.endsWith(userSession.domain) &&
                EmailValidator.validateAddress(this.searchedInput)
            ) {
                this.suggestions.push({ value: { mail: this.searchedInput }, isNewContact: true });
            }
        }, 500),
        onSelect(selected) {
            if (selected.isNewContact) {
                this.$emit("add-new-external", selected);
            } else if (!selected.value.source) {
                this.$emit("add-external", selected);
            } else {
                const newDirEntry = vcardToDirEntry(selected);
                this.dirEntriesAcl_.push({ ...newDirEntry, acl: CalendarAcl.CAN_INVITE_ME });
                this.saveShares();
            }
            this.suggestions = [];
            this.searchedInput = "";
        },

        // select listener
        async saveShares() {
            await inject("ContainerManagementPersistence", this.calendar.uid).setAccessControlList(
                this.aclReadyForServer
            );
            if (this.isDefaultCalendar) {
                await inject("ContainerManagementPersistence", "freebusy:" + this.calendar.owner).setAccessControlList(
                    this.freebusyAclReadyForServer
                );
            }
        },

        // outside organization
        canRemoveLink() {
            // not implemented yet
            // later, every user will have a default 'Public' link that you wont be able to remove but only regenerate its link
            return true;
        },
        regenerateLink() {
            // not implemented yet, code will be close to editPublishMode method (ManageSharesModal component)
        },
        removeLink(external) {
            this.$emit("remove-external", external.token);
        },
        copyLinkInClipboard(url, index) {
            navigator.clipboard.writeText(url);
            this.activeCopyBtn = index;
        },
        displayedLabel({ publishMode }) {
            return publishMode === PublishMode.PUBLIC ? this.$t("common.public") : this.$t("common.private");
        },
        editPublishMode(external) {
            this.activeCopyBtn = -1;
            this.$emit("edit-publish-mode", external);
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
