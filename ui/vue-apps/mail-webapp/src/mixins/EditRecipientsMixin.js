import ComposerActionsMixin from "./ComposerActionsMixin";

export default {
    mixins: [ComposerActionsMixin],
    props: {
        message: { type: Object, required: true }
    },
    data() {
        return { showCc: undefined, showBcc: undefined };
    },
    created() {
        this.showCc = !!this.message.cc.length;
        this.showBcc = !!this.message.bcc.length;
    }
};
