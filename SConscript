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

Import('env')

# Dependent Projects
if not env.has_key('_ALLJOYNCORE_'):
    env.SConscript('../alljoyn_core/SConscript')

# Indicate that this SConscript file has been loaded already
env['_ALLJOYN_JAVA_'] = True

javaenv = env.Clone()

vars = Variables();

vars.Add(EnumVariable('JAVAVERSION', '''The version of Java pointed to by the JAVA_HOME
    environment variable. This is not used to select one version of
    the Java comiler vs. another.''', '1.6', allowed_values=('1.5', '1.6')))

vars.Update(javaenv)

Help(vars.GenerateHelpText(javaenv))


# Java requires a couple of header files in alljoyn_core/src
javaenv.Append(CPPPATH = [javaenv.Dir('../alljoyn_core/src').srcnode()])

sys.path.append(str(javaenv.Dir('../build_core/tools/scons').srcnode()))
from configurejni import ConfigureJNI

if not ConfigureJNI(javaenv):
    if not GetOption('help'):
        Exit()


classpath = os.environ.get('CLASSPATH')
if not classpath:
    if javaenv['OS'] == 'linux' and os.path.exists('/usr/share/java/junit4.jar'):
        classpath = '/usr/share/java/junit4.jar'
    else:
        print "CLASSPATH not set"
        if not GetOption('help'):
            Exit()

# Set JAVACLASSPATH to contents of CLASSPATH env variable
javaenv.AppendENVPath("JAVACLASSPATH", classpath)
javaenv['JAVACLASSPATH'] = javaenv['ENV']['JAVACLASSPATH']


# Make alljoyn_java dist a sub-directory of the alljoyn dist.  This avoids any conflicts with alljoyn dist targets.
javaenv['JAVA_DISTDIR'] = javaenv['DISTDIR'] + '/java'
javaenv['JAVA_TESTDIR'] = javaenv['TESTDIR'] + '/java'

# Tell dependent dirs where to stick classes
javaenv.Append(CLASSDIR='$OBJDIR/classes')

# Tell dependent dirs where jar files are located. ("#" doesn't work here for some reason)
javaenv.Append(JARDIR='$JAVA_DISTDIR/jar')

# Add support for mulitiple build targets in the same workset
javaenv.VariantDir('$OBJDIR', '.', duplicate = 0)


# AllJoyn Java binding
alljoyn_jar = javaenv.SConscript('src/SConscript', exports = {'env':javaenv})

# AllJoyn JNI library
libs = javaenv.SConscript('$OBJDIR/jni/SConscript', exports = {'env':javaenv})
javaenv.Install('$JAVA_DISTDIR/lib', libs)
# Also install a copy of liballjoyn_java, and junit.jar, alljoyn.jar into 
# the bin folder so it can be found by the alljoyn_java eclipse project 
javaenv.Install('bin/libs', libs)
javaenv.Install('bin/jar', alljoyn_jar)

# AllJoyn Java binding tests
javaenv.SConscript('test/SConscript', exports = {'env':javaenv})

# AllJoyn Java binding docs
javaenv['PROJECT_SHORT_NAME'] = 'AllJoyn Java API<br/>Reference Manual'
javaenv['PROJECT_LONG_NAME'] = 'AllJoyn Java API Reference Manual'
javaenv['PROJECT_NUMBER'] = 'Version 0.0.1'
javaenv.JavaDoc('$JAVA_DISTDIR/docs/html', 'src', JAVACLASSPATH=javaenv.subst('$JAVACLASSPATH'))

# AllJoyn samples
javaenv.SConscript('samples/SConscript', exports = {'env':javaenv})
