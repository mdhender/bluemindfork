<template>
    <bm-modal v-model="show" centered modal-class="manage-shares-modal" :hide-footer="!displayFooter">
        <template #modal-title>
            <h1 v-if="showAvailabilitiesAdvancedManagement" class="modal-title">
                <bm-button variant="inline" @click="back()"><bm-icon icon="arrow-back" size="2x" /></bm-button>
                {{ $t("preferences.calendar.my_calendars.availabilities_advanced_management") }}
            </h1>
            <h1 v-else class="modal-title">{{ initialModalTitle }}</h1>
        </template>
        <bm-spinner v-if="isLoading" :size="2" class="d-flex justify-content-center" />
        <availabilities-advanced-management v-else-if="showAvailabilitiesAdvancedManagement" />
        <modal-body
            v-else
            :container="container"
            :dir-entries-acl="dirEntriesAcl"
            :domain-acl="domainAcl"
            :external-shares="externalShares"
            :is-my-container="isOwnerMyself"
            :is-my-default-calendar="isMyDefaultCalendar"
            @add-external="addExternal"
            @add-new-external="createContactAndAddExternal"
            @edit-publish-mode="editPublishMode"
            @remove-external="removeExternal"
        />
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
import { ContainerType } from "../container";
import AvailabilitiesAdvancedManagement from "./AvailabilitiesAdvancedManagement";
import { loadAcl } from "./AclHelper";
import { loadCalendarUrls, sendExternalToServer } from "./ExternalShareHelper";
import ModalBody from "./ModalBody";

export default {
    name: "ManageContainerSharesModal",
    components: {
        AvailabilitiesAdvancedManagement,
        BmButton,
        BmIcon,
        BmModal,
        BmSpinner,
        ModalBody
    },
    data() {
        return {
            isLoading: true,
            show: false,
            showAvailabilitiesAdvancedManagement: false,
            container: {},

            dirEntriesAcl: [],
            domainAcl: null,
            externalShares: []
        };
    },
    computed: {
        initialModalTitle() {
            if (this.isMyMailbox) {
                return this.$t("preferences.mail.my_mailbox.modal_title");
            }
            return this.$t("preferences.manage_shares.title", {
                name: this.container.name,
                type: this.$t("common.container_type_with_definite_article." + this.container.type)
            });
        },
        displayFooter() {
            return !this.isLoading && !this.showAvailabilitiesAdvancedManagement && this.isMyDefaultCalendar;
        },
        isOwnerMyself() {
            return this.container.owner === inject("UserSession").userId;
        },
        isMyDefaultCalendar() {
            return this.isMyCalendar && this.container.uid === "calendar:Default:" + inject("UserSession").userId;
        },
        isMyCalendar() {
            return this.container.type === ContainerType.CALENDAR && this.isOwnerMyself;
        },
        isMyMailbox() {
            return this.container.type === ContainerType.MAILBOX && this.isOwnerMyself;
        }
    },
    methods: {
        async open(container) {
            this.isLoading = true;
            this.show = true;
            this.showAvailabilitiesAdvancedManagement = false;

            this.container = container;
            this.searchedInput = "";

            const acl = await loadAcl(this.container, this.isMyDefaultCalendar);
            this.domainAcl = acl.domainAcl;
            this.dirEntriesAcl = acl.dirEntriesAcl;

            if (this.container.type === ContainerType.CALENDAR) {
                this.externalShares = await loadCalendarUrls(this.container.uid);
            }

            this.isLoading = false;
        },
        back() {
            this.showAvailabilitiesAdvancedManagement = false;
        },
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
                const publishMode =
                    externalShare.publishMode === PublishMode.PUBLIC ? PublishMode.PRIVATE : PublishMode.PUBLIC;
                externalShare.publishMode = publishMode;
                const newUrl = await sendExternalToServer(publishMode, externalShare.token, this.container.uid);
                const oldUrl = externalShare.url;
                externalShare.url = newUrl;
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
