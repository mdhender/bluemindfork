#!/usr/bin/env python3

import gzip
import posixpath
import re
try:
    import requests
except ImportError:
    print("sudo apt install python3-requests ? ;)")
    raise
import sys
import xml.etree.ElementTree as ET
from urllib.parse import urljoin


def get(url):
    r = requests.get(url)
    if r.status_code != 200:
        print("URL {} error {}: {}".format(url, r.status_code, r.text))
        assert r.status_code != 200
    return r

def main(repomd_baseurl, postgresql_version, rhel_version):
    r = get(urljoin(repomd_baseurl, "repodata/repomd.xml"))
    xmldata = re.sub(' xmlns="[^"]+"', '', r.text, count=1)
    root = ET.fromstring(xmldata)
    primary_location = root.findall("./data[@type='primary']/location")[0].get("href")

    r = get(urljoin(repomd_baseurl, primary_location))
    primary_xmldata = re.sub(' xmlns="[^"]+"', '', gzip.decompress(r.content).decode("utf-8"), count=1)
    root = ET.fromstring(primary_xmldata)
    for pkgname, varname in (
        ("postgresql%s" % postgresql_version, "PG"),
        ("pg_repack%s" % postgresql_version, "PG_REPACK"),
        ("pg_qualstats_%s" % postgresql_version, "PG_QUALSTATS"),
        ("pg_stat_kcache_%s" % postgresql_version, "PG_STAT_KCACHE"),
        ("pg_track_settings%s" % postgresql_version, "PG_TRACK_SETTINGS"),
        ("pg_wait_sampling_%s" % postgresql_version, "PG_WAIT_SAMPLING"),
        ("hypopg_%s" % postgresql_version, "PG_HYPOPG"),
        ("powa_%s" % postgresql_version, "PG_POWA_WEB"),
        ("powa_%s" % postgresql_version, "PG_POWA"),
    ):
        pkg_versions = [
            "{}-{}".format(e.get("ver"), e.get("rel"))
            for e in root.findall("package[@type='rpm']/name/[.='{}']/../version".format(pkgname))
            if "rc" not in e.get("ver") and "beta" not in e.get("ver")
        ]
        pkg_versions.sort()
        print('{}_RHEL{}="{}"'.format(varname, rhel_version, sorted(pkg_versions)[-1]))


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(
        formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument("postgresql_version",
        help="PostgreSQL major version (eg. 12)")
    parser.add_argument("--rhel-version", default="7", help="RHEL version (eg. 7)")
    parser.add_argument("--arch", default="x86_64", help="Architecture")
    parser.add_argument("--yum-repomd-base",
        default="https://download.postgresql.org/pub/repos/yum/srpms/",
        help="Yum repository base (see default value)")
    args = parser.parse_args()
    repomd_baseurl = urljoin(
        args.yum_repomd_base,
        "{postgresql_version}/redhat/rhel-{rhel_version}-{arch}/".format(
            postgresql_version=args.postgresql_version,
            rhel_version=args.rhel_version,
            arch=args.arch)
    )
    main(
        repomd_baseurl=repomd_baseurl,
        postgresql_version=args.postgresql_version,
        rhel_version=args.rhel_version)
    sys.exit(0)
