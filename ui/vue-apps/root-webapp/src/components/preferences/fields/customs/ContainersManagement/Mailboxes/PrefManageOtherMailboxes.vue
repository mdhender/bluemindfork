<template>
    <containers-management
        :containers="otherMailboxesContainers"
        :container-type="containerType"
        :field-id="id"
        share-column
        @offline-sync-changed="UPDATE_OTHER_MAILBOX_CONTAINER"
        @remove="REMOVE_OTHER_MAILBOX_CONTAINER"
        @subscribe="ADD_OTHER_MAILBOXES"
        @update="UPDATE_OTHER_MAILBOX_CONTAINER"
    >
        <template #item="{ container }">
            <contact :contact="{ dn: container.ownerDisplayname }" transparent bold />
        </template>
        <template #badge-item="{ container, closeFn }">
            <contact
                :contact="{ dn: container.ownerDisplayname }"
                class="mr-2"
                closeable
                @remove="closeFn(container)"
            />
        </template>
    </containers-management>
</template>

<script>
import { ContainerHelper, ContainerType } from "../container";
import { BaseField } from "@bluemind/preferences";
import ContainersManagement from "../ContainersManagement";
import MailboxHelper from "./helper";
import { Contact } from "@bluemind/business-components";

import { mapMutations, mapState } from "vuex";

ContainerHelper.register(ContainerType.MAILBOX, MailboxHelper);

export default {
    name: "PrefManageOtherMailboxes",
    components: { Contact, ContainersManagement },
    mixins: [BaseField],
    data() {
        return { containerType: ContainerType.MAILBOX };
    },
    computed: {
        ...mapState("preferences", {
            otherMailboxesContainers: state => state.containers.otherMailboxesContainers
        })
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
