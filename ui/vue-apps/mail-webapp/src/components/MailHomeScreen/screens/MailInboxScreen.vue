<template>
    <chain-of-responsibility :is-responsible="activeFolder === MY_INBOX.key">
        <section>
            <h1>INBOX Home Screen</h1>
        </section>
    </chain-of-responsibility>
</template>
<script>
import { mapGetters, mapState } from "vuex";
import { MY_INBOX } from "~/getters";
import ChainOfResponsibility from "../ChainOfResponsibility";
export default {
    name: "MailInboxScreen",
    components: { ChainOfResponsibility },
    computed: {
        ...mapState("mail", ["activeFolder"]),
        ...mapGetters("mail", { MY_INBOX })
        // computed: {}
    },

    priority: 128
};
</script>
