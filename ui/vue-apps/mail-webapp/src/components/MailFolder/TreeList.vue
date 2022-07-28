<template>
    <div>
        <user-folders v-for="mailbox in USER_MAILBOXES" :key="mailbox.key" :mailbox="mailbox" />
        <mailshare-folders />
        <group-folders />
    </div>
</template>
<script>
import { mapGetters } from "vuex";
import { USER_MAILBOXES } from "~/getters";
import GroupFolders from "./GroupFolders";
import MailshareFolders from "./MailshareFolders";
import UserFolders from "./UserFolders";

export default {
    name: "TreeList",
    components: { GroupFolders, MailshareFolders, UserFolders },
    computed: {
        ...mapGetters("mail", { USER_MAILBOXES })
    }
};
</script>
