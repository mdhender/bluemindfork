<template>
    <bm-modal v-model="show" centered modal-class="manage-shares-modal">
        <template #modal-title>
            <div v-if="showAvailabilitiesManagement" class="modal-title">
                <bm-icon-button icon="arrow-back" size="lg" @click="back()" />
                {{ $t("preferences.calendar.my_calendars.availabilities_advanced_management") }}
            </div>
            <div v-else class="modal-title">{{ initialModalTitle }}</div>
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
                v-if="!showAvailabilitiesManagement && displayAvailabilitiesManagement"
                variant="text"
                @click="showAvailabilitiesManagement = true"
            >
                {{ $t("preferences.calendar.my_calendars.availabilities_advanced_management") }}
            </bm-button>
            <bm-alert-area v-show="alerts.length > 0" :alerts="alerts" stackable @remove="REMOVE">
                <template v-slot="context">
                    <component :is="context.alert.renderer" :alert="context.alert" />
                </template>
            </bm-alert-area>
        </template>
    </bm-modal>
</template>

<script>
import { mapActions, mapState } from "vuex";
import { inject } from "@bluemind/inject";
import { REMOVE } from "@bluemind/alert.store";
import { BmAlertArea, BmButton, BmButtonClose, BmIconButton, BmModal, BmRow } from "@bluemind/styleguide";
import { ContainerType, isDefault } from "../container";
import AvailabilitiesManagement from "./AvailabilitiesManagement";
import ShareManagement from "./ShareManagement";

export default {
    name: "ManageContainerSharesModal",
    components: {
        AvailabilitiesManagement,
        BmAlertArea,
        BmButton,
        BmButtonClose,
        BmIconButton,
        BmModal,
        BmRow,
        ShareManagement
    },
    data() {
        return {
            show: false,
            showAvailabilitiesManagement: false,
            container: {}
        };
    },
    computed: {
        ...mapState({ alerts: state => state.alert.filter(({ area }) => area === "pref-modal") }),
        initialModalTitle() {
            if (this.isMyMailbox) {
                return this.$t("preferences.mail.my_mailbox.modal_title");
            }
            const share = this.isOwnerMyself
                ? ""
                : "(" + this.$t("common.shared_by", { name: this.container.ownerDisplayname }) + ")";
            return this.$t("preferences.manage_shares.title", {
                name: this.container.name,
                type: this.$t("common.container_type_with_definite_article." + this.container.type),
                share
            });
        },
        displayAvailabilitiesManagement() {
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
        ...mapActions("alert", { REMOVE }),
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
@import "~@bluemind/styleguide/css/mixins/_responsiveness";
@import "~@bluemind/styleguide/css/_variables";

.manage-shares-modal {
    .modal-body {
        padding-bottom: 0;
    }
    .modal-footer .bm-alert-area {
        margin: 0;
    }
    .modal-dialog {
        @include from-lg {
            max-width: 50%;
        }
    }
    .bm-alert-area {
        min-width: 100%;
        position: absolute;
        bottom: 0;
        left: 0;
    }
}
</style>
