<template>
    <containers-management
        :containers="otherAddressbooks"
        :container-type="containerType"
        :field-id="id"
        share-column
        @offline-sync-changed="UPDATE_OTHER_ADDRESSBOOK"
        @remove="REMOVE_OTHER_ADDRESSBOOK"
        @subscribe="ADD_OTHER_ADDRESSBOOK"
        @update="UPDATE_OTHER_ADDRESSBOOK"
    >
        <template #item="{ container }"><bm-addressbook-item :addressbook="container" /></template>
        <template #badge-item="{ container, closeFn }">
            <bm-addressbook-badge :addressbook="container" closeable @close="closeFn(container)" />
        </template>
    </containers-management>
</template>

<script>
import AddressbookHelper from "./helper";
import BmAddressbookBadge from "./BmAddressbookBadge";
import BmAddressbookItem from "./BmAddressbookItem";
import { ContainerHelper, ContainerType } from "../container";
import { BaseField } from "@bluemind/preferences";
import ContainersManagement from "../ContainersManagement";
import { mapMutations, mapState } from "vuex";

ContainerHelper.register(ContainerType.ADDRESSBOOK, AddressbookHelper);

export default {
    name: "PrefManageOtherAddressBooks",
    components: { BmAddressbookBadge, BmAddressbookItem, ContainersManagement },
    mixins: [BaseField],
    data() {
        return { containerType: ContainerType.ADDRESSBOOK };
    },
    computed: {
        ...mapState("preferences", { otherAddressbooks: state => state.containers.otherAddressbooks })
    },
    methods: {
        ...mapMutations("preferences", [
            "ADD_OTHER_ADDRESSBOOK",
            "REMOVE_OTHER_ADDRESSBOOK",
            "UPDATE_OTHER_ADDRESSBOOK"
        ])
    }
};
</script>
