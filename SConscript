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

import os
import sys

# The return value is the collection of files installed in the build destination.
returnValue = []

Import('env')

vars = Variables();

vars.Add(EnumVariable('JAVAVERSION', '''The version of Java pointed to by the JAVA_HOME
    environment variable. This is not used to select one version of
    the Java comiler vs. another.''', '1.6', allowed_values=('1.5', '1.6')))

vars.Update(env)

Help(vars.GenerateHelpText(env))

sys.path.append('../build_core/tools/scons')
from configurejni import ConfigureJNI

if not ConfigureJNI(env):
    if not GetOption('help'):
        Exit()

if not os.environ.get('CLASSPATH'):
    print "CLASSPATH not set"
    if not GetOption('help'):
        Exit()

# Dependent Projects
if not env.has_key('_ALLJOYNCORE_'):
    env.SConscript('../alljoyn_core/SConscript')

# Make alljoyn_java dist a sub-directory of the alljoyn dist.  This avoids any conflicts with alljoyn dist targets.
env['JAVA_DISTDIR'] = env['DISTDIR'] + '/java'
env['JAVA_TESTDIR'] = env['TESTDIR'] + '/java'

# Tell dependent dirs where to stick classes
env.Append(CLASSDIR='$OBJDIR/classes')

# Tell dependent dirs where jar files are located. ("#" doesn't work here for some reason)
env.Append(JARDIR='$JAVA_DISTDIR/jar')

# Add support for mulitiple build targets in the same workset
env.VariantDir('$OBJDIR', '.', duplicate = 0)


# AllJoyn Java binding
alljoyn_jar = env.SConscript('src/SConscript')

# AllJoyn JNI library
libs = env.SConscript('$OBJDIR/jni/SConscript')
returnValue += env.Install('$JAVA_DISTDIR/lib', libs)
# Also install a copy of liballjoyn_java, and junit.jar, alljoyn.jar into 
# the bin folder so it can be found by the alljoyn_java eclipse project 
env.Install('bin/libs', libs)
env.Install('bin/jar', alljoyn_jar)

# AllJoyn Java binding tests
env.SConscript('test/SConscript')

# AllJoyn Java binding docs
env['PROJECT_SHORT_NAME'] = 'AllJoyn Java API<br/>Reference Manual'
env['PROJECT_LONG_NAME'] = 'AllJoyn Java API Reference Manual'
env['PROJECT_NUMBER'] = 'Version 3.3.4'
env.JavaDoc('$JAVA_DISTDIR/docs/html', 'src', JAVACLASSPATH=env.subst('$JAVACLASSPATH'))

# AllJoyn samples
returnValue += env.SConscript('samples/SConscript')

Return('returnValue')
