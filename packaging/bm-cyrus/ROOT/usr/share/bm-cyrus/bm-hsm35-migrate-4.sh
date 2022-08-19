#!/bin/bash

set -e

shopt -s checkjobs

export MALLOC_CHECK_=0
WORKERS=1
force=0
declare -a GRPS
declare -a USERS

if [ $EUID -ne 0 ]; then
    echo "Error: this script must be run as root"
    exit 1
fi

if [ -e /etc/bm/no.mail.indexing ]; then
    echo "Error: /etc/bm/no.mail.indexing still present. Please refer to the upgrade documentation before running this script." 
    exit 2
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


usage() {
    echo "Usage: $0 [-f] [-w worker_count] [-g group] [-g group] [-u useruid...]"
    exit 1
}

while getopts "g:w:u:f" o; do
    case "${o}" in
    w)
        WORKERS="${OPTARG}"
        ;;
    g)
        GRPS+=("${OPTARG}")
        ;;
    u)
        USERS+=("${OPTARG}")
        ;;
    f)
        force=1
        ;;
    *)
        usage
        ;;
    esac
done
shift $((OPTIND-1))

# Not used anymore
replication_progress() {
    echo "Estimating the progress of replication"
    message_body_count=$(PGPASSWORD=bj psql -qtA -h localhost bj-data bj -c 'select count(*) from t_mailbox_record')
    cyrus_data_count=$(find /var/spool/cyrus/data/ -type f -links 1 -print|wc -l)
    cyrus_archive_count=$(find /var/spool/bm-hsm/cyrus-archives -type f -links 1 -print|wc -l)
    cyrus_total_count=$(($cyrus_data_count + $cyrus_archive_count))
    delta_percent=$(python -c "print(abs(int(((${message_body_count}-${cyrus_total_count})/${cyrus_data_count}.)*100)))")

    echo "Cyrus data: ${cyrus_data_count} archives: ${cyrus_archive_count} total: ${cyrus_total_count}"
    echo "Indexed count: ${message_body_count}"
    echo "Delta percent: ${delta_percent}%"
    # if [ "$delta_percent" -ge 5 ]; then
    #     echo
    #     echo "WARNING: the indexation process seems incomplete"
    #     echo "The migration process involves requesting indexes for BMARCHIVED emails."
    #     echo "If you continue, you could miss some archived emails"
    #     echo
    #     echo "NOTE: The count may be erroneous because cyrus does not remove files immediately"
    #     echo "  You can run cyr_expire -X0 in order to remove thoses files and retry the migration script again"
    #     echo
    #     echo "NOTE: you can force the reindexation of the mailspool:"
    #     echo "    bm-cli maintenance repair --ops replication.subtree [domain_uid]"
    #     echo "    bm-cli maintenance repair --ops replication.parentUid [domain_uid]"
    #     echo "  Please note this command is asynchronous, you will need to WAIT for /var/log/bm/replication.log to settle down"
    #     echo "  before running the HSM migration again."
    #     echo
    #     echo "Continue anyway ?"
    #     select yn in Yes No; do
    #         case $yn in
    #             Yes) break;;
    #             No) exit 3;;
    #         esac
    #     done
    # else
    #     echo "Indexation seems fine"
    # fi
}

get_group_members() {
    domain="$1"
    name="$2"

    group_uid=$(curl -s -k -H "X-BM-ApiKey: ${API_KEY}" -XGET ${API_URL}/groups/${domain}/byName/${name} | jq -rc '.uid')
    if [ -z "$group_uid" ]; then
        echo "Group ${name} not found in domain ${domain}" 1>&2
    else
        group_members=$(curl -s -k -H "X-BM-ApiKey: ${API_KEY}" -XGET ${API_URL}/groups/${domain}/${group_uid}/expandedmembers | jq -rc '.[] | .uid')
        echo $group_members
    fi
}

migrate_user() {
    job=$1
    domain="$2"
    user_email="$3"
    user_uid="$4"
    user_login="$5"
    quota_update="$6"

    echo "[$job][${domain}][${user_email}] get bluemind user"
    user_json=$(curl -s -k -H "X-BM-ApiKey: ${API_KEY}" -XGET ${API_URL}/users/${domain}/byEmail/${user_email})
    user_values=$(echo $user_json | jq -c '.value | .quota=0')
    if [ -z "$user_values" ] || [ -z "$user_uid" ]; then
        echo "[${domain}][${user_email}] Unable to find bluemind user"
        return
    fi
    echo "[$job][${domain}][${user_email}] Setting quota to unlimited"
    curl -s -k -H "Content-Type:application/json" -H "X-BM-ApiKey: ${API_KEY}" \
        -XPOST -d "$user_values" \
        ${API_URL}/users/${domain}/${user_uid}

    if [ "$force" -eq "1" ]; then
        echo "[$job][${domain}][${user_email}] forced repair (all ops)"
        bm-cli maintenance repair "${user_email}" || true
    else
        # Light repair
        echo "[$job][${domain}][${user_email}] Repair ops: mailboxAcls,mailboxHsm"
        bm-cli maintenance repair --ops mailboxAcls,mailboxHsm "${user_email}" || true
    fi

    echo "[$job][${domain}][${user_email}] Migrate orphaned HSM messages"
    bm-cli maintenance hsm-to-cyrus --domain ${domain} --user ${user_uid} --delete || true

    echo ${user_email} >> ${MIGRATED_LOG}
    if [ "$quota_update" -eq "1" ]; then
        echo "[$job][${domain}][${user_email}] Retrieve used space using quota"
	    used_space=$(quota -J | jq -r -c 'to_entries[] | select(.key == "user/'${user_login}@${domain}'") | .value.STORAGE.used')

    	user_before_leftquota=$(jq -a -r -c 'to_entries[] | select(.key == "user/'${user_login}@${domain}'") | .value' ${QUOTA_DUMP})
        if [ -z "${user_before_leftquota}" ] || [ -z "${used_space}" ]; then
            echo "[$job][${domain}][${user_email}] Unable to restore quota: can't calculate actual disk usage"
        else
            new_quota=$(((${used_space} + ${user_before_leftquota})/1024))
            new_user_values=$(echo $user_json | jq -c '.value | .quota='${new_quota})

            echo "[$job][${domain}][${user_email}] Setting quota to ${new_quota} KiB"
            curl -s -k -H "Content-Type:application/json" -H "X-BM-ApiKey: ${API_KEY}" \
                -XPOST -d "$new_user_values" \
                ${API_URL}/users/${domain}/${user_uid}
        fi
    fi
}

worker() {
    ID=$1
    exec 3<$FIFO
    exec 4<$FIFO_LOCK
    exec 5<$START_LOCK

    flock 5
    echo $ID >> $START
    flock -u 5
    exec 5<&-
    echo worker $ID started

    while true; do
        flock 4
        read -su 3 work_id total domain email uid login quota_update
        read_status=$?
        flock -u 4

        if [[ $read_status -eq 0 ]]; then
            echo "*** Starting for user "$((work_id+1))"/"${total}" ***"
            ( migrate_user "$work_id" "$domain" "$email" "$uid" "$login" "$quota_update" )
        else
            break
        fi
    done
    exec 3<&-
    exec 4<&-
    echo $ID "done"
}

unsetDomainMaxQuota() {
    local domain=$1

    local domainSettings_json=$(curl -s -k -H "X-BM-ApiKey: ${API_KEY}" -XGET ${API_URL}/domains/${domain}/_settings)
    local maxUserQuota=$(echo ${domainSettings_json} | jq -r -c '.mailbox_max_user_quota')

    [ ${maxUserQuota} -ne 0 ] && {
        echo "[${domain}] Unset domain user quota max - previous: ${maxUserQuota}"
        curl -s -k -H "Content-Type:application/json" -H "X-BM-ApiKey: ${API_KEY}" \
            -XPUT -d "$(echo ${domainSettings_json} | jq -c '.mailbox_max_user_quota=0')" \
            ${API_URL}/domains/${domain}/_settings
    }
}

(

echo "[START]: $(date -R) (force: $force)"

if [ "$force" -eq "1" ]; then
    echo "Removing hsm.promote.completed from all folders in /var/spool/bm-hsm/snappy/"
    find /var/spool/bm-hsm/snappy/ -type f -name hsm.promote.completed -delete
fi


[ -f "${QUOTA_DUMP}" ] && (
    echo "Command was already launched, not updating quota"
) || (
    quota -J | jq '[to_entries[] | select(.value.STORAGE.limit > 0) | {key: .key, value: (.value.STORAGE.limit -.value.STORAGE.used)}] | from_entries' >${QUOTA_DUMP}
)

domains=$(PGPASSWORD=bj psql -qtA -h localhost bj bj -c "select array_to_string(array(select name from t_domain where name != 'global.virt'), ' ');")

declare -a migration_list

for domain in ${domains}; do
    echo "Migrating domain ${domain}"

    unsetDomainMaxQuota ${domain}

    declare -a vip_uids
    if [ "${#GRPS[@]}" -gt "0" ]; then
        for grp in "${GRPS[@]}"; do
            vip_uids+=($(get_group_members "$domain" "$grp"))
        done
    fi


    if [ "${#USERS[@]}" -gt "0" ]; then
        echo "Retrieve only specified userids in bluemind"
        all_userids=("${USERS[@]}")
    else
        echo "Retrieve all userids in bluemind"
        all_userids=($(curl -s -k -H "Content-Type:application/json" -H "X-BM-ApiKey: ${API_KEY}" \
            -XGET ${API_URL}/users/${domain}/_alluids | jq -r '.[]'))
    fi

    # sort all_userids
    declare -a sorted_userids=()
    if [ "${#vip_uids[@]}" -gt 0 ]; then
        for uid in "${vip_uids[@]}"; do
            for tmpuid in "${all_userids[@]}"; do
                if [ "${tmpuid}" = "${uid}" ]; then
                    sorted_userids+=(${uid})
                    break
                fi
            done
        done
        for uid in ${all_userids[@]}; do
            found=0
            for tmpuid in "${sorted_userids[@]}"; do
                if [ "${tmpuid}" = "${uid}" ]; then
                    found=1
                    break
                fi
            done
            [ "${found}" -eq "0" ] && sorted_userids+=(${uid})
        done
    else
        sorted_userids=("${all_userids[@]}")
    fi

    echo "Retrieve all user informations"
    allusers=""
    for userid in "${sorted_userids[@]}"; do
        userinfo=$(curl -s -k -H "Content-Type:application/json" -H "X-BM-ApiKey: ${API_KEY}" \
            -XGET ${API_URL}/users/${domain}/${userid}/complete | jq -c -r '{"email": .value.emails | .[] | select(.isDefault == true).address, "uid": .uid, "login": .value.login, "archived": .value.archived}')
        allusers="$allusers $userinfo"
    done

    for userinfo in $allusers; do
        user_email=$(echo $userinfo | jq -r -c '.email')
        user_uid=$(echo $userinfo | jq -r -c '.uid')
        user_login=$(echo $userinfo | jq -r -c '.login')
        user_archived=$(echo $userinfo | jq -r -c '.archived')

        if [ "$user_archived" = "true" ]; then
            echo "[${domain}][${user_email}:${user_uid}] user archived: unable to migrate"
            continue
        fi

        if grep -q ${user_uid} ${MIGRATED_LOG} 2>/dev/null; then
            echo "[${domain}][${user_email}:${user_uid}] already migrated"
            [ "$force" -ne "1" ] && continue
        fi

        # Updating the quota for users needing it
	    if grep -q "${user_login}@${domain}" "${QUOTA_DUMP}"; then
            quota_update=1
        else
            quota_update=0
        fi
        migration_list+=("$domain $user_email $user_uid $user_login $quota_update")
    done
done


if [ "$WORKERS" -gt 1 ]; then
    START=$(mktemp -t start-XXXX)
    FIFO=$(mktemp -t fifo-XXXX)
    FIFO_LOCK=$(mktemp -t lock-XXXX)
    START_LOCK=$(mktemp -t lock-XXXX)

    rm $FIFO
    mkfifo $FIFO
    echo $FIFO

    cleanup() {
        rm $FIFO
        rm $START
        rm $FIFO_LOCK
        rm $START_LOCK
    }
    trap cleanup 0

    for ((i=1;i<=$WORKERS;i++)); do
        echo will start $i
        worker $i &
    done

    exec 3>$FIFO
    exec 4<$START_LOCK

    while true; do
        flock 4
        started=$(wc -l $START | cut -d \  -f 1)
        flock -u 4
        if [[ $started -eq $WORKERS ]]; then
            break
        else
            echo waiting, started $started of $WORKERS
        fi
    done
    exec 4<&-

    send() {
        work_id=$1; shift
        echo "$work_id" $@ 1>&3
    }

    i=0
    for u in "${migration_list[@]}"; do
        send $i ${#migration_list[@]} $u
        i=$((i+1))
    done

    exec 3<&-
    trap '' 0

    cleanup
    wait
else
    count=0
    for u in "${migration_list[@]}"; do
        count=$((count+1))
        echo "*** Starting for user "${count}"/"${#migration_list[@]}" ***"
        (migrate_user 1 $u) || echo "Migration of user $u failed"
    done
fi

echo "[END]: $(date -R)"
) 2>&1 | tee -a ${MIGRATION_LOG}

