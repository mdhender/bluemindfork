import { mapMutations, mapState } from "vuex";
import { MailRoutesMixin } from "~/mixins";
import { ConversationListStatus } from "~/store/conversationList";
import { SET_CONVERSATION_LIST_STATUS } from "~/mutations";

const SPINNER_TIMEOUT = 250;

export default {
    mixins: [MailRoutesMixin],
    computed: {
        ...mapState("mail", {
            currentSearch: ({ conversationList }) => conversationList.search.currentSearch
        })
    },
    methods: {
        ...mapMutations("mail", { SET_CONVERSATION_LIST_STATUS }),
        buildSearchQuery(pattern, folder, deep) {
            let searchQuery = `"${pattern}"`;
            if (folder && folder.key) {
                searchQuery += ` AND in:${folder.key}`;
                if (deep) {
                    searchQuery += ` AND is:deep`;
                }
            }
            return searchQuery;
        },
        updateRoute() {
            const { pattern, folder, deep } = this.currentSearch;
            this.$router.navigate({
                name: "v:mail:home",
                params: {
                    search: this.buildSearchQuery(pattern, folder, deep),
                    ...this.folderRoute({ key: folder?.key }).params
                }
            });
        },
        search() {
            this.showSpinner();
            this.updateRoute();
            this.cancelSpinner();
        },
        showSpinner() {
            this.SET_CONVERSATION_LIST_STATUS(ConversationListStatus.LOADING), SPINNER_TIMEOUT;
        },
        cancelSpinner() {
            this.SET_CONVERSATION_LIST_STATUS(ConversationListStatus.SUCCESS);
        }
    }
};
