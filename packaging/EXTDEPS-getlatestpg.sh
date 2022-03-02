#!/bin/bash

# Allow to retrieve latest packaged version of a selected PostgreSQL version.

set -e

APT_DISTNAMES="bionic focal stretch buster bullseye"  # Use space separator
RPM_RHEL_VERSIONS="7 8"  # Use space separator


PGVERSION=$1
[ ! -z "${PGVERSION}" ] && shift || PGVERSION=14
TEMPDIR=$(mktemp -d --suffix .pgdepcheck)

trap "{ rm -fr \"$TEMPDIR\"; }" EXIT

for dist in ${APT_DISTNAMES}; do
	outfile="${TEMPDIR}/Packages.${dist}"
	wget -q -O "${outfile}" "http://apt.postgresql.org/pub/repos/apt/dists/${dist}-pgdg/main/binary-amd64/Packages"
	pg_pkg_version=$(grep "Package: postgresql-${PGVERSION}$" -A5 "${outfile}" | grep 'Version:' | awk '{print $2}')
	pg_repack_version=$(grep "Package: postgresql-${PGVERSION}-repack$" -A5 "${outfile}" | grep 'Version:' | awk '{print $2}')
	pg_qualstats_version=$(grep "Package: postgresql-${PGVERSION}-pg-qualstats$" -A5 "${outfile}" | grep 'Version:' | awk '{print $2}')
	pg_kcache_version=$(grep "Package: postgresql-${PGVERSION}-pg-stat-kcache$" -A5 "${outfile}" | grep 'Version:' | awk '{print $2}')
	pg_track_settings_version=$(grep "Package: postgresql-${PGVERSION}-pg-track-settings$" -A5 "${outfile}" | grep 'Version:' | awk '{print $2}')
	pg_wait_sampling_version=$(grep "Package: postgresql-${PGVERSION}-pg-wait-sampling$" -A5 "${outfile}" | grep 'Version:' | awk '{print $2}')
	pg_hypopg_version=$(grep "Package: postgresql-${PGVERSION}-hypopg$" -A5 "${outfile}" | grep 'Version:' | awk '{print $2}')
	pg_powa_version=$(grep "Package: postgresql-${PGVERSION}-powa$" -A5 "${outfile}" | grep 'Version:' | awk '{print $2}')
	pg_common_pkg_version=$(grep 'Package: postgresql-common$' -A5 "${outfile}" | grep 'Version:' | awk '{print $2}')
	echo PG_${dist^^}=\"${pg_pkg_version}\"
	echo PGCOMMON_${dist^^}=\"${pg_common_pkg_version}\"

	echo PG_REPACK_${dist^^}=\"${pg_repack_version}\"
	echo PG_QUALSTATS_${dist^^}=\"${pg_qualstats_version}\"
	echo PG_STAT_KCACHE_${dist^^}=\"${pg_kcache_version}\"
	echo PG_TRACK_SETTINGS_${dist^^}=\"${pg_track_settings_version}\"
	echo PG_WAIT_SAMPLING_${dist^^}=\"${pg_wait_sampling_version}\"
	echo PG_HYPOPG_${dist^^}=\"${pg_hypopg_version}\"
	echo PG_POWA_${dist^^}=\"${pg_powa_version}\"
done
# Use the latest distrib to get PGKEYRING, which should be the same for all distributions
# NOTE: This is an intentional out of the loop variable use
pg_keyring_version=$(grep "Package: pgdg-keyring" -A5 "${outfile}" | grep 'Version:' | awk '{print $2}')
echo PGKEYRING=\"${pg_keyring_version}\"

# Redhat
# https://yum.postgresql.org/srpms/12/redhat/rhel-7-x86_64/
# https://download.postgresql.org/pub/repos/yum/srpms/12/redhat/rhel-7-x86_64/repodata/repomd.xml

for rhel_version in ${RPM_RHEL_VERSIONS}; do
	python3 "$(dirname $0)/EXTDEPS-getlatestpg-rpm.py" "$PGVERSION" --rhel-version "${rhel_version}" --arch x86_64
done
