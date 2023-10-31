import DependecyLocator from "../src/DependencyLocator";

describe("DependecyLocator", () => {
    test("Registering a dependency return the locator", () => {
        const provider = {
            provide: "DummyService"
        };
        const dep = DependecyLocator.register(provider);
        expect(dep).toBe(DependecyLocator);
    });

    test("Locator provide dependency", () => {
        const provider = {
            provide: "DummyService"
        };
        DependecyLocator.register(provider);
        expect(DependecyLocator.getProvider("DummyService").get()).toBe("DummyService");
    });

    test("If 'factory' is provided, Locator use factory to provide dependency", () => {
        const provider = {
            provide: "DummyService",
            factory: () => "YummyService"
        };
        DependecyLocator.register(provider);
        expect(DependecyLocator.getProvider("DummyService").get()).toBe("YummyService");
    });

    test("If 'use' is provided, Locator use it to provide dependency", () => {
        const provider = {
            provide: "DummyService",
            use: "FancyService"
        };
        DependecyLocator.register(provider);
        expect(DependecyLocator.getProvider("DummyService").get()).toBe("FancyService");
    });

    test("If 'use' and 'factory' are provided, Locator use factory to provide dependency", () => {
        const provider = {
            provide: "DummyService",
            use: "YummyService",
            factory: () => "YummyService"
        };
        DependecyLocator.register(provider);
        expect(DependecyLocator.getProvider("DummyService").get()).toBe("YummyService");
    });

    test("Locator provide class instance using class name or class def as key", () => {
        class Dummy {}

        const provider = {
            provide: Dummy
        };
        DependecyLocator.register(provider);
        expect(DependecyLocator.getProvider("Dummy").get()).toBeInstanceOf(Dummy);
        expect(DependecyLocator.getProvider(Dummy).get()).toBeInstanceOf(Dummy);
    });

    test("Locator use 'get' parameters as parameter for the 'factory'", () => {
        const provider = {
            provide: "Dummy",
            factory: (Yummy, Fancy) => {
                return { yumminess: Yummy, fanciness: Fancy };
            }
        };
        DependecyLocator.register(provider);
        expect(DependecyLocator.getProvider("Dummy").get("very", "sweet")).toEqual({
            yumminess: "very",
            fanciness: "sweet"
        });
    });

    test("Locator use dependency constructor as factory, and constructory parameters as factory parameters", () => {
        class Dummy {
            constructor(Yummy, Fancy) {
                this.yumminess = Yummy;
                this.fanciness = Fancy;
            }
        }
        const provider = {
            provide: Dummy
        };
        DependecyLocator.register(provider);
        expect(DependecyLocator.getProvider("Dummy").get("very", "sweet")).toEqual({
            yumminess: "very",
            fanciness: "sweet"
        });
    });

    test("Locator accept to register an array of providers", () => {
        const providers = ["Dummy", "Fancy", "Sweet"];
        DependecyLocator.register(providers);
        expect(DependecyLocator.getProvider("Dummy").get()).toBe("Dummy");
        expect(DependecyLocator.getProvider("Fancy").get()).toBe("Fancy");
        expect(DependecyLocator.getProvider("Sweet").get()).toBe("Sweet");
    });

    test("Locator locate dependencies to provide factory parameters", () => {
        const providers = [
            {
                provide: "Dummy",
                factory: (Yummy, Fancy) => {
                    return { yumminess: Yummy, fanciness: Fancy };
                }
            },
            { provide: "Yummy", use: "very" },
            { provide: "Fancy", factory: () => "sweet" }
        ];
        DependecyLocator.register(providers);
        expect(DependecyLocator.getProvider("Dummy").get()).toEqual({
            yumminess: "very",
            fanciness: "sweet"
        });
    });

    test("Locator factory support mix of parameters provided by locator and arguments", () => {
        const providers = [
            {
                provide: "Dummy",
                factory: (Yummy, Glossy, Fancy) => {
                    return { yumminess: Yummy, fanciness: Fancy, glossiness: Glossy };
                }
            },
            { provide: "Yummy", use: "very" },
            { provide: "Fancy", use: "sweet" }
        ];
        DependecyLocator.register(providers);
        expect(DependecyLocator.getProvider("Dummy").get("shine")).toEqual({
            yumminess: "very",
            fanciness: "sweet",
            glossiness: "shine"
        });
    });

    test("Locator dependencies resolution is recursive", () => {
        const providers = [
            {
                provide: "Dummy",
                factory: Yummy => {
                    return { ...Yummy, glossiness: "shine" };
                }
            },
            {
                provide: "Yummy",
                factory: Fancy => {
                    return {
                        yumminess: "very",
                        fanciness: Fancy
                    };
                }
            },
            { provide: "Fancy", use: "sweet" }
        ];
        DependecyLocator.register(providers);
        expect(DependecyLocator.getProvider("Dummy").get("shine")).toEqual({
            yumminess: "very",
            fanciness: "sweet",
            glossiness: "shine"
        });
    });
});
