import isEqual from "lodash.isequal";
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";
import { loadingStatusUtils, folderUtils } from "@bluemind/mail";
import { FETCH_CONVERSATION_LIST_KEYS, UNREAD_FOLDER_COUNT } from "~/actions";
import {
    CONVERSATIONS_ACTIVATED,
    FOLDER_BY_PATH,
    MY_INBOX,
    MY_MAILBOX,
    MAILBOX_BY_NAME,
    MAILBOXES_ARE_LOADED
} from "~/getters";
import {
    RESET_ACTIVE_FOLDER,
    RESET_CONVERSATIONS,
    SET_ACTIVE_FOLDER,
    SET_CONVERSATION_LIST_FILTER,
    SET_CONVERSATION_LIST_SORT,
    SET_CONVERSATION_LIST,
    SET_CURRENT_SEARCH_DEEP,
    SET_CURRENT_SEARCH_FOLDER,
    SET_MAIL_THREAD_SETTING,
    SET_ROUTE_FILTER,
    SET_ROUTE_FOLDER,
    SET_ROUTE_MAILBOX,
    SET_ROUTE_SEARCH,
    SET_ROUTE_SORT,
    SET_SEARCH_QUERY_DEEP,
    SET_SEARCH_QUERY_FOLDER,
    SET_SEARCH_QUERY_PATTERN
} from "~/mutations";
import MessageQueryParam from "~/router/MessageQueryParam";
import SearchHelper from "../MailSearch/SearchHelper";
import { ConversationListFilter } from "~/store/conversationList";
import { WaitForMixin } from "~/mixins";
import { SortField, SortOrder } from "../../store/conversationList";

const { DEFAULT_FOLDERS } = folderUtils;
const { LoadingStatus } = loadingStatusUtils;

export default {
    mixins: [WaitForMixin],
    computed: {
        ...mapGetters("mail", {
            FOLDER_BY_PATH,
            MAILBOX_BY_NAME,
            MAILBOXES_ARE_LOADED,
            MY_INBOX,
            MY_MAILBOX
        }),
        ...mapState("mail", ["activeFolder", "folders", "route", "conversationList"]),
        ...mapState("settings", ["mail_thread"]),
        $_RouterMixin_query() {
            const query = MessageQueryParam.parse(this.$route.params.messagequery);
            return {
                mailbox: query.mailbox,
                folder: query.folder,
                search: SearchHelper.parseQuery(query.search),
                filter: (query.filter || ConversationListFilter.ALL).trim().toLowerCase(),
                sort: {
                    field: query.sort?.split(" ")[0],
                    order: query.sort?.split(" ")[1]
                }
            };
        }
    },
    watch: {
        "$route.params.messagequery": {
            immediate: true,
            async handler() {
                let changed = false;
                if (this.route.folder !== this.$_RouterMixin_query.folder) {
                    this.SET_ROUTE_FOLDER(this.$_RouterMixin_query.folder);
                    changed = true;
                }
                if (this.route.mailbox !== this.$_RouterMixin_query.mailbox) {
                    this.SET_ROUTE_MAILBOX(this.$_RouterMixin_query.mailbox);
                    changed = true;
                }
                if (changed) {
                    this.RESET_ACTIVE_FOLDER();
                    this.RESET_CONVERSATIONS();
                }
                if (!isEqual(this.route.search, this.$_RouterMixin_query.search)) {
                    this.SET_ROUTE_SEARCH(this.$_RouterMixin_query.search);
                    changed = true;
                }
                if (this.route.filter !== this.$_RouterMixin_query.filter) {
                    this.SET_ROUTE_FILTER(this.$_RouterMixin_query.filter);
                    changed = true;
                }
                if (!isEqual(this.route.sort, this.$_RouterMixin_query.sort)) {
                    this.SET_ROUTE_SORT(this.$_RouterMixin_query.sort);
                    changed = true;
                }
                if (changed) {
                    try {
                        await this.$_RouterMixin_fetchConversationlist();
                        //TODO: Add an alert here
                    } catch {
                        this.$router.push({ name: "mail:root" });
                    }
                }
            }
        }
    },
    methods: {
        ...mapActions("mail", { FETCH_CONVERSATION_LIST_KEYS }),
        ...mapMutations("mail", {
            RESET_ACTIVE_FOLDER,
            RESET_CONVERSATIONS,
            SET_ACTIVE_FOLDER,
            SET_CONVERSATION_LIST_FILTER,
            SET_CONVERSATION_LIST_SORT,
            SET_CONVERSATION_LIST,
            SET_CURRENT_SEARCH_DEEP,
            SET_CURRENT_SEARCH_FOLDER,
            SET_ROUTE_FILTER,
            SET_ROUTE_FOLDER,
            SET_ROUTE_MAILBOX,
            SET_ROUTE_SEARCH,
            SET_ROUTE_SORT,
            SET_SEARCH_QUERY_DEEP,
            SET_SEARCH_QUERY_FOLDER,
            SET_SEARCH_QUERY_PATTERN
        }),
        async $_RouterMixin_fetchConversationlist() {
            await this.$_RouterMixin_ready(this.route.mailbox);
            this.SET_CONVERSATION_LIST_FILTER(this.route.filter);
            const folder = this.$_RouterMixin_resolveFolder();
            if (!this.route.search.pattern || this.$_RouterMixin_query.folder || !this.activeFolder) {
                this.SET_ACTIVE_FOLDER(folder);
            }
            this.setSearchState(folder);
            this.$_RouterMixin_setSort();
            await this.FETCH_CONVERSATION_LIST_KEYS({
                folder: this.folders[this.activeFolder],
                conversationsActivated: this.$store.getters[`mail/${CONVERSATIONS_ACTIVATED}`]
            });
            if (folder?.unread === undefined) {
                await this.$store.dispatch(`mail/${UNREAD_FOLDER_COUNT}`, folder);
            }
            //TODO: Sync query params with router params (navigate...)
        },
        $_RouterMixin_setSort() {
            const sort = { field: SortField.DATE, order: SortOrder.DESC };
            if (this.route.sort.field) {
                for (const field of Object.values(SortField)) {
                    if (field === this.route.sort.field.toLowerCase()) {
                        sort.field = field;
                        break;
                    }
                }
            }
            if (this.route.sort.order) {
                for (const order of Object.values(SortOrder)) {
                    if (order === this.route.sort.order.toLowerCase()) {
                        sort.order = order;
                        break;
                    }
                }
            }
            this.SET_CONVERSATION_LIST_SORT(sort);
        },
        $_RouterMixin_resolveFolder() {
            let folderPath = this.route.folder;
            let mailboxName = this.route.mailbox;
            if (mailboxName && !folderPath) {
                folderPath = DEFAULT_FOLDERS.INBOX;
            }

            if (folderPath) {
                let mailbox = this.MY_MAILBOX;
                if (mailboxName) {
                    mailbox = this.MAILBOX_BY_NAME(mailboxName);
                }
                return this.FOLDER_BY_PATH(folderPath, mailbox);
            }
            return this.MY_INBOX;
        },
        async $_RouterMixin_ready(name) {
            await this.$_RouterMixin_waitAndSetThreadSetting();
            await this.$_RouterMixin_waitForMailboxes(name);
        },
        async $_RouterMixin_waitAndSetThreadSetting() {
            await this.$waitFor(
                () => this.mail_thread,
                mailThreadSetting => mailThreadSetting !== undefined
            );
            this.$store.commit(`mail/${SET_MAIL_THREAD_SETTING}`, this.mail_thread);
        },
        async $_RouterMixin_waitForMailboxes(name) {
            let assert = mailbox => mailbox && mailbox.loading === LoadingStatus.LOADED;
            if (!name) {
                return this.$waitFor(MY_MAILBOX, assert);
            } else {
                return this.$waitFor(() => this.MAILBOXES_ARE_LOADED && this.MAILBOX_BY_NAME(name), assert);
            }
        },
        setSearchState(folder) {
            this.SET_SEARCH_QUERY_PATTERN(this.route.search.pattern ? decodeURI(this.route.search.pattern) : null);
            if (this.route.search.pattern) {
                const searchFolder = this.$_RouterMixin_query.folder ? this.folders[folder.key] : null;
                this.SET_SEARCH_QUERY_FOLDER(searchFolder);
                let isDeep = !!this.route.search.deep;
                this.SET_SEARCH_QUERY_DEEP(isDeep);
            } else {
                this.SET_CURRENT_SEARCH_DEEP(true);
                this.SET_CURRENT_SEARCH_FOLDER(folder.imapName === folderUtils.DEFAULT_FOLDERS.INBOX ? null : folder);
            }
        }
    }
};
