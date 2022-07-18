import { fileUtils } from "@bluemind/mail";
const { isLarge, isUploading, hasRemoteContent, isAllowedToPreview } = fileUtils;

export default {
    props: {
        file: {
            type: Object,
            required: true
        }
    },
    computed: {
        blockedRemoteContent() {
            return this.$store.state.mail.consultPanel.remoteImages.mustBeBlocked;
        },
        isAllowedToPreview() {
            return isAllowedToPreview(this.file);
        },
        hasRemoteContent() {
            return hasRemoteContent(this.file);
        },
        hasBlockedRemoteContent() {
            return hasRemoteContent(this.file) && this.$store.state.mail.consultPanel.remoteImages.mustBeBlocked;
        },
        isLarge() {
            return isLarge(this.file);
        }
    },
    methods: {
        isUploading
    }
};
