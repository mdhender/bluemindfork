import EmailValidator from "../src/EmailValidator";

describe("EmailValidator", () => {
    test("valid emails", () => {
        const validEmails = ["my@mail.com", "1goodmail@example.fr"];
        validEmails.forEach(validEmail => expect(EmailValidator.validateAddress(validEmail)).toEqual(true));
    });

    test("invalid emails", () => {
        const invalidEmails = ["@mail.com", "bad.fr"];
        invalidEmails.forEach(invalidEmail => expect(EmailValidator.validateAddress(invalidEmail)).toEqual(false));
    });
});
