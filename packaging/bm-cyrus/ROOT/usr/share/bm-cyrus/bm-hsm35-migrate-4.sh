#!/bin/bash

set -e

export MALLOC_CHECK_=0

if [ $EUID -ne 0 ]; then
    echo "Error: this script must be run as root"
    exit 1
fi

if [ -e /etc/bm/no.mail.indexing ]; then
    echo "Error: /etc/bm/no.mail.indexing still present. Please refer to the upgrade documentation before running this script." 
    exit 2
fi

force=0
if [ "$1" = "-f" ] || [ "$1" = "--force" ]; then
    shift
    force=1
fi

for cmd in curl jq bm-cli; do
    if ! which ${cmd} >/dev/null 2>&1; then
        echo "Command \"${cmd}\" is not installed."
        exit 1
    fi
done

QUOTA_DUMP=/root/hsm_migration_quota_left.json
MIGRATED_LOG=/root/hsm_migration_migrated.log
MIGRATION_LOG=/root/hsm_migration.log
API_URL=https://localhost/api
API_KEY=$(cat /etc/bm/bm-core.tok)
CURL="curl -s -k -H \"X-BM-ApiKey: ${API_KEY}\""


echo "Estimating the progress of replication"
message_body_count=$(PGPASSWORD=bj psql -qtA -h localhost bj-data bj -c 'select count(*) from t_mailbox_record')
cyrus_data_count=$(find /var/spool/cyrus/data/ -type f -links 1 -print|wc -l)
cyrus_archive_count=$(find /var/spool/bm-hsm/cyrus-archives -type f -links 1 -print|wc -l)
cyrus_total_count=$(($cyrus_data_count + $cyrus_archive_count))
delta_percent=$(python -c "print(abs(int(((${message_body_count}-${cyrus_total_count})/${cyrus_data_count}.)*100)))")

echo "Cyrus data: ${cyrus_data_count} archives: ${cyrus_archive_count} total: ${cyrus_total_count}"
echo "Indexed count: ${message_body_count}"
echo "Delta percent: ${delta_percent}%"

if [ "$delta_percent" -ge 5 ]; then
    echo
    echo "WARNING: the indexation process seems incomplete"
    echo "The migration process involves requesting indexes for BMARCHIVED emails."
    echo "If you continue, you could miss some archived emails"
    echo
    echo "NOTE: The count may be erroneous because cyrus does not remove files immediately"
    echo "  You can run cyr_expire -X0 in order to remove thoses files and retry the migration script again"
    echo
    echo "NOTE: you can force the reindexation of the mailspool:"
    echo "    bm-cli maintenance repair --ops replication.subtree [domain_uid]"
    echo "    bm-cli maintenance repair --ops replication.parentUid [domain_uid]"
    echo "  Please note this command is asynchronous, you will need to WAIT for /var/log/bm/replication.log to settle down"
    echo "  before running the HSM migration again."
    echo
    echo "Continue anyway ?"
    select yn in Yes No; do
        case $yn in
            Yes) break;;
            No) exit 3;;
        esac
    done
else
    echo "Inexation seems fine"
fi

(
echo "[START]: $(date -R) (force: $force)"

if [ "$force" -ne "1" ]; then
    echo "Removing hsm.promote.completed from all folders in /var/spool/bm-hsm/snappy/"
    find /var/spool/bm-hsm/snappy/ -type f -name hsm.promote.completed -delete
fi


[ -f "${QUOTA_DUMP}" ] && (
    echo "Command was already launched, not updating quota"
) || (
    quota -J | jq '[to_entries[] | select(.value.STORAGE.limit > 0) | {key: .key, value: (.value.STORAGE.limit -.value.STORAGE.used)}] | from_entries' >${QUOTA_DUMP}
)

domains=$(PGPASSWORD=bj psql -qtA -h localhost bj bj -c "select array_to_string(array(select name from t_domain where name != 'global.virt'), ' ');")

for domain in ${domains}; do
    echo "Migrating domain ${domain}"

    echo "Retrieve all userids in bluemind"
    all_userids=$(curl -s -k -H "Content-Type:application/json" -H "X-BM-ApiKey: ${API_KEY}" \
        -XGET ${API_URL}/users/${domain}/_alluids | jq -r '.[]')
    echo "Retrieve all user informations"
    allusers=""
    for userid in $all_userids; do
        userinfo=$(curl -s -k -H "Content-Type:application/json" -H "X-BM-ApiKey: ${API_KEY}" \
            -XGET ${API_URL}/users/${domain}/${userid}/complete | jq -c -r '{"email": .value.emails | .[] | select(.isDefault == true).address, "uid": .uid}')
        allusers="$allusers $userinfo"
    done
    
    quota_user_emails=$(jq -r -a -c '[to_entries | .[] | .key | match("user/((.+)@'${domain}')") | .captures [0].string]|sort' ${QUOTA_DUMP} || true)
    for userinfo in $allusers; do
        user_email=$(echo $userinfo | jq -r -c '.email')
        user_uid=$(echo $userinfo | jq -r -c '.uid')

        if grep -q ${user_uid} ${MIGRATED_LOG} 2>/dev/null; then
            echo "[${domain}][${user_email}:${user_uid}] already migrated"
            [ "$force" -ne "1" ] && continue
        fi

        # Updating the quota for users needing it
        if echo $quota_user_emails | grep -q "$user_email"; then
            quota_update=1
        else
            quota_update=0
        fi

        if [ "$quota_update" -eq "1" ]; then
            echo "[${domain}][${user_email}] quota reset is needed"

            echo "[${domain}][${user_email}] get bluemind user"
            user_json=$(curl -s -k -H "X-BM-ApiKey: ${API_KEY}" -XGET ${API_URL}/users/${domain}/byEmail/${user_email})
            user_values=$(echo $user_json | jq -c '.value | .quota=0')
            if [ -z "$user_values" ] || [ -z "$user_uid" ]; then
                echo "[${domain}][${user_email}] Unable to find bluemind user"
                continue
            fi
            echo "[${domain}][${user_email}] Setting quota to unlimited"
            curl -s -k -H "Content-Type:application/json" -H "X-BM-ApiKey: ${API_KEY}" \
                -XPOST -d "$user_values" \
                ${API_URL}/users/${domain}/${user_uid}
        fi

        if [ "$force" -eq "1" ]; then
            echo "[${domain}][${user_email}] forced repair (all ops)"
            bm-cli maintenance repair "${user_email}"
        else
            # Light repair
            echo "[${domain}][${user_email}] Repair ops: mailboxAcls,mailboxHsm"
            bm-cli maintenance repair --ops mailboxAcls,mailboxHsm "${user_email}"
        fi

        echo "[${domain}][${user_email}] Migrate orphaned HSM messages"
        bm-cli maintenance hsm-to-cyrus --domain ${domain} --user ${user_uid} --delete

        echo ${user_email} >> ${MIGRATED_LOG}
        if [ "$quota_update" -eq "1" ]; then
            echo "[${domain}][${user_email}] Retrieve used space using quota"
            used_space=$(quota -J | jq -r -c 'to_entries[] | select(.key == "user/'$user_email'") | .value.STORAGE.used')

            user_before_leftquota=$(jq -a -r -c 'to_entries[] | select(.key == "user/'$user_email'") | .value' ${QUOTA_DUMP})
            new_quota=$(((${used_space} + ${user_before_leftquota})/1024))

            new_user_values=$(echo $user_json | jq -c '.value | .quota='${new_quota})

            echo "[${domain}][${user_email}] Setting quota to ${new_quota} KiB"
            curl -s -k -H "Content-Type:application/json" -H "X-BM-ApiKey: ${API_KEY}" \
                -XPOST -d "$user_values" \
                ${API_URL}/users/${domain}/${user_uid}
        fi
    done
done
echo "[END]: $(date -R)"
) | tee -a ${MIGRATION_LOG}
