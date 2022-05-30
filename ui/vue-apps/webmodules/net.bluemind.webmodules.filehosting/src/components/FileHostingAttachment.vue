<script>
import { mapGetters } from "vuex";
export default {
    name: "FileHostingAttachment",
    props: {
        attachment: {
            type: Object,
            required: true
        },
        message: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapGetters("mail", ["GET_FH_ATTACHMENT"]),
        decorated() {
            if (isDetached(this.attachment)) {
                return {
                    ...this.attachment,
                    ...this.GET_FH_ATTACHMENT(this.message, this.attachment)
                };
            }
            return this.attachment;
        }
    },
    render() {
        return this.$scopedSlots.default({ attachment: this.decorated, message: this.message });
    }
};
function isDetached({ headers }) {
    return (
        headers.find(header => header.name.toLowerCase() === "x-bm-disposition") ||
        headers.find(header => header.name.toLowerCase() === "x-mozilla-cloud-part")
    );
}
</script>
