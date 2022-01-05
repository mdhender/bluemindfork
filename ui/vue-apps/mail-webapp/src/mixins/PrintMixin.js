import Vue from "vue";

export default {
    methods: {
        printMessage(message) {
            const folder = this.$store.state.mail.folders[message.folderRef.key];
            const mailbox = this.$store.state.mail.mailboxes[folder.mailboxRef.key];
            const mbox =
                (mailbox.type === "mailshares"
                    ? "Dossiers Partag&AOK-s"
                    : mailbox.key !== this.$store.getters[`mail/MY_MAILBOX`].key
                    ? "Autres utilisateurs"
                    : "") + folder.path;
            const url = `/webmail/?_task=mail&_action=print&_uid=${message.remoteRef.imapUid}&_mbox=${mbox}`;

            const print = document.createElement("iframe");
            print.style.width = "0";
            print.style.height = "0";
            document.body.appendChild(print);
            print.src = url;
            // print.contentWindow.print();
            print.contentWindow.onafterprint = () => print.remove();
        },
        print(body, style) {
            const content = new PrintContent({ parent: this.$parent });
            content.$slots.body = body;
            content.$slots.style = style;
            const print = document.createElement("iframe");
            print.style.width = "0";
            print.style.height = "0";
            document.body.appendChild(print);
            content.$mount(print.contentDocument.body);
            print.contentWindow.print();
            print.contentWindow.onafterprint = () => print.remove();
        }
    }
};
const PrintContent = Vue.extend({
    render(h) {
        return h("body", [h("style", this.$slots.style), this.$slots.body]);
    }
});
