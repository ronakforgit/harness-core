#!/usr/bin/env bash
# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

set -e

MODULES="$1"

echo "def get_sonarqube_targets_seperated():"
echo "    return {"

if [[ "$MODULES" == "below400" ]]; then
  bazel query 'attr(tags, "sonarqube", //...:*)' | cut -c 3- | rev | cut -c 11- | rev |\
    awk ' { print "        \"//"$1":sonarqube\": \""$1"\"," } ' | sed '1,/400-rest/d'
elif [[ "$MODULES" == "only400" ]]; then
  bazel query 'attr(tags, "sonarqube", //...:*)' | cut -c 3- | rev | cut -c 11- | rev |\
    awk ' { print "        \"//"$1":sonarqube\": \""$1"\"," } ' | grep 400-rest
elif [[ "$MODULES" == "above400" ]]; then
  bazel query 'attr(tags, "sonarqube", //...:*)' | cut -c 3- | rev | cut -c 11- | rev |\
    awk ' { print "        \"//"$1":sonarqube\": \""$1"\"," } ' | sed -n '/400-rest/q;p'
else
  echo "No Module Range Provided...Exiting"; exit 1
fi

echo "    }"
