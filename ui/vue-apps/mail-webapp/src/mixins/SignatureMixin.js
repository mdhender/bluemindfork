import { mapState } from "vuex";

export default {
    computed: {
        ...mapState("mail", { $_SignatureMixin_corporateSignature: state => state.messageCompose.corporateSignature })
    },
    data() {
        return { editorRef: null };
    },
    watch: {
        $_SignatureMixin_corporateSignature() {}
    },
    mounted() {
        if (this.$refs["message-content"]) {
            console.log("ok !!");
            this.editorRef = this.$refs["message-content"];
        } else {
            console.warn("SignatureMixin can't work without an editorRef");
        }
    }
};
