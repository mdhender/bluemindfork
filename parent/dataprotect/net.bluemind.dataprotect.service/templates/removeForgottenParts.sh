#!/bin/bash

<#setting number_format="computer">
validId="${validPartsIds?join(" ")}"

pushd ${backupRoot}

for i in `find . -maxdepth 4 -regextype sed -regex '.*/[0-9]\+$' -type d`; do
  found=0

  for id in ${r"${validId}"}; do
    if [[ ${r"${i}"} == */${r"${id}"} ]]; then
      found=1
      break
    fi
  done

  if [ ${r"${found}"} -eq 0 ]; then
    echo "Remove ${r"$i"}"
    rm -rf ${r"$i"}
  fi
done
