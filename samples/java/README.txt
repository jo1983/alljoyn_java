
AllJoyn Java Samples README
---------------------------

This directory contains the following set of AllJoyn usage samples:


simple -      This sample shows how to connect to a local AllJoyn daemon in 
              order to publish an AllJoyn object that implements a very simple
              interface. The sample also shows how to connect to the local
              daemon in order to execute a method on the published object.

game -        This sample shows how to implement a very simple multi-player
              game in which each player continuously broadcasts his player
              state (using AllJoyn signals) to all the other players in the game.

props -       This sample shows how to use a standard (built-in) DBus 
              interface called org.freedesktop.DBus.Properties in order to 
              publish a set of object properties with standardized 
              "getter/setter" methods. The sample also shows how to use
              the AllJoyn Variant data type.

addressbook - This sample shows how to make AllJoyn method calls that take and
              return user-defined complex data types (Java classes). This sample
              also shows how to enable authentication and encryption for a 
              given AllJoyn interface.

JavaSDKDoc  - This is a large collection of samples that were written to 
              correspond with the 'Guide to AllJoyn Development Using the Java SDK'
              document that can be found on https://www.alljoyn.org/docs-and-downloads.
              This section contains several projects that can be imported into the
              Eclipse IDE. 
