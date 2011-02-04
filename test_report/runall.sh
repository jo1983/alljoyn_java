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

killall -v bbdaemon
rm -rf bbdaemon.log

if cygpath -wa . > /dev/null 2>&1
then
	: Cygwin
	export BUS_SERVER_ADDRESSES="tcp:addr=0.0.0.0,port=5342"
else
	: Linux
	export BUS_SERVER_ADDRESSES="unix:abstract=/tmp/mbustest;tcp:addr=0.0.0.0,port=5342"
fi

# set ALLJOYN_JAVA and/or ALLJOYN_DIST either through environment variables...
# .. or by putting ALLJOYN_JAVA=something and/or ALLJOYN_DIST=something as commandline parameters
while test $# -gt 0;
do
	if test -n "$1"; then
		eval "$1"
	fi
	shift
done

if test -z "$ALLJOYN_JAVA"
then
	: setting default ALLJOYN_JAVA...
	ALLJOYN_JAVA=` cd .. > /dev/null && pwd `
fi
t=` cd "$ALLJOYN_JAVA" > /dev/null && pwd `
if test -z "$t"
then
	echo >&2 "error, ALLJOYN_JAVA=$ALLJOYN_JAVA not found."
	exit 2
else
	ALLJOYN_JAVA=$t
fi

if test -z "$ALLJOYN_DIST"
then
	: setting default ALLJOYN_DIST...
	ALLJOYN_DIST=` cd "$ALLJOYN_JAVA"/../build/*/*/*/dist > /dev/null && pwd `
fi
t=` cd "$ALLJOYN_DIST" > /dev/null && pwd `
if test -z "$t"
then
	echo >&2 "error, ALLJOYN_DIST=$ALLJOYN_DIST not found."
	exit 2
else
	ALLJOYN_DIST=$t
fi

: start bbdaemon

( cd "$ALLJOYN_DIST/bin" && ls -l bbdaemon ) || { echo >&2 "error, bbdaemon not found." ; exit 2 ; }

: get target_os/target_cpu/variant
os_cpu_variant=` cd "$ALLJOYN_DIST/.." > /dev/null && pwd | awk -F/ 'NR==1 && NF>=3 { print $(NF-2) "/" $(NF-1) "/" $NF; }' `
if test -z "$os_cpu_variant"
then
	echo >&2 "error, cannot get target_os/target_cpu/variant from ALLJOYN_DIST=$ALLJOYN_DIST"
fi

(
	cd "$ALLJOYN_DIST/bin" || exit 2
	pwd
	date

	./bbdaemon ; xit=$?

	date
	set +x
	echo exit status $xit
) > bbdaemon.log 2>&1 </dev/null &

sleep 5

: run ant junit

( cd "$ALLJOYN_JAVA" && ls -l build.xml ) || { echo >&2 "error, build.xml not found." ; exit 2 ; }
ant -version || { echo >&2 "error, ant not found." ; exit 2 ; }

pwd
date

(
	test=$PWD
	build=` cd "$ALLJOYN_DIST/.." > /dev/null && pwd `
	classes="$ALLJOYN_JAVA/test/build/$os_cpu_variant/obj/classes"
	cd "$ALLJOYN_JAVA" || exit 2
	pwd
	date

	# following recipe from Todd Malsbury, 2010-08-16
	# added explicit build, dist, classes properties definitions, 2011-01-06
	if cygpath -wa . > /dev/null 2>&1
	then
		: Cygwin
		PATH="$ALLJOYN_DIST/bin:$PATH" ant \
			-Dtest="$( cygpath -wa "$test" )" \
			-Dbuild="$( cygpath -wa "$build" )" \
			-Ddist="$( cygpath -wa "$ALLJOYN_DIST/java" )" \
			-Dclasses="$( cygpath -wa "$classes" )" \
			-Dorg.alljoyn.bus.address="tcp:addr=127.0.0.1,port=5342" \
			-Dorg.alljoyn.bus.daemonaddress="tcp:addr=0.0.0.0,port=5343" \
			-Dorg.alljoyn.bus.daemonremoteaddress="tcp:addr=127.0.0.1,port=5343" \
			-Dorg.alljoyn.bus.authMechanisms=EXTERNAL \
			test
		xit=$?
	else
		: Linux
		PATH="$ALLJOYN_DIST/bin:$PATH" ant \
			-Dtest="$test" \
			-Dbuild="$build" \
			-Ddist="$ALLJOYN_DIST/java" \
			-Dclasses="$classes" \
			-Dorg.alljoyn.bus.address="unix:abstract=/tmp/mbustest" \
			-Dorg.alljoyn.bus.daemonaddress="unix:abstract=AllJoynDaemonTest" \
			-Dorg.alljoyn.bus.daemonremoteaddress="tcp:addr=127.0.0.1,port=5343" \
			-Dorg.alljoyn.bus.authMechanisms=EXTERNAL \
			test
		xit=$?
	fi

	date
	set +x
	echo exit status $xit
	exit $xit
) ; xit=$?

killall -v bbdaemon

echo exit status $xit
exit $xit
