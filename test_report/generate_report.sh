#!/bin/bash

# Copyright 2010 - 2011, Qualcomm Innovation Center, Inc.
# 
#    Licensed under the Apache License, Version 2.0 (the "License");
#    you may not use this file except in compliance with the License.
#    You may obtain a copy of the License at
# 
#        http://www.apache.org/licenses/LICENSE-2.0
# 
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    See the License for the specific language governing permissions and
#    limitations under the License.
# 


set -x

test -n "$1" && title="$*" || title="Unit Test Report."

pwd
date

: re-run ant junit_report only, with noframes and optional title

ant -version || { echo >&2 "error, ant not found." ; exit 2 ; }
ls -l build_report.xml || { echo >&2 "error, build_report.xml not found." ; exit 2 ; }
ls -l data/TEST*.xml || { echo >&2 "error, data/TEST*.xml not found." ; exit 2 ; }
rm -f junit-noframes.html test_report.html || : ok

xit=0
ant -f build_report.xml -Dtitle="$title" testreport || xit=$?

mv junit-noframes.html test_report.html

date
echo exit status $xit
exit $xit
