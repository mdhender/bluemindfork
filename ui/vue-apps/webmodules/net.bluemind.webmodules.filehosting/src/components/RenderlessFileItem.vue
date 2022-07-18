<script>
import { mapGetters } from "vuex";
export default {
    name: "RenderlessFileItem",
    props: {
        file: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapGetters("mail", ["GET_FH_FILE"]),
        decorated() {
            if (isDetached(this.file)) {
                return {
                    ...this.file,
                    ...this.GET_FH_FILE(this.file, this.file)
                };
            }
            return this.file;
        }
    },
    render() {
        return this.$scopedSlots.default({ file: this.decorated });
    }
};
function isDetached({ headers }) {
    return (
        headers.find(header => header.name.toLowerCase() === "x-bm-disposition") ||
        headers.find(header => header.name.toLowerCase() === "x-mozilla-cloud-part")
    );
}
</script>
