#!/bin/bash

echo 'Executing Maven build'

if [[ ! -d maven-repo ]]; then
  echo '===> Creating local Maven repository'
  mkdir maven-repo
fi

echo '===> Creating Java classpath directory'
rm -rf dependencies
mkdir dependencies

echo '===> Starting build'
mvn clean install -Dcheckstyle.skip --settings maven-settings.xml --file=../pom.xml
result=${?}
if [[ ${result} != 0 ]]; then
  echo '===> Maven build failed!'
  exit ${result}
fi
echo
echo '===> Maven build completed successfully'
echo
