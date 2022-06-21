import { getPartDownloadUrl } from "@bluemind/email";

export default {
    props: {
        message: {
            type: Object,
            required: true
        },
        part: {
            type: Object,
            required: true
        }
    },
    computed: {
        src() {
            return (
                this.part.url ||
                getPartDownloadUrl(this.message.folderRef.uid, this.message.remoteRef.imapUid, this.part)
            );
        }
    }
};
