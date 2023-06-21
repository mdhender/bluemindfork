import { Session } from "@bluemind/session";

export class MailAPI {
    static async fetchSessionInfos(): Promise<Session> {
        return fetchAPI<Session>("/session-infos");
    }
}

async function fetchAPI<T>(url: string, requestInit?: RequestInit): Promise<T> {
    const response = await fetch(url, requestInit);
    if (response.ok) {
        return response.json();
    }
    if (response.status === 401) {
        return Promise.reject(`${response.status} Unauthorized`);
    }
    return Promise.reject(`Error in BM API ${response.status}`);
}
