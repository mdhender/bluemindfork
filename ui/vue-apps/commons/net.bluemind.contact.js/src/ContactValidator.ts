import { Contact } from "@bluemind/contact";
import { EmailValidator } from "@bluemind/email";

export default {
    validateContact(contact: Contact): boolean {
        return contact.address
            ? EmailValidator.validateAddress(contact.address)
            : contact.kind === "group"
            ? contact.members?.length || contact.memberCount
            : false;
    }
};
