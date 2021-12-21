<template>
    <div>
        <filtered-user-mailbox :mailbox="MY_MAILBOX" />
        <filtered-mailshares />
    </div>
</template>
<script>
import { mapGetters } from "vuex";
import { MY_MAILBOX } from "~/getters";
import FilteredMailshares from "./FilteredMailshares";
import FilteredUserMailbox from "./FilteredUserMailbox";

export default {
    name: "FilteredList",
    components: { FilteredUserMailbox, FilteredMailshares },
    computed: {
        ...mapGetters("mail", { MY_MAILBOX })
    }
};
</script>
