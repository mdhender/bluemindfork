import { mapActions, mapState } from "vuex";
import { messageUtils } from "@bluemind/mail";
import { INFO, REMOVE } from "@bluemind/alert.store";

const { hasCalendarPart, MessageHeader } = messageUtils;

export default {
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapState("mail", {
            organizer: ({ consultPanel }) => consultPanel.currentEvent?.serverEvent?.value?.main?.organizer
        }),
        hasEventForward() {
            const isForward =
                this.message.headers.find(header => header.name === MessageHeader.REFERENCES) &&
                this.message.from.address !== this.organizer?.mail;
            return isForward && hasCalendarPart(this.message.structure);
        },
        alert() {
            return {
                name: "mail.notify_organizer",
                uid: `MESSAGE_NOTIFY_ORGANIZER_${this.message.key}`,
                payload: this.organizer?.commonName
            };
        }
    },
    watch: {
        organizer: {
            async handler() {
                if (this.hasEventForward && this.organizer) {
                    await this.INFO({
                        alert: this.alert,
                        options: { area: "right-panel", renderer: "NotifyOrganizerAlert" }
                    });
                } else {
                    this.REMOVE(this.alert);
                }
            },
            immediate: true
        }
    },
    methods: { ...mapActions("alert", { REMOVE, INFO }) },
    destroyed() {
        this.REMOVE(this.alert);
    },
    render() {
        return "";
    }
};
