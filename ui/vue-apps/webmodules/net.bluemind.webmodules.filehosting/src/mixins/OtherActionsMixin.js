import { mapState } from "vuex";
import { GET_CONFIGURATION } from "../store/types/actions";
import { getFhHeader } from "../helpers";
export default {
    props: {
        file: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            maxFilesize: null
        };
    },
    computed: {
        ...mapState("mail", ["folders"]),
        isToolarge() {
            return this.maxFilesize === null || !(this.maxFilesize > this.file.size || this.maxFilesize === 0);
        },
        fhFile() {
            return getFhHeader(this.file.headers);
        },
        isReadOnly() {
            const CURRENT_CONVERSATION = this.$store.getters["mail/CURRENT_CONVERSATION_METADATA"];
            return CURRENT_CONVERSATION && !this.folders[CURRENT_CONVERSATION.folderRef.key]?.writable;
        }
    },
    async beforeMount() {
        const { maxFilesize } = await this.$store.dispatch(`mail/${GET_CONFIGURATION}`);
        this.maxFilesize = maxFilesize;
    }
};
