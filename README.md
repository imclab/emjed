What is this
------------

Emjed is a program which manages programs for embedded computers.
Written in Clojure.
Emjed requires an implementation of JRE 1.6 or higher installed and
configured on the target computer.
Emjed runs on a JVM on a target computer and give controls (i.e. installing,
uninstalling, starting, stopping and updating) of programs on them
to remote computers via tcp connection.
Emjed has an on-memory-database for storing the configuration
for programs and gives interfaces to access the database
to programs and remote TCP clients

Though emjed supports programgs written in only Clojure now,
its frames accepts other languages independent of the fact that
it is written in Clojure.
Some programming languages may be supported near future.

Usage
-----
On a target computer with JRE installed,

    java -jar embed-x.x-standalone.jar

or, on a target computer with JRE and emjed's underlying libraries
installed

    java -jar embed-x.x.jar

enables you to access tcp access to control programs.

Terms
-----
LDB:


Library Interface Reference
---------------------------
(require '[emjed.ldb :as ldb])

'kv'  is a vector of keywords. such as [:foo :bar]
'val' is a string, a number or a map.

ldb/pwd        : print the current working directory
ldb/cd path    : change directory to the specifed path
                  and load conf.json and programs.json
                 path is a relative to current working directory
                 or an absolute one,
                 in a valid form of underlying OS.

ldb/get kv     : if single value, returns it, if map? returns list of keys.
ldb/getrec kv  : whole values
ldb/set kv val :
ldb/del kv     :
ldb/rename skv dkv :

ldb/flist :
ldb/fget path : returns a byte-array
ldb/fput path body : body is a byte-array
ldb/fdel path
ldb/frename spath dpath

Network Interface Reference
---------------------------
'qk' is a qualified key, ":foo:bar"
'json-exp' is a valid json expression.

pwd
cd path
get qk         : returns a json array
getrec qk      : returns a json expression
set qk json    :
del qk         :
rename sqk dqk :
flist
fget path      : returns size, "\r\n", binary, "\r\nOK\r\n"
fput path size : follow "\r\n", binary, "\r\n". returns "OK\r\n"
fdel path
frename spath dpath

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

