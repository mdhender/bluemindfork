#!/bin/bash

function check_subscription {
    ret=$(bm-cli setup check-subscription)

    if [[ ${ret} == OK* ]]
    then
        arrRet=(${ret//;/ })
        nbDays=${arrRet[1]} 
        if [[ ${nbDays} -lt 5 ]]
        then
            echo "Subscription expires in "${nbDays}" days"
            exit 2
        elif [[ ${nbDays} -lt 30 ]]
        then
            echo "Subscription expires in "${nbDays}" days"
            exit 1
        else
            echo "Subscription expires in "${nbDays}" days"
            exit 0
        fi
    elif [[ ${ret} == ERROR* ]]
    then
        isNumber='^[0-9]+$'
        arrRet=(${ret//;/ })
        nbDays=${arrRet[1]} 
        if [[ "${nbDays}" == "NONE" ]]
        then
            echo "No Subscription found"
            exit 2
        elif [[ ${nbDays} =~ ${isNumber} ]]
        then
            echo "Invalid subscription expiration days: "${nbDays}
            exit 2
        fi
    else 
        echo "Unable to check subscription expiration: "${ret}
        exit 2
    fi
}

check_subscription
exit 2
