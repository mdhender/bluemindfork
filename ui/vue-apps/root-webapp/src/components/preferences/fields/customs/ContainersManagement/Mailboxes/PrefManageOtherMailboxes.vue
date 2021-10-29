<template>
    <containers-management
        :containers="otherMailboxesContainers"
        :container-type="containerType"
        @add="ADD_OTHER_MAILBOXES"
        @update="UPDATE_OTHER_MAILBOX_CONTAINER"
        @remove="REMOVE_OTHER_MAILBOX_CONTAINER"
    >
        <template v-slot:item="{ container }">
            <!-- FIXME: group may not be displayed as user -->
            <bm-contact :contact="{ dn: container.ownerDisplayname }" variant="transparent" />
        </template>
        <template v-slot:badge-item="{ container, closeFn }">
            <bm-contact
                :contact="{ dn: container.ownerDisplayname }"
                class="mr-2"
                closeable
                @remove="closeFn(container)"
            />
        </template>
    </containers-management>
</template>

<script>
import { ContainerType } from "../container";
import ContainersManagement from "../ContainersManagement";
import { BmContact } from "@bluemind/styleguide";
import { mapMutations, mapState } from "vuex";

export default {
    name: "PrefManageOtherMailboxes",
    components: { BmContact, ContainersManagement },
    data() {
        return { containerType: ContainerType.MAILBOX };
    },
    computed: {
        ...mapState("preferences", ["otherMailboxesContainers"])
    },
    methods: {
        ...mapMutations("preferences", [
            "ADD_OTHER_MAILBOXES",
            "REMOVE_OTHER_MAILBOX_CONTAINER",
            "UPDATE_OTHER_MAILBOX_CONTAINER"
        ])
    }
};
</script>
