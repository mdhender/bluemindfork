<template>
    <containers-management
        :containers="otherAddressbooks"
        :container-type="containerType"
        has-share-column
        @offline-sync-changed="UPDATE_OTHER_ADDRESSBOOK"
        @remove="REMOVE_OTHER_ADDRESSBOOK"
        @subscribe="ADD_OTHER_ADDRESSBOOK"
        @update="UPDATE_OTHER_ADDRESSBOOK"
    >
        <template v-slot:item="{ container }"><bm-addressbook-item :addressbook="container" /></template>
        <template v-slot:badge-item="{ container, closeFn }">
            <bm-addressbook-badge :addressbook="container" closeable @close="closeFn(container)" />
        </template>
    </containers-management>
</template>

<script>
import BmAddressbookBadge from "./BmAddressbookBadge";
import BmAddressbookItem from "./BmAddressbookItem";
import { ContainerType } from "../container";
import ContainersManagement from "../ContainersManagement";
import { mapMutations, mapState } from "vuex";

export default {
    name: "PrefManageOtherAddressBooks",
    components: { BmAddressbookBadge, BmAddressbookItem, ContainersManagement },
    data() {
        return { containerType: ContainerType.ADDRESSBOOK };
    },
    computed: {
        ...mapState("preferences", ["otherAddressbooks"])
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
