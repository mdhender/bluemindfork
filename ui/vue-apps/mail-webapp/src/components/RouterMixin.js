import isEqual from "lodash.isequal";
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";
import { FETCH_MESSAGE_METADATA, FETCH_MESSAGE_LIST_KEYS } from "~actions";
import { FOLDERS_BY_UPPERCASE_PATH, MY_INBOX } from "~getters";
import {
    SET_ACTIVE_FOLDER,
    SET_MESSAGE_LIST_FILTER,
    SET_SEARCH_PATTERN,
    SET_SEARCH_FOLDER,
    SET_ROUTE_FILTER,
    SET_ROUTE_FOLDER,
    SET_ROUTE_SEARCH
} from "~mutations";
import MessageQueryParam from "../router/MessageQueryParam";
import SearchHelper from "../store.deprecated/SearchHelper";
import { FolderAdaptor } from "../store/folders/helpers/FolderAdaptor";
import { MessageListFilter } from "../store/messageList";

export default {
    computed: {
        ...mapGetters("mail", { FOLDERS_BY_UPPERCASE_PATH, MY_INBOX }),
        ...mapState("mail", ["activeFolder", "folders", "route"]),
        query() {
            const query = MessageQueryParam.parse(this.$route.params.messagequery);
            return {
                mailshare: query.mailshare,
                folder: query.folder,
                search: SearchHelper.parseQuery(query.search),
                filter: (query.filter || MessageListFilter.ALL).trim().toLowerCase()
            };
        }
    },
    watch: {
        "$route.params.messagequery": {
            immediate: true,
            async handler() {
                if (this.route.folder !== this.query.folder) {
                    this.SET_ROUTE_FOLDER(this.query.folder);
                }

                if (!isEqual(this.route.search, this.query.search)) {
                    this.SET_ROUTE_SEARCH(this.query.search);
                }

                if (this.route.filter !== this.query.filter) {
                    this.SET_ROUTE_FILTER(this.query.filter);
                }
            }
        },
        route: {
            immediate: true,
            deep: true,
            async handler() {
                await this.initialized;
                try {
                    const folder = this.resolveFolder(this.query);
                    this.SET_ACTIVE_FOLDER(folder);
                    this.SET_MESSAGE_LIST_FILTER(this.route.filter);
                    this.SET_SEARCH_PATTERN(this.route.search.pattern);
                    this.SET_SEARCH_FOLDER(this.query.folder ? FolderAdaptor.toRef(folder) : undefined);
                    this.$_RouterMixin_fetchMessageList();
                    //TODO: Sync query params with router params (navigate...)
                } catch {
                    //TODO: Add an alert here
                    this.$router.push({ name: "mail:root" });
                }
            }
        }
    },
    methods: {
        ...mapActions("mail", { FETCH_MESSAGE_METADATA, FETCH_MESSAGE_LIST_KEYS }),
        ...mapMutations("mail", {
            SET_ACTIVE_FOLDER,
            SET_MESSAGE_LIST_FILTER,
            SET_ROUTE_FILTER,
            SET_ROUTE_FOLDER,
            SET_ROUTE_SEARCH,
            SET_SEARCH_PATTERN,
            SET_SEARCH_FOLDER
        }),
        resolveFolder({ folder, mailshare }) {
            if (folder || mailshare) {
                let path = folder || mailshare;
                const result = this.FOLDERS_BY_UPPERCASE_PATH[path.toUpperCase()];
                if (!result) {
                    throw "Folder " + path + "not found";
                }
                return result;
            }
            return this.MY_INBOX;
        },
        async $_RouterMixin_fetchMessageList() {
            await this.FETCH_MESSAGE_LIST_KEYS({
                folder: this.folders[this.activeFolder],
                conversationsEnabled: false
            });
            //TODO: We should convert the hard coded slice with a getter GET_MESSAGE_LIST_PAGE(pageNumber)
            // or GET_MESSSAGE_LIST_FIRST_PAGE + GET_MESSAGE_LIST_PREV/NEXT_PAGE
            this.FETCH_MESSAGE_METADATA(this.messageList.messageKeys.slice(0, 40).map(key => this.messages[key]));
            // await dispatch("mail/" + FETCH_MESSAGE_LIST_KEYS, { folder: f, conversationsEnabled }, ROOT);
            // const sorted = rootState.mail.messageList.messageKeys.slice(0, 40).map(key => rootState.mail.messages[key]);
            // await dispatch("mail/" + FETCH_MESSAGE_METADATA, sorted, ROOT);
        }
    }
};
