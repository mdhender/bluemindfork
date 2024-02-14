import json
import pathlib
import uuid
from datetime import datetime
from typing import Callable, NamedTuple, Optional
from functools import partial
import argparse


# Parse command line arguments
argument_parser = argparse.ArgumentParser()
argument_parser.add_argument(
    "bluemind_version",
    help="BlueMind version number",
)
args = argument_parser.parse_args()


class ParsingConfiguration(NamedTuple):
    version_parser: Callable[[str], Optional[dict]]
    component_template: dict[str, str]

def direct_version_parser(version: str) -> dict:
    """Returns the version passed as argument in the right dict format.
    No processing is done on the version number
    """
    return {"version": version}

def split_str_version_parser(split_string: str, version:str) -> Optional[dict]:
    """Splits the input verions on split_string then returns the first part
    Use partial to "pre-fill" the split_string argument.
    ex : partial(split_str_version_parser, "-something")
    """
    return direct_version_parser(version.split(split_string)[0])

def dash_bluemind_version_parser(version: str) -> Optional[dict]:
    """parses versions in the following format {ORIGINAL_VERSION}-bluemind{BUILD_NUMBER}
    returns ORIGINAL_VERSION
    example : 3.0.8-bluemind107 -> 3.0.8
    """
    return split_str_version_parser("-bluemind", version)


def cyrus_version_parser(version: str) -> Optional[dict]:
    cyrus_version = dash_bluemind_version_parser(version)
    if int(cyrus_version.split(".")[0]) >= 5:
        # Cyrus version >= 5 means there is no cyrus
        return None


def jdk_version_parser(version: str) -> Optional[dict]:
    """Parses OpenJDK versions (17.0.7+7-bluemind18 -> 17.0.7)
    """
    original_version = dash_bluemind_version_parser(version)

    if original_version is None:
        return None
    
    original_version["version"] = original_version["version"].split("+")[0]
    return original_version

base_component = {
    "version": "{version}",
    "type": "application",
}

# CPE Database https://nvd.nist.gov/products/cpe
external_deps_mapping = {
    "BMPOSTGRESQL": ParsingConfiguration(
        dash_bluemind_version_parser,
        {
            **base_component,
            "name": "PostgreSQL",
            "cpe": "cpe:2.3:a:postgresql:postgresql:{version}:*:*:*:*:*:*:*",
        },
    ),
    "KAFKA": ParsingConfiguration(
        dash_bluemind_version_parser,
        {
            **base_component,
            "name": "Kafka",
            "cpe": "cpe:2.3:a:apache:kafka:{version}:-:*:*:*:*:*:*",
        },
    ),
    "BMES": ParsingConfiguration(
        dash_bluemind_version_parser,
        {
            **base_component,
            "name": "Elasticsearch",
            "cpe": "cpe:2.3:a:elastic:elasticsearch:{version}:*:*:*:*:*:*:*",
        },
    ),
    # "MINIO": ParsingConfiguration(
    #     dash_bluemind_version_parser,
    #     {
    #         **base_component,
    #         "name": "MinIO",
    #         "cpe": "cpe:2.3:a:minio:minio:{version}:*:*:*:*:*:*:*",
    #     },
    # ),
    "BMJDK": ParsingConfiguration(
        jdk_version_parser,
        {
            **base_component,
            "name": "OpenJDK",
            "cpe": "cpe:2.3:a:oracle:openjdk:{version}:*:*:*:*:*:*:*",
        },
    ),
    "BMKEYCLOAK": ParsingConfiguration(
        dash_bluemind_version_parser,
        {
            **base_component,
            "name": "Keycloak",
            "cpe": "cpe:2.3:a:redhat:keycloak:{version}:*:*:*:*:*:*:*",
        }
    ),
    "BMNGINX": ParsingConfiguration(
        dash_bluemind_version_parser,
        {
            **base_component,
            "name": "NGINX",
            "cpe": "cpe:2.3:a:f5:nginx:{version}:*:*:*:*:*:*:*",
        }
    ),
    "BMPHP": ParsingConfiguration(
        dash_bluemind_version_parser,
        {
            **base_component,
            "name": "PHP",
            "cpe": "cpe:2.3:a:php:php:{version}:*:*:*:*:*:*:*",
        }
    ),
    "BMPHPMEMCACHE": ParsingConfiguration(
        dash_bluemind_version_parser,
        {
            **base_component,
            "name": "PHP Memcache",
            "cpe": "cpe:2.3:a:php:memcached:{version}:*:*:*:*:*:*:*",
        }
    ),
    "TELEGRAF": ParsingConfiguration(
        partial(split_str_version_parser, "-"),
        {
            **base_component,
            "name": "Telegraf",
            "cpe": "cpe:2.3:a:influxdata:telegraf:{version}:*:*:*:*:*:*:*",
        }
    ),
    "KAPACITOR": ParsingConfiguration(
        partial(split_str_version_parser,".bm4"),
        {
            **base_component,
            "name": "Kapacitor",
            "cpe": "cpe:2.3:a:influxdata:kapacitor:{version}:*:*:*:*:*:*:*",
        }
    ),
    "CHRONOGRAF": ParsingConfiguration(
        partial(split_str_version_parser,".bm4"),
        {
            **base_component,
            "name": "Chronograf",
            "cpe": "cpe:2.3:a:influxdata:chronograf:{version}:*:*:*:*:*:*:*",
        }
    ),
    "INFLUXDB": ParsingConfiguration(
        partial(split_str_version_parser,".bm4"),
        {
            **base_component,
            "name": "Chronograf",
            "cpe": "cpe:2.3:a:influxdata:chronograf:{version}:*:*:*:*:*:*:*",
        }
    ),
    "TELEMETRY": ParsingConfiguration(
        dash_bluemind_version_parser,
        {
            **base_component,
            "name": "OpenTelemetry",
            "cpe": "cpe:2.3:a:linuxfoundation:opentelemetry_instrumentation_for_java:{version}:*:*:*:*:*:*:*",
        }
    ),
    "GRAFANA": ParsingConfiguration(
        direct_version_parser,
        {
            **base_component,
            "name": "Grafana",
            "cpe": "cpe:2.3:a:grafana:grafana:{version}:*:*:*:*:*:*:*",
        }
    ), 
    "GRAFANA_MIMIR": ParsingConfiguration(
        direct_version_parser,
        {
            **base_component,
            "name": "Grafana Mimir",
            "cpe": "cpe:2.3:a:grafana:mimir:{version}:*:*:*:*:*:*:*",
        }
    ),
    "GRAFANA_TEMPO": ParsingConfiguration(
        direct_version_parser,
        {
            **base_component,
            "name": "Grafana",
            "cpe": "cpe:2.3:a:grafana:tempo:{version}:*:*:*:*:*:*:*",
        }
    ),
    "GRAFANA_AGENT": ParsingConfiguration(
        direct_version_parser,
        {
            **base_component,
            "name": "Grafana",
            "cpe": "cpe:2.3:a:grafana:agent:{version}:*:*:*:*:*:*:*",
        }
    )
}

with open("./EXTDEPS", "r") as f:
    lines = f.readlines()

bom_ref = f"extdep@{args.bluemind_version}"

bom = {
    "bomFormat": "CycloneDX",
    "specVersion": "1.4",
    "serialNumber": f"urn:uuid:{uuid.uuid4()}",
    "version": 1,
    "metadata": {
        "timestamp": f"{datetime.utcnow().isoformat()}Z",
        "tools": [
            {"vendor": "BlueMind", "name": "EXTDEPS_TO_SBOM", "version": "0.0.0"}
        ],
        "component": {
            "name": "extdeps",
            "version": args.bluemind_version,
            "type": "application",
            "bom-ref": bom_ref
        },
    },
    "components": [],
    "dependencies": [{
        "ref": bom_ref,
        "dependsOn" : []
    }],
}


for line in lines:
    variable_name: str
    parsing_data: ParsingConfiguration
    for variable_name, parsing_data in external_deps_mapping.items():
        if line.startswith(variable_name + "="):
            version_string = line[len(variable_name + "=") :].strip('"\n')
            parsed_version = parsing_data.version_parser(version_string)

            if parsed_version is None:
                continue

            component = {}
            for key, template in parsing_data.component_template.items():
                component[key] = template.format_map(parsed_version)
            component["bom-ref"] = f"{bom_ref}:{component['cpe']}"
            bom["dependencies"][0]["dependsOn"].append(component["bom-ref"])
            bom["components"].append(component)

print(json.dumps(bom))
