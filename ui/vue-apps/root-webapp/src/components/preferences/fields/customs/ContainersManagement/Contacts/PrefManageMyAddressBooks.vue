<template>
    <containers-management
        class="pref-manage-my-addressbooks"
        :containers="myAddressbooks"
        :container-type="containerType"
        manage-mine
        @create="create"
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

import { containerToAddressBookDescriptor, ContainerType } from "../container";
import BmAddressbookItem from "./BmAddressbookItem";
import ContainersManagement from "../ContainersManagement";

export default {
    name: "PrefManageMyAddressBooks",
    components: { BmAddressbookItem, ContainersManagement },
    data() {
        return { containerType: ContainerType.ADDRESSBOOK };
    },
    computed: {
        ...mapState("preferences", ["myAddressbooks"])
    },
    methods: {
        ...mapActions("preferences", ["SUBSCRIBE_TO_CONTAINERS"]),
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
        },
        async create(addressbook) {
            const addressBookDescriptor = containerToAddressBookDescriptor(addressbook);
            await inject("AddressBooksMgmtPersistence").create(addressbook.uid, addressBookDescriptor, false);
            this.ADD_PERSONAL_ADDRESSBOOK(addressbook);
            this.SUBSCRIBE_TO_CONTAINERS([addressbook]);
        },
        async update(container) {
            const addressBookDescriptor = containerToAddressBookDescriptor(container);
            await inject("AddressBooksMgmtPersistence").update(container.uid, addressBookDescriptor);
            this.UPDATE_PERSONAL_ADDRESSBOOK(container);
        }
    }
};
</script>
