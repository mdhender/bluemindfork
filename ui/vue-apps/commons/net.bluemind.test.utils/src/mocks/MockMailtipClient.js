import { MailTipClient } from "@bluemind/mailmessage.api";

const mockedMailtipClient = jest.genMockFromModule("@bluemind/mailmessage.api").MailTipClient;

Object.getOwnPropertyNames(MailTipClient.prototype).forEach(property => {
    if (typeof MailTipClient.prototype[property] === "function") {
        mockedMailtipClient.prototype[property] = jest.fn().mockReturnValue(Promise.resolve());
    }
});

export default mockedMailtipClient;
