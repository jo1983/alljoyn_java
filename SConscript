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

import os
import sys

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
    Exit()

if not os.environ.get('CLASSPATH'):
    print "CLASSPATH not set"
    Exit()

# Dependent Projects
if not env.has_key('_ALLJOYNCORE_'):
    env.SConscript('../alljoyn_core/SConscript')

# Make alljoyn_java dist a sub-directory of the alljoyn dist.  This avoids any conflicts with alljoyn dist targets.
env['JAVA_DISTDIR'] = env['DISTDIR'] + '/java'

# Tell dependent dirs where to stick classes
env.Append(CLASSDIR='$OBJDIR/classes')

# Tell dependent dirs where jar files are located. ("#" doesn't work here for some reason)
env.Append(JARDIR='$JAVA_DISTDIR/jar')

# Add support for mulitiple build targets in the same workset
env.VariantDir('$OBJDIR', '.', duplicate = 0)

# AllJoyn Java binding
env.SConscript('src/SConscript')

# AllJoyn JNI library
libs = env.SConscript('$OBJDIR/jni/SConscript')
env.Install('$JAVA_DISTDIR/lib', libs)

# AllJoyn Java binding tests
env.SConscript('test/SConscript')

# AllJoyn Java binding docs
env['PROJECT_SHORT_NAME'] = 'AllJoyn Java API<br/>Reference Manual'
env['PROJECT_LONG_NAME'] = 'AllJoyn Java API Reference Manual'
env['PROJECT_NUMBER'] = 'Version 2.2.0'
env.JavaDoc('$JAVA_DISTDIR/docs', 'src', JAVACLASSPATH=os.environ.get('CLASSPATH'))

# AllJoyn samples
env.SConscript('samples/SConscript')

