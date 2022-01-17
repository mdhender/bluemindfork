import { inject } from "@bluemind/inject";
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";
import {
    MAILBOXES_ARE_LOADED,
    MAILSHARES,
    MAILBOX_BY_NAME,
    MY_MAILBOX,
    MY_MAILBOX_FOLDERS,
    MY_INBOX,
    MAILBOXES
} from "~/getters";
import { FETCH_FOLDERS, FETCH_MAILBOXES, LOAD_MAX_MESSAGE_SIZE, UNREAD_FOLDER_COUNT } from "~/actions";
import { ADD_MAILBOXES } from "~/mutations";
import { LoadingStatus } from "~/model/loading-status";
import { create, MailboxType } from "~/model/mailbox";

export default {
    computed: {
        ...mapGetters("mail", {
            MAILBOX_BY_NAME,
            MAILBOXES_ARE_LOADED,
            MAILBOXES,
            MAILSHARES,
            MY_INBOX,
            MY_MAILBOX_FOLDERS,
            MY_MAILBOX
        }),
        ...mapState("mail", ["folders", "conversationList"])
    },
    methods: {
        ...mapActions("mail", { LOAD_MAX_MESSAGE_SIZE, FETCH_FOLDERS, FETCH_MAILBOXES, UNREAD_FOLDER_COUNT }),
        ...mapMutations("mail", { ADD_MAILBOXES }),
        ...mapMutations("root-app", ["SET_APP_STATE"]),
        $_BootstrapMixin_initMyMailbox() {
            const { userId: owner, defaultEmail: address } = inject("UserSession");
            const myMailbox = create({ owner, dn: address, address, type: MailboxType.USER });
            this.ADD_MAILBOXES([myMailbox]);
        },
        async $_BootstrapMixin_loadMailbox() {
            await this.FETCH_MAILBOXES();
            const mailbox = this.MAILBOX_BY_NAME(this.route.mailbox);
            await this.FETCH_FOLDERS(mailbox);
        },
        async $_BootstrapMixin_loadAllMailboxes() {
            if (!this.MAILBOXES_ARE_LOADED) {
                await this.FETCH_MAILBOXES();
            }
            await Promise.all(
                this.MAILBOXES.filter(({ loading }) => loading === LoadingStatus.NOT_LOADED).map(mailbox =>
                    this.FETCH_FOLDERS(mailbox)
                )
            );
        }
    },

    async created() {
        try {
            this.$_BootstrapMixin_initMyMailbox();
            if (this.route.mailbox) {
                await this.$_BootstrapMixin_loadMailbox();
            }
            await this.FETCH_FOLDERS(this.MY_MAILBOX);
            await this.$_BootstrapMixin_loadAllMailboxes();
            if (this.MY_INBOX?.unread === undefined) {
                await this.UNREAD_FOLDER_COUNT(this.MY_INBOX);
            }
            this.LOAD_MAX_MESSAGE_SIZE(inject("UserSession").userId);
        } catch (error) {
            // eslint-disable-next-line no-console
            console.error("Error while bootstraping application... ", error);
            this.SET_APP_STATE("error");
        }
    }
};
