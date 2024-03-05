import { mapActions } from "vuex";
import { inject } from "@bluemind/inject";
import { EmailExtractor } from "@bluemind/email";
import { searchVCardsHelper } from "@bluemind/contact";
import { messageUtils } from "@bluemind/mail";

const { isEventRequest } = messageUtils;

export default {
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    computed: {
        eventOrganizer() {
            return this.$store.state.mail.consultPanel?.currentEvent?.serverEvent?.value?.main?.organizer;
        },
        organizerCannotBeFound() {
            return this.eventOrganizer == null;
        },
        sender() {
            return this.message.from;
        },
        organizerHasSameEmailAddressAsSender() {
            return Boolean(this.eventOrganizer?.mailto) && this.eventOrganizer.mailto === this.sender.address;
        },
        alert() {
            return {
                name: "mail.forwarded_event",
                uid: `MESSAGE_IMIP_FORWARDED_${this.message.key}`,
                payload: {
                    organizer: this.eventOrganizer?.commonName,
                    sender: this.message.from.dn
                }
            };
        }
    },
    watch: {
        eventOrganizer: {
            async handler() {
                if (
                    isEventRequest(this.message) &&
                    (await this.eventIsForwarded()) &&
                    !this.message.eventInfo.isResourceBooking
                ) {
                    this.INFO({
                        alert: this.alert,
                        options: { area: "right-panel", renderer: "ForwardedEventAlert" }
                    });
                } else {
                    this.REMOVE(this.alert);
                }
            },
            immediate: true
        }
    },

    destroyed() {
        this.REMOVE(this.alert);
    },

    methods: {
        ...mapActions("alert", ["REMOVE", "INFO"]),
        async eventIsForwarded() {
            if (this.organizerCannotBeFound || this.organizerHasSameEmailAddressAsSender) {
                return false;
            }
            return !(await this.isOrganizerAnAliasForSender());
        },
        async isOrganizerAnAliasForSender() {
            const searchResults = await this.retreiveContactsByEmailAddress(
                EmailExtractor.extractEmail(this.sender.address)
            );
            return searchResults.some(hasAlias(this.extractUidFromDir(this.eventOrganizer?.dir), this.sender.address));
        },
        async retreiveContactsByEmailAddress(emailAddress) {
            const searchResults = await inject("AddressBooksPersistence").search(
                searchVCardsHelper(emailAddress, { size: 50 })
            );
            return searchResults.values;
        },
        extractUidFromDir(dir) {
            if (!dir) {
                return "";
            }
            return dir.split("/").pop();
        }
    },

    render() {
        return "";
    }
};

function hasAlias(organiserUid, senderEmailAddress) {
    return result => result.uid === organiserUid && result.value.mail === senderEmailAddress;
}
