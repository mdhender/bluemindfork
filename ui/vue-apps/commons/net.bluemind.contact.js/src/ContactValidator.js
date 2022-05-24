import { EmailValidator } from "@bluemind/email";

export default {
    validateContact(contact) {
        return contact.address
            ? EmailValidator.validateAddress(contact.address)
            : contact.kind === "group"
            ? contact.members?.length
            : false;
    }
};
