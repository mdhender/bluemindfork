import { inject } from "@bluemind/inject";
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";
import {
    FETCH_FOLDERS,
    FETCH_FOLDER_UNREAD_COUNT,
    FETCH_MAILBOXES,
    FETCH_SIGNATURE,
    LOAD_MAX_MESSAGE_SIZE
} from "~actions";
import { MAILSHARES, MY_MAILBOX, MY_MAILBOX_FOLDERS } from "~getters";
import { ADD_MAILBOXES } from "~mutations";
import { create, MailboxType } from "../model/mailbox";

let bootstrapEnded;

export default {
    computed: {
        ...mapGetters("mail", { MY_MAILBOX, MY_MAILBOX_FOLDERS, MAILSHARES }),
        ...mapState("mail", ["folders", "messages", "messageList"])
    },
    data() {
        const initialized = new Promise(resolve => (bootstrapEnded = resolve));
        return { initialized };
    },
    methods: {
        ...mapActions("mail", {
            LOAD_MAX_MESSAGE_SIZE,
            FETCH_FOLDERS,
            FETCH_FOLDER_UNREAD_COUNT,
            FETCH_MAILBOXES,
            FETCH_SIGNATURE
        }),
        ...mapMutations("mail", { ADD_MAILBOXES }),
        ...mapMutations("root-app", ["SET_APP_STATE"])
    },

    async created() {
        // TODO: Replace mailshare parameter with a generic mailbox parameter.
        // const { mailshare } = MessageQueryParam.parse(this.$route.params.messagequery);
        const { userId: owner, formatedName: name } = inject("UserSession");
        const myMailbox = create({ owner, name, type: MailboxType.USER });
        this.ADD_MAILBOXES([myMailbox]);

        try {
            await this.FETCH_FOLDERS(this.MY_MAILBOX);
            await this.FETCH_MAILBOXES();
            this.MAILSHARES.forEach(mailbox => this.FETCH_FOLDERS(mailbox));
            this.MY_MAILBOX_FOLDERS.forEach(this.FETCH_FOLDER_UNREAD_COUNT);
            this.FETCH_SIGNATURE();
            this.LOAD_MAX_MESSAGE_SIZE(owner);
            bootstrapEnded();
        } catch (e) {
            this.SET_APP_STATE("error");
        }
    }
};
