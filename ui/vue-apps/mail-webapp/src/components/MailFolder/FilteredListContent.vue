<template>
    <div>
        <filtered-user-mailbox v-for="mailbox in USER_MAILBOXES" :key="mailbox.key" :mailbox="mailbox" />
        <filtered-mailshares />
    </div>
</template>
<script>
import { mapGetters } from "vuex";
import { USER_MAILBOXES } from "~/getters";
import FilteredMailshares from "./FilteredMailshares";
import FilteredUserMailbox from "./FilteredUserMailbox";

export default {
    name: "FilteredListContent",
    components: { FilteredUserMailbox, FilteredMailshares },
    computed: {
        ...mapGetters("mail", { USER_MAILBOXES })
    }
};
</script>
