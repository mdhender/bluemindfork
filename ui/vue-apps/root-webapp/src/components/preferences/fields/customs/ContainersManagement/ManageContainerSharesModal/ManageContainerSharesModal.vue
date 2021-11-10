<template>
    <bm-modal v-model="show" centered modal-class="manage-shares-modal" :hide-footer="!displayFooter">
        <template #modal-title>
            <h1 v-if="showAvailabilitiesManagement" class="modal-title">
                <bm-button variant="inline" @click="back()"><bm-icon icon="arrow-back" size="2x" /></bm-button>
                {{ $t("preferences.calendar.my_calendars.availabilities_advanced_management") }}
            </h1>
            <h1 v-else class="modal-title">{{ initialModalTitle }}</h1>
        </template>
        <availabilities-management v-if="showAvailabilitiesManagement" />
        <share-management
            v-else
            :container="container"
            :is-my-container="isOwnerMyself"
            :is-my-default-calendar="isMyDefaultCalendar"
        />
        <template #modal-footer>
            <bm-button
                v-if="!showAvailabilitiesManagement"
                variant="simple-secondary"
                @click="showAvailabilitiesManagement = true"
            >
                {{ $t("preferences.calendar.my_calendars.availabilities_advanced_management") }}
            </bm-button>
        </template>
    </bm-modal>
</template>

<script>
import { inject } from "@bluemind/inject";
import { BmButton, BmIcon, BmModal } from "@bluemind/styleguide";
import { ContainerType, isDefault } from "../container";
import AvailabilitiesManagement from "./AvailabilitiesManagement";
import ShareManagement from "./ShareManagement";

export default {
    name: "ManageContainerSharesModal",
    components: { AvailabilitiesManagement, BmButton, BmIcon, BmModal, ShareManagement },
    data() {
        return { show: false, showAvailabilitiesManagement: false, container: {} };
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
            return !this.showAvailabilitiesManagement && this.isMyDefaultCalendar;
        },
        isOwnerMyself() {
            return this.container.owner === inject("UserSession").userId;
        },
        isMyDefaultCalendar() {
            return this.isMyCalendar && isDefault(this.container.uid);
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
            this.show = true;
            this.showAvailabilitiesManagement = false;
            this.container = container;
        },
        back() {
            this.showAvailabilitiesManagement = false;
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
