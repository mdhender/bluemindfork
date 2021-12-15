import isEqual from "lodash.isequal";
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";
import { FETCH_CONVERSATION_LIST_KEYS, UNREAD_FOLDER_COUNT } from "~/actions";
import {
    CONVERSATIONS_ACTIVATED,
    FOLDERS_BY_UPPERCASE_PATH,
    MY_INBOX,
    MY_MAILBOX,
    MAILBOX_BY_NAME,
    MAILBOXES_ARE_LOADED
} from "~/getters";
import {
    SET_ACTIVE_FOLDER,
    SET_CONVERSATION_LIST_FILTER,
    SET_ROUTE_FILTER,
    SET_ROUTE_FOLDER,
    SET_ROUTE_MAILBOX,
    SET_ROUTE_SEARCH,
    SET_SEARCH_FOLDER,
    SET_SEARCH_PATTERN
} from "~/mutations";
import { LoadingStatus } from "~/model/loading-status";
import MessageQueryParam from "~/router/MessageQueryParam";
import SearchHelper from "../SearchHelper";
import { FolderAdaptor } from "~/store/folders/helpers/FolderAdaptor";
import { ConversationListFilter } from "~/store/conversationList";
import { WaitForMixin } from "~/mixins";

export default {
    mixins: [WaitForMixin],
    computed: {
        ...mapGetters("mail", {
            FOLDERS_BY_UPPERCASE_PATH,
            MAILBOX_BY_NAME,
            MAILBOXES_ARE_LOADED,
            MY_INBOX,
            MY_MAILBOX
        }),
        ...mapState("mail", ["activeFolder", "folders", "route"]),
        $_RouterMixin_query() {
            const query = MessageQueryParam.parse(this.$route.params.messagequery);
            return {
                mailbox: query.mailbox,
                folder: query.folder,
                search: SearchHelper.parseQuery(query.search),
                filter: (query.filter || ConversationListFilter.ALL).trim().toLowerCase()
            };
        }
    },
    watch: {
        "$route.params.messagequery": {
            immediate: true,
            async handler() {
                if (this.route.folder !== this.$_RouterMixin_query.folder) {
                    this.SET_ROUTE_FOLDER(this.$_RouterMixin_query.folder);
                }

                if (!isEqual(this.route.search, this.$_RouterMixin_query.search)) {
                    this.SET_ROUTE_SEARCH(this.$_RouterMixin_query.search);
                }

                if (this.route.filter !== this.$_RouterMixin_query.filter) {
                    this.SET_ROUTE_FILTER(this.$_RouterMixin_query.filter);
                }

                if (this.route.mailbox !== this.$_RouterMixin_query.mailbox) {
                    this.SET_ROUTE_MAILBOX(this.$_RouterMixin_query.mailbox);
                }
            }
        },
        route: {
            immediate: true,
            deep: true,
            async handler() {
                await this.$_RouterMixin_isReady(this.route.mailbox);
                try {
                    const folder = this.$_RouterMixin_resolveFolder();
                    this.SET_CONVERSATION_LIST_FILTER(this.route.filter);
                    this.SET_SEARCH_PATTERN(this.route.search.pattern);
                    if (this.route.search.pattern) {
                        this.SET_SEARCH_FOLDER(
                            this.$_RouterMixin_query.folder ? FolderAdaptor.toRef(folder) : undefined
                        );
                    }
                    if (!this.route.search.pattern || this.$_RouterMixin_query.folder || !this.activeFolder) {
                        this.SET_ACTIVE_FOLDER(folder);
                    }
                    await this.FETCH_CONVERSATION_LIST_KEYS({
                        folder: this.folders[this.activeFolder],
                        conversationsActivated: this.$store.getters[`mail/${CONVERSATIONS_ACTIVATED}`]
                    });
                    if (folder?.unread === undefined) {
                        await this.$store.dispatch(`mail/${UNREAD_FOLDER_COUNT}`, folder);
                    }
                    //TODO: Sync query params with router params (navigate...)
                } catch {
                    //TODO: Add an alert here
                    this.$router.push({ name: "mail:root" });
                }
            }
        }
    },
    methods: {
        ...mapActions("mail", { FETCH_CONVERSATION_LIST_KEYS }),
        ...mapMutations("mail", {
            SET_ACTIVE_FOLDER,
            SET_CONVERSATION_LIST_FILTER,
            SET_ROUTE_FILTER,
            SET_ROUTE_FOLDER,
            SET_ROUTE_MAILBOX,
            SET_ROUTE_SEARCH,
            SET_SEARCH_PATTERN,
            SET_SEARCH_FOLDER
        }),
        $_RouterMixin_resolveFolder() {
            let path = this.route.folder;
            if (path) {
                let mailbox = this.MY_MAILBOX;
                if (this.route.mailbox) {
                    mailbox = this.MAILBOX_BY_NAME(this.route.mailbox);
                }
                return this.FOLDERS_BY_UPPERCASE_PATH(mailbox.key)[path.toUpperCase()];
            }
            return this.MY_INBOX;
        },
        $_RouterMixin_isReady(name) {
            let assert = mailbox => mailbox && mailbox.loading === LoadingStatus.LOADED;
            if (!name) {
                return this.$waitFor(MY_MAILBOX, assert);
            } else {
                return this.$waitFor(() => this.MAILBOXES_ARE_LOADED && this.MAILBOX_BY_NAME(name), assert);
            }
        }
    }
};
