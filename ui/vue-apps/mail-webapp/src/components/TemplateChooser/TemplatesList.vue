<template>
    <templates-list-spinner v-if="loading" />
    <conversation-list
        v-else-if="keys.length > 0"
        :all-conversation-keys="keys"
        :conversation-keys="keys"
        :draggable="false"
        :multiple="false"
        :selected="value"
        :folder="MY_TEMPLATES"
        :conversations-activated="false"
        @set-selection="key => $emit('input', key)"
    />
    <empty-search-result v-else-if="pattern" :pattern="pattern" />
    <empty-templates-folder v-else :folder="MY_TEMPLATES" />
</template>
<script>
import { mapGetters, mapState } from "vuex";
import { MY_TEMPLATES } from "~/getters";
import ConversationList from "~/components/ConversationList/ConversationList";
import TemplatesListSpinner from "./TemplatesListSpinner";
import EmptySearchResult from "./EmptySearchResult";
import EmptyTemplatesFolder from "./EmptyTemplatesFolder";

export default {
    name: "TemplateChooser",
    components: { ConversationList, TemplatesListSpinner, EmptySearchResult, EmptyTemplatesFolder },
    props: {
        value: {
            type: Number,
            required: true
        }
    },
    computed: {
        ...mapGetters("mail", { MY_TEMPLATES }),
        ...mapState("mail", {
            loading: state => state.messageCompose.templateChooser.loading,
            pattern: state => state.messageCompose.templateChooser.pattern,
            keys: state => state.messageCompose.templateChooser.keys
        })
    }
};
</script>
