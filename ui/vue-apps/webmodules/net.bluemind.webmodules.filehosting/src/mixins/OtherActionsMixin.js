import { mapGetters } from "vuex";
import { GET_FH_FILE } from "../store/types/getters";
import { GET_CONFIGURATION } from "../store/types/actions";
import FilehostingL10N from "../l10n";

export default {
    componentI18N: { messages: FilehostingL10N },
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
        ...mapGetters("mail", [GET_FH_FILE]),
        isToolarge() {
            return this.maxFilesize === null || !(this.maxFilesize > this.file.size || this.maxFilesize === 0);
        },
        fhFile() {
            return this.GET_FH_FILE(this.file);
        }
    },
    async beforeMount() {
        const { maxFilesize } = await this.$store.dispatch(`mail/${GET_CONFIGURATION}`);
        this.maxFilesize = maxFilesize;
    }
};
