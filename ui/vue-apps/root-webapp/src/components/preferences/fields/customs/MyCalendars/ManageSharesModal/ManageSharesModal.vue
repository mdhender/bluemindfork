<template>
    <bm-modal
        v-model="show"
        centered
        modal-class="manage-shares-modal"
        :hide-footer="isLoading || showAvailabilitiesAdvancedManagement || !isDefaultCalendar"
    >
        <template #modal-title>
            <h1 v-if="showAvailabilitiesAdvancedManagement" class="modal-title">
                <bm-button variant="inline" @click="back()">
                    <bm-icon icon="arrow-back" size="2x" />
                </bm-button>
                {{ $t("preferences.calendar.my_calendars.availabilities_advanced_management") }}
            </h1>
            <h1 v-else class="modal-title">
                {{ $t("preferences.calendar.my_calendars.manage_shares", { calendarName: calendar.name }) }}
            </h1>
        </template>
        <template v-if="isLoading">
            <bm-spinner :size="2" class="d-flex justify-content-center" />
        </template>
        <template v-else-if="showAvailabilitiesAdvancedManagement">
            <availabilities-advanced-management />
        </template>
        <template v-else>
            <manage-shares-modal-content
                :calendar="calendar"
                :dir-entries-acl="dirEntriesAcl"
                :domain-acl="domainAcl"
                :external-shares="externalShares"
                @add-external="addExternal"
                @add-new-external="createContactAndAddExternal"
                @edit-publish-mode="editPublishMode"
                @remove-external="removeExternal"
            />
        </template>
        <template #modal-footer>
            <bm-button
                v-if="!showAvailabilitiesAdvancedManagement"
                variant="simple-secondary"
                @click="showAvailabilitiesAdvancedManagement = true"
            >
                {{ $t("preferences.calendar.my_calendars.availabilities_advanced_management") }}
            </bm-button>
        </template>
    </bm-modal>
</template>

<script>
import { PublishMode } from "@bluemind/calendar.api";
import { inject } from "@bluemind/inject";
import { BmButton, BmIcon, BmModal, BmSpinner } from "@bluemind/styleguide";
import UUIDHelper from "@bluemind/uuid";
import AvailabilitiesAdvancedManagement from "./AvailabilitiesAdvancedManagement";
import { CalendarAcl, loadAcl } from "./CalendarAclHelper";
import { loadCalendarUrls, sendExternalToServer } from "./ExternalShareHelper";
import ManageSharesModalContent from "./ManageSharesModalContent";

export default {
    name: "ManageSharesModal",
    components: {
        AvailabilitiesAdvancedManagement,
        BmButton,
        BmIcon,
        BmModal,
        BmSpinner,
        ManageSharesModalContent
    },
    data() {
        return {
            isLoading: true,
            show: false,
            showAvailabilitiesAdvancedManagement: false,
            calendar: {},

            dirEntriesAcl: [],
            domainAcl: CalendarAcl.CANT_INVITE_ME,
            externalShares: []
        };
    },
    computed: {
        isDefaultCalendar() {
            return (
                this.calendar &&
                this.calendar.uid &&
                this.calendar.uid === "calendar:Default:" + inject("UserSession").userId
            );
        }
    },
    methods: {
        async open(calendar) {
            this.isLoading = true;
            this.show = true;
            this.showAvailabilitiesAdvancedManagement = false;

            this.calendar = calendar;
            this.searchedInput = "";

            const acl = await loadAcl(this.calendar, this.isDefaultCalendar);
            this.domainAcl = acl.domainAcl;
            this.dirEntriesAcl = acl.dirEntriesAcl;

            this.externalShares = await loadCalendarUrls(this.calendar.uid);

            this.isLoading = false;
        },
        back() {
            this.showAvailabilitiesAdvancedManagement = false;
        },
        removeExternal(externalToken) {
            const index = this.externalShares.findIndex(share => share.token === externalToken);
            if (index !== -1) {
                // FIXME: problem with axios, need header Content-Type: text/plain for this method
                inject("PublishCalendarPersistence", this.calendar.uid).disableUrl(
                    '"' + this.externalShares[index].url + '"'
                );
                this.externalShares.splice(index, 1);
            }
        },
        async editPublishMode(externalShare) {
            const index = this.externalShares.findIndex(share => share.token === externalShare.token);
            if (index !== -1) {
                const publishMode =
                    externalShare.publishMode === PublishMode.PUBLIC ? PublishMode.PRIVATE : PublishMode.PUBLIC;
                externalShare.publishMode = publishMode;
                const newUrl = await sendExternalToServer(publishMode, externalShare.token, this.calendar.uid);
                const oldUrl = externalShare.url;
                externalShare.url = newUrl;
                // FIXME: problem with axios, need header Content-Type: text/plain for this method
                inject("PublishCalendarPersistence", this.calendar.uid).disableUrl('"' + oldUrl + '"');
            }
        },
        async addExternal(vcardInfo) {
            const publishMode = PublishMode.PUBLIC;
            const newUrl = await sendExternalToServer(publishMode, vcardInfo.uid, this.calendar.uid);
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
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";
.manage-shares-modal {
    .modal-body {
        padding-bottom: 0;
    }
    .modal-dialog {
        max-width: 50%;
    }
    .bm-form-select {
        width: 100%;
    }
}
</style>
