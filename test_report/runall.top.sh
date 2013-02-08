#!/bin/bash

# Copyright 2010 - 2013, Qualcomm Innovation Center, Inc.
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

function Usage() {
	set +ex
	echo >&2 "
Runs JUnit tests with Ant

Usage: $(basename -- "$0") [ -s ] -o OS -c CPU -v VARIANT
where
	-s		# start and stop our own AllJoyn-Daemon (default internal transport address, --no-bt)
	-o OS		# same meaning as SCONS
	-c CPU
	-v VARIANT
"
	exit 2
}

: read commandline options

set -e

start_daemon=false
target_cpu=$CPU
target_os=$OS
variant=$VARIANT

while getopts sSc:o:v: option
do
	case "$option" in
	( c ) target_cpu="$OPTARG" ;;
	( o ) target_os="$OPTARG" ;;
	( v ) variant="$OPTARG" ;;
	( s ) start_daemon=true ;;
	( S ) start_daemon=false ;;
	( \? ) Usage ;;
	esac
done

: check commandline options

case "/$target_os/$target_cpu/$variant/" in ( *//* | *\ * ) echo >&2 -- "error, -o OS, -c CPU, -v VARIANT are required"; Usage;; esac

if cygpath -wa . > /dev/null 2>&1
then
	: Cygwin, which means Windows

	options='--no-bt --verbosity=5'

	bus_address="tcp:addr=127.0.0.1,port=9956"
	export USERPROFILE="$( cygpath -wa . )"
else
	: any kind of Linux

	options='--internal --no-bt --verbosity=5'

	bus_address="unix:abstract=alljoyn"
fi

daemon_bin=../../build/$target_os/$target_cpu/$variant/dist/cpp/bin
if $start_daemon
then
	ls >/dev/null -ld "$daemon_bin/alljoyn-daemon" || ls >/dev/null -ld "$daemon_bin/alljoyn-daemon.exe" || {
		echo >&2 "error, $daemon_bin/alljoyn-daemon exe not found"
		exit 2
	}
fi

alljoyn_java=..
ls -ld "$alljoyn_java/build.xml.top" || {
	echo >&2 "error, build.xml.top not found"
	exit 2
}

kill-alljoyn() {

	: kill any alljoyn-daemon

	killall -v alljoyn-daemon || : ok
	sleep 5
	killall -v alljoyn-daemon || : ok
	sleep 5
}

: begin

kill-alljoyn
rm -f alljoyn-daemon.log

if $start_daemon
then
	: start alljoyn-daemon

	(
		set -x
		cd "$daemon_bin"
		pwd
		date

		xit=0
		./alljoyn-daemon $options || xit=$?

		date
		set +x
		echo >&2 exit status $xit
	) > alljoyn-daemon.log 2>&1 </dev/null &

	sleep 5
fi

: run Ant JUnit

xit=0
ant > junit.log 2>&1 < /dev/null -f ../build.xml.top -Dtop=.. -Dalljoyn_java=. -Dtest=test_report \
	-DOS=$target_os -DCPU=$target_cpu -DVARIANT=$variant test || xit=$?

sleep 5

kill-alljoyn

echo exit status $xit
exit $xit
