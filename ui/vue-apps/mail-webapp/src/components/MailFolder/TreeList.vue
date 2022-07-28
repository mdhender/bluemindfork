<template>
    <div>
        <user-folders v-for="mailbox in USER_MAILBOXES" :key="mailbox.key" :mailbox="mailbox" />
        <mailshare-folders />
    </div>
</template>
<script>
import { mapGetters } from "vuex";
import { USER_MAILBOXES } from "~/getters";
import MailshareFolders from "./MailshareFolders.vue";
import UserFolders from "./UserFolders.vue";

export default {
    name: "FilteredList",
    components: { MailshareFolders, UserFolders },
    computed: {
        ...mapGetters("mail", { USER_MAILBOXES })
    }
};
</script>
