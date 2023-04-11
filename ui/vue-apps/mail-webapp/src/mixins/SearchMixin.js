import { MailRoutesMixin } from "~/mixins";
import { ConversationListStatus } from "~/store/conversationList";
import { SET_CONVERSATION_LIST_STATUS, SET_SEARCH_MODE } from "~/mutations";
import { mapMutations, mapState } from "vuex";

const SPINNER_TIMEOUT = 250;

export default {
    data() {
        return {
            pattern: null
        };
    },
    computed: {
        ...mapState("mail", ["searchMode"])
    },
    watch: {
        "currentSearch.pattern": {
            async handler(pattern) {
                this.pattern = pattern;
                if (this.pattern) {
                    this.SET_SEARCH_MODE(true);
                }
            }
        }
    },
    mixins: [MailRoutesMixin],
    methods: {
        ...mapMutations("mail", { SET_CONVERSATION_LIST_STATUS, SET_SEARCH_MODE }),
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
        updateRoute(pattern, folder, deep) {
            this.$router.navigate({
                name: "v:mail:home",
                params: {
                    search: this.buildSearchQuery(pattern, folder, deep),
                    ...this.folderRoute({ key: folder?.key }).params
                }
            });
        },
        search(pattern, folder, deep) {
            this.showSpinner();
            this.updateRoute(pattern, folder, deep);
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
