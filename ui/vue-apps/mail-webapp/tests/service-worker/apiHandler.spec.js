import apiHandler from "../../src/service-worker/workbox/apiHandler";
import { MailIDB } from "../../src/service-worker/mailIDB";
jest.mock("../../src/service-worker/mailIDB");

global.fetch = jest.fn();

describe("[SERVICE WORKER] API Handler", () => {
    beforeEach(() => {
        MailIDB.mockClear();
        fetch.mockClear();
    });

    const BASE_API_URL = "http://localhost:8080/api/";

    const generateImapRequest = {
        request: {
            url: BASE_API_URL + "mail_items/_addFlag/",
            method: "PUT"
        }
    };

    test("if one /api/ sequential request fails, following sequentials are not stopped", async () => {
        global.fetch = jest.fn(() => Promise.resolve("success"));
        await expect(apiHandler(generateImapRequest)).resolves.toBe("success");

        global.fetch = jest.fn(() => Promise.reject("error"));
        await expect(apiHandler(generateImapRequest)).rejects.toBe("error");

        global.fetch = jest.fn(() => Promise.resolve("success"));
        await expect(apiHandler(generateImapRequest)).resolves.toBe("success");
    });
});
