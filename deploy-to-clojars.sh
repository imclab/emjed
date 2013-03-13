#!/bin/sh
lein pom
scp pom.xml target/emjed-?.?.?.jar clojars@clojars.org:
rm pom.xml
