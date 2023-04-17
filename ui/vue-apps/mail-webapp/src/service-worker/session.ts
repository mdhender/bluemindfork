import { MailAPI } from "./MailAPI";
import { MailDB } from "./MailDB";
import { Session } from "@bluemind/session";
import { EnvironmentDB } from "./EnvironmentDB";

let instance: SessionWrapper | null = null;

export default class SessionWrapper {
    infos: Session;
    _api: MailAPI | null;
    _db: MailDB | null;
    _environment: EnvironmentDB | null;

    constructor(infos: Session) {
        this._api = null;
        this._db = null;
        this._environment = null;
        this.infos = infos;
    }

    get api(): MailAPI {
        if (!this._api) {
            const { sid } = this.infos;
            this._api = new MailAPI({ sid });
        }
        return this._api;
    }

    get db(): MailDB {
        if (!this._db) {
            const dbName = this.userAtDomain;
            this._db = new MailDB(dbName);
        }
        return this._db;
    }

    get environment(): EnvironmentDB {
        if (!this._environment) {
            this._environment = new EnvironmentDB();
        }
        return this._environment;
    }

    get userAtDomain(): string {
        const { userId, domain } = this.infos;
        return `user.${userId}@${domain.replace(/\./g, "_")}`;
    }

    static async instance(): Promise<SessionWrapper> {
        if (!instance) {
            const infos = await MailAPI.fetchSessionInfos();
            instance = new SessionWrapper(infos);
        }
        return instance;
    }

    static async infos(): Promise<Session> {
        return (await SessionWrapper.instance()).infos;
    }

    static async api(): Promise<MailAPI> {
        return (await SessionWrapper.instance()).api;
    }

    static async db(): Promise<MailDB> {
        return (await SessionWrapper.instance()).db;
    }

    static async environment(): Promise<EnvironmentDB> {
        return (await SessionWrapper.instance()).environment;
    }

    static async userAtDomain() {
        return (await SessionWrapper.instance()).userAtDomain;
    }

    static clear() {
        instance = null;
    }
}
