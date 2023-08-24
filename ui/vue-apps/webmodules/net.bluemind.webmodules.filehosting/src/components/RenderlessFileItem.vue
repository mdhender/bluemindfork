<script>
import { mapMutations } from "vuex";
import { GET_FH_FILE } from "../store/types/mutations";
import { getFhHeader, getFhInfos } from "../helpers";
import { fileUtils } from "@bluemind/mail";
const { FileStatus } = fileUtils;
export default {
    name: "RenderlessFileItem",
    props: {
        file: {
            type: Object,
            required: true
        }
    },
    computed: {
        decorated() {
            if (getFhHeader(this.file.headers)) {
                return {
                    ...this.file,
                    ...getFhInfos(this.file)
                };
            }
            return this.file;
        }
    },
    methods: {
        ...mapMutations("mail", { GET_FH_FILE })
    },
    render() {
        return this.$scopedSlots.default({ file: this.decorated });
    }
};
</script>
