<template>
    <containers-management
        class="pref-manage-my-addressbooks"
        :containers="myAddressbooks"
        :container-type="containerType"
        :create-container-fn="create"
        :collapsed="collapsed"
        :field-id="id"
        manage-mine
        @offline-sync-changed="UPDATE_PERSONAL_ADDRESSBOOK"
        @update="update"
        @remove="remove"
        @reset-data="resetData"
    >
        <template v-slot:item="{ container }"><bm-addressbook-item :addressbook="container" /></template>
    </containers-management>
</template>

<script>
import { mapActions, mapMutations, mapState } from "vuex";

import { inject } from "@bluemind/inject";

import { containerToAddressBookDescriptor, ContainerHelper, ContainerType } from "../container";
import AddressbookHelper from "./helper";
import BmAddressbookItem from "./BmAddressbookItem";
import BaseField from "../../../../mixins/BaseField";
import ContainersManagement from "../ContainersManagement";
import { SUCCESS } from "@bluemind/alert.store";
import { SAVE_ALERT } from "../../../../Alerts/defaultAlerts";

ContainerHelper.register(ContainerType.ADDRESSBOOK, AddressbookHelper);

export default {
    name: "PrefManageMyAddressBooks",
    components: { BmAddressbookItem, ContainersManagement },
    mixins: [BaseField],
    data() {
        return { containerType: ContainerType.ADDRESSBOOK };
    },
    computed: {
        ...mapState("preferences", { myAddressbooks: state => state.containers.myAddressbooks })
    },
    methods: {
        ...mapActions("preferences", ["SUBSCRIBE_TO_CONTAINERS"]),
        ...mapActions("alert", { SUCCESS }),
        ...mapMutations("preferences", [
            "ADD_PERSONAL_ADDRESSBOOK",
            "REMOVE_PERSONAL_ADDRESSBOOK",
            "UPDATE_PERSONAL_ADDRESSBOOK"
        ]),
        resetData(addressbook) {
            inject("AddressBookPersistence", addressbook.uid).reset();
        },
        async remove(addressbook) {
            await inject("AddressBooksMgmtPersistence").remove(addressbook.uid);
            this.REMOVE_PERSONAL_ADDRESSBOOK(addressbook.uid);
            this.SUCCESS(SAVE_ALERT);
        },
        async create(addressbook) {
            const addressBookDescriptor = containerToAddressBookDescriptor(addressbook);
            await inject("AddressBooksMgmtPersistence").create(addressbook.uid, addressBookDescriptor, false);
            this.ADD_PERSONAL_ADDRESSBOOK(addressbook);
            this.SUBSCRIBE_TO_CONTAINERS([addressbook]);
            this.SUCCESS(SAVE_ALERT);
        },
        async update(container) {
            const addressBookDescriptor = containerToAddressBookDescriptor(container);
            await inject("AddressBooksMgmtPersistence").update(container.uid, addressBookDescriptor);
            this.UPDATE_PERSONAL_ADDRESSBOOK(container);
            this.SUCCESS(SAVE_ALERT);
        }
    }
};
</script>
