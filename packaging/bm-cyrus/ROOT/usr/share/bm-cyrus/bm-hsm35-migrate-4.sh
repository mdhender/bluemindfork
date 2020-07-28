#!/bin/bash

set -e

if [ $EUID -ne 0 ]; then
    echo "Error: this script must be run as root"
    exit 1
fi

QUOTA_DUMP=/root/hsm_migration_quota_left.json
MIGRATED_LOG=/root/hsm_migration_migrated.log
API_URL=https://localhost/api
API_KEY=$(cat /etc/bm/bm-core.tok)
CURL="curl -s -k -H \"X-BM-ApiKey: ${API_KEY}\""

for cmd in curl jq bm-cli; do
    if ! which ${cmd} >/dev/null 2>&1; then
        echo "Command \"${cmd}\" is not installed."
        exit 1
    fi
done


[ -f "${QUOTA_DUMP}" ] && (
    echo "Command was already launched, not updating quota"
) || (
    quota -J | jq '[to_entries[] | select(.value.STORAGE.limit > 0) | {key: .key, value: (.value.STORAGE.limit -.value.STORAGE.used)}] | from_entries' >${QUOTA_DUMP}
)

domains=$(PGPASSWORD=bj psql -qtA -h localhost bj bj -c "select array_to_string(array(select name from t_domain where name != 'global.virt'), ' ');")

for domain in ${domains}; do
    echo "Migrating domain ${domain}"
    # Extract user mailbox only
    user_emails=$(jq -r -a -c '[to_entries | .[] | .key | match("user/((.+)@'${domain}')") | .captures [0].string]|sort|.[]' ${QUOTA_DUMP})
    for user_email in ${user_emails}; do
        if grep -q ${user_email} ${MIGRATED_LOG}; then
            echo "[${domain}][${user_email}] already migrated"
            continue
        fi
        echo "[${domain}][${user_email}] get bluemind user"
        user_json=$(curl -s -k -H "X-BM-ApiKey: ${API_KEY}" -XGET ${API_URL}/users/${domain}/byEmail/${user_email})
        user_values=$(echo $user_json | jq -c '.value | .quota=0')
        user_uid=$(echo $user_json | jq -c -r '.uid')
        if [ -z "$user_values" ] || [ -z "$user_uid" ]; then
            echo "[${domain}][${user_email}] Unable to find bluemind user"
            continue
        fi
        echo "[${domain}][${user_email}] Setting quota to unlimited"
        curl -s -k -H "Content-Type:application/json" -H "X-BM-ApiKey: ${API_KEY}" \
            -XPOST -d "$user_values" \
            ${API_URL}/users/${domain}/${user_uid}

        echo "[${domain}][${user_email}] HSM Migration"
        bm-cli maintenance repair --ops mailboxHsm "${user_email}"
        echo ${user_email} >> ${MIGRATED_LOG}

        echo "[${domain}][${user_email}] Retrieve used space using quota"
        used_space=$(quota -J | jq -r -c 'to_entries[] | select(.key == "user/'$user_email'") | .value.STORAGE.used')

        user_before_leftquota=$(jq -a -r -c 'to_entries[] | select(.key == "user/'$user_email'") | .value' ${QUOTA_DUMP})
        new_quota=$((${used_space} + ${user_before_leftquota}))

        new_user_values=$(echo $user_values | jq -c '.value | .quota='${new_quota})

        echo "[${domain}][${user_email}] Setting quota to ${new_quota} KiB"
        curl -s -k -H "Content-Type:application/json" -H "X-BM-ApiKey: ${API_KEY}" \
            -XPOST -d "$user_values" \
            ${API_URL}/users/${domain}/${user_uid}
    done
done
