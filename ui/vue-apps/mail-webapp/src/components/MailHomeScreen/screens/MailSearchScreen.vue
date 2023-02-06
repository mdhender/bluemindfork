<template>
    <chain-of-responsibility :is-responsible="CONVERSATION_LIST_IS_SEARCH_MODE">
        <section>
            <h1>Search Home Screen</h1>
        </section>
    </chain-of-responsibility>
</template>
<script>
import { mapGetters } from "vuex";
import { CONVERSATION_LIST_IS_SEARCH_MODE } from "~/getters";
import ChainOfResponsibility from "../ChainOfResponsibility";
export default {
    name: "MailSearchScreen",
    components: { ChainOfResponsibility },
    computed: {
        ...mapGetters("mail", { CONVERSATION_LIST_IS_SEARCH_MODE })
    },

    priority: 129
};
</script>
