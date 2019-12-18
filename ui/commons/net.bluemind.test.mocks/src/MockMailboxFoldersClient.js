import { MailboxFoldersClient } from "@bluemind/backend.mail.api";

const mockedMailboxFoldersClient = jest.genMockFromModule('@bluemind/backend.mail.api').MailboxFoldersClient;

Object.getOwnPropertyNames(MailboxFoldersClient.prototype).forEach(property => {
    // every function of MailboxFoldersClient is mocked and return a Promise.resolve
    if(typeof MailboxFoldersClient.prototype[property] == 'function') {
        mockedMailboxFoldersClient.prototype[property] = jest.fn().mockReturnValue(Promise.resolve());
    }
});

export default mockedMailboxFoldersClient;