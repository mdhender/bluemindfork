export class APIClient {
    // eslint-disable-next-line no-unused-vars
    constructor(sid?: String, base?: String) {}
    getMetadatas(): ClientMetadatas {
        return { className: "", packageName: "", path: { value: "", parameters: [] }, methods: [] };
    }
}

export type ClientMetadatas = EndPointMetadatas & {
    methods: Array<MethodMetadatas>;
};
export type Path = {
    value: string;
    parameters: Array<string>;
};

export type EndPointMetadatas = {
    className: string;
    packageName: string;
    path: Path;
};

export type MethodMetadatas = {
    name: string;
    verb: "GET" | "POST" | "PUT";
    path: Path;
    inParams: Array<ParameterMetadatas>;
    outParam: ParameterType;
    produce: string | undefined;
};

export type ParameterMetadatas = {
    name: string;
    type: ParameterType;
    paramType: "PathParam" | "Body" | "QueryParam";
};

export type ExecutionParameters = {
    client: Array<any>;
    method: Array<any>;
};

export type ParameterType = {
    name: string;
};
