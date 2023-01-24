<template>
    <div>
        <filtered-user-mailbox v-for="mailbox in USER_MAILBOXES" :key="mailbox.key" :mailbox="mailbox" />
        <filtered-mailshares />
        <filtered-group-mailboxes />
    </div>
</template>
<script>
import { mapGetters } from "vuex";
import { USER_MAILBOXES } from "~/getters";
import FilteredGroupMailboxes from "./FilteredGroupMailboxes";
import FilteredMailshares from "./FilteredMailshares";
import FilteredUserMailbox from "./FilteredUserMailbox";

export default {
    name: "FilteredListContent",
    components: { FilteredGroupMailboxes, FilteredUserMailbox, FilteredMailshares },
    computed: {
        ...mapGetters("mail", { USER_MAILBOXES })
    }
};
</script>
