<template>
    <bm-modal
        v-model="show"
        variant="advanced"
        size="lg"
        height="sm"
        scrollable
        centered
        :modal-class="{
            'manage-shares-modal': true,
            'invisible-footer': !displayAvailabilitiesManagement || showAvailabilitiesManagement
        }"
    >
        <template #modal-header>
            <bm-modal-header class="d-lg-none" :title="modalTitle" @close="back()" />
            <bm-modal-header class="d-none d-lg-block" @close="close()">
                <template #title>
                    <bm-icon-button
                        v-if="showAvailabilitiesManagement"
                        variant="compact"
                        icon="arrow-back"
                        @click="back()"
                    />
                    <div class="custom-modal-title">{{ modalTitle }}</div>
                </template>
            </bm-modal-header>
        </template>

        <availabilities-management v-if="showAvailabilitiesManagement" />
        <share-management
            v-else
            :container="container"
            :is-my-container="isOwnerMyself"
            :is-my-default-calendar="isMyDefaultCalendar"
        />

        <template #modal-footer>
            <bm-alert-area v-show="alerts.length > 0" :alerts="alerts" stackable @remove="REMOVE">
                <template #default="context">
                    <component :is="context.alert.renderer" :alert="context.alert" />
                </template>
            </bm-alert-area>
            <bm-button
                v-if="!showAvailabilitiesManagement && displayAvailabilitiesManagement"
                variant="text"
                @click="showAvailabilitiesManagement = true"
            >
                {{ $t("preferences.calendar.my_calendars.availabilities_advanced_management") }}
            </bm-button>
        </template>
    </bm-modal>
</template>

<script>
import { mapActions, mapState } from "vuex";
import { inject } from "@bluemind/inject";
import { REMOVE } from "@bluemind/alert.store";
import {
    BmAlertArea,
    BmButton,
    BmButtonClose,
    BmIconButton,
    BmModal,
    BmModalHeader,
    BmNavbarBack,
    BmRow
} from "@bluemind/ui-components";
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
        BmModalHeader,
        BmNavbarBack,
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
        modalTitle() {
            if (this.showAvailabilitiesManagement) {
                return this.$t("preferences.calendar.my_calendars.availabilities_advanced_management");
            }
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
        close() {
            this.show = false;
        },
        back() {
            if (this.showAvailabilitiesManagement) {
                this.showAvailabilitiesManagement = false;
            } else {
                this.close();
            }
        }
    }
};
</script>

<style lang="scss">
@use "sass:math";
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";

.manage-shares-modal {
    .bm-modal-header {
        .title-and-close {
            padding-left: base-px-to-rem(14);
            .modal-title {
                display: flex;
                margin-top: 0 !important;
                .custom-modal-title {
                    padding-left: base-px-to-rem(16);
                    $btn-height: map-get($btn-close-sizes, "lg");
                    margin-top: math.div($btn-height - $title-line-height, 2);
                }
            }
        }
    }

    .modal-body {
        padding: $sp-6 0 !important;
    }
    .modal-footer .bm-alert-area {
        margin: 0;
    }
    &.invisible-footer .modal-footer {
        display: none !important;
    }

    .bm-alert-area {
        min-width: 100%;
        position: absolute;
        bottom: 0;
        left: 0;
        z-index: $zindex-popover;
    }
}
</style>
