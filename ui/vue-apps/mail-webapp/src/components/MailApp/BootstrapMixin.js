import { inject } from "@bluemind/inject";
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";
import { FETCH_FOLDERS, FETCH_MAILBOXES, FETCH_SIGNATURE, LOAD_MAX_MESSAGE_SIZE, UNREAD_FOLDER_COUNT } from "~actions";
import { MAILBOXES_ARE_LOADED, MAILSHARES, MAILBOX_BY_NAME, MY_MAILBOX, MY_MAILBOX_FOLDERS } from "~getters";
import { ADD_MAILBOXES } from "~mutations";
import { LoadingStatus } from "../../model/loading-status";
import { create, MailboxType } from "../../model/mailbox";

export default {
    computed: {
        ...mapGetters("mail", { MAILBOXES_ARE_LOADED, MAILBOX_BY_NAME, MY_MAILBOX, MY_MAILBOX_FOLDERS, MAILSHARES }),
        ...mapState("mail", ["folders", "messages", "messageList"])
    },
    methods: {
        ...mapActions("mail", {
            LOAD_MAX_MESSAGE_SIZE,
            FETCH_FOLDERS,
            FETCH_MAILBOXES,
            FETCH_SIGNATURE,
            UNREAD_FOLDER_COUNT
        }),
        ...mapMutations("mail", { ADD_MAILBOXES }),
        ...mapMutations("root-app", ["SET_APP_STATE"]),
        async $_BootstrapMixin_loadMyMailbox() {
            if (!this.MAILBOXES_ARE_LOADED) {
                const { userId: owner, formatedName: name } = inject("UserSession");
                const myMailbox = create({ owner, name, type: MailboxType.USER });
                this.ADD_MAILBOXES([myMailbox]);
            }
            await this.FETCH_FOLDERS(this.MY_MAILBOX);
        },
        async $_BootstrapMixin_loadMailshare(name) {
            await this.FETCH_MAILBOXES();
            const mailbox = this.MAILBOX_BY_NAME(name);
            await this.FETCH_FOLDERS(mailbox);
        },
        async $_BootstrapMixin_loadAllMailshares() {
            if (!this.MAILBOXES_ARE_LOADED) {
                await this.FETCH_MAILBOXES();
            }
            await Promise.all(
                this.MAILSHARES.filter(({ loading }) => loading === LoadingStatus.NOT_LOADED).map(async mailbox =>
                    this.FETCH_FOLDERS(mailbox)
                )
            );
        }
    },

    async created() {
        try {
            if (this.route.mailbox) {
                await this.$_BootstrapMixin_loadMailshare(this.route.mailbox);
            }
            await this.$_BootstrapMixin_loadMyMailbox();
            await this.$_BootstrapMixin_loadAllMailshares();
            this.MY_MAILBOX_FOLDERS.forEach(this.UNREAD_FOLDER_COUNT);
            this.FETCH_SIGNATURE();
            this.LOAD_MAX_MESSAGE_SIZE(inject("UserSession").userId);
        } catch (error) {
            console.error("Error when bootstraping application... ", error);
            this.SET_APP_STATE("error");
        }
    }
};
