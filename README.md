What is this
------------

Emjed is a program which manages programs for embedded computers.
Written in Clojure.
This requires an implementation of JRE 1.6 or higher and redis-server
runnning on the target computer.
It runs on a JVM on a target computer and give controls (i.e. installing,
uninstalling, starting, stopping and updating) of programs on them
to remote computers via tcp connection.

Though emjed supports programgs written in only Clojure initially,
its frames accepts other languages independent of the fact that
it is written in Clojure.

Usage
-----
On a target computer with JRE installed,

    java -jar embed-x.x-standalone.jar

or, on a target computer with JRE and emjed's underlying libraries
installed

    java -jar embed-x.x.jar

enables you to access tcp access to control programs.

License
-------

Copyright (c) 2011, Yoshinori Kohyama (http://algobit.jp/)  
All rights reserved.  

Redistribution and use in source and binary forms, with or  
without modification, are permitted provided that the following  
conditions are met:  

Redistributions of source code must retain the above copyright  
notice, this list of conditions and the following disclaimer.  
Redistributions in binary form must reproduce the above  
copyright notice, this list of conditions and the following  
disclaimer in the documentation and/or other materials provided  
with the distribution.  
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND  
CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,  
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF  
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE  
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR  
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,  
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT  
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF  
USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED  
AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT  
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING  
IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF  
THE POSSIBILITY OF SUCH DAMAGE.  

