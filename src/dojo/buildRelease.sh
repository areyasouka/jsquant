#!/bin/sh

# updates compressed release under WebContent/js/release/* for deployment

cd `dirname $0`/../..
jqBase=$(cygpath -m $(pwd))
jqBase=$(pwd)

profileFile=$jqBase/src/dojo/jsquant.profile.js
releaseDir=WebContent/js/release
jqReleaseDir=$jqBase/$releaseDir

echo "svn delete $releaseDir ?"
read 
svn delete https://jsquant.googlecode.com/svn/trunk/$releaseDir -m "rem dojo"
rm -rf $releaseDir
svn update

# build release
cd $jqBase/WebContent/js/util/buildscripts/

sh -x ./build.sh profileFile="$profileFile" action=release \
    releaseDir="$jqReleaseDir/" copyTests=false optimize=shrinksafe cssOptimize=comments \
    internStrings=true || exit

find $jqReleaseDir/ -name tests -or -name demos -or -name themeTesterImages -or -name "*.commented.css" \
     -or -name "*.uncompressed.js" -or -name "*.php" -or -name "README" -or -name "LICENSE" \
     -or -name "*.txt" | xargs rm -rf

find $jqReleaseDir/dojo/dojo/nls/* -type d | xargs rm -rf
mv $jqReleaseDir/dojo/dojo/resources $jqReleaseDir/dojo/dojo-resources
mv $jqReleaseDir/dojo/dojo/nls $jqReleaseDir/dojo/dojo-nls
mv $jqReleaseDir/dojo/dojo/jsquant-all.js $jqReleaseDir/dojo/jsquant-all.js
mv $jqReleaseDir/dojo/dojo/dojo.js $jqReleaseDir/dojo/dojo.js
rm -rf $jqReleaseDir/dojo/dojo/*
mv $jqReleaseDir/dojo/dojo-resources $jqReleaseDir/dojo/dojo/resources
mv $jqReleaseDir/dojo/dojo-nls $jqReleaseDir/dojo/dojo/nls
mv $jqReleaseDir/dojo/*.js $jqReleaseDir/dojo/dojo/

rm -rf $jqReleaseDir/dojo/jsquant

find $jqReleaseDir/dojo/dojox/grid -name "*.js" -delete
find $jqReleaseDir/dojo -name "*.tar.gz" -delete
find $jqReleaseDir/dojo/dijit/themes -name "*Test*" -delete

mv $jqReleaseDir/dojo/dojox/grid $jqReleaseDir/dojo/dojox-grid
#mv $jqReleaseDir/dojo/dojox/image $jqReleaseDir/dojo/dojox-image
rm -rf $jqReleaseDir/dojo/dojox/*
mv $jqReleaseDir/dojo/dojox-grid $jqReleaseDir/dojo/dojox/grid
#mv $jqReleaseDir/dojo/dojox-image $jqReleaseDir/dojo/dojox/image

#rm -rf $jqReleaseDir/dojo/dijit/themes/claro
rm -rf $jqReleaseDir/dojo/dijit/themes/soria
rm -rf $jqReleaseDir/dojo/dijit/themes/a11y
rm -rf $jqReleaseDir/dojo/dijit/themes/nihilo
rm -rf $jqReleaseDir/dojo/dijit/themes/noir
rm -rf $jqReleaseDir/dojo/dijit/themes/tundra

mv $jqReleaseDir/dojo/dijit/themes $jqReleaseDir/dojo/dijit-themes
rm -rf $jqReleaseDir/dojo/dijit/*
mv $jqReleaseDir/dojo/dijit-themes $jqReleaseDir/dojo/dijit/themes

# when merging in dojo release from dojodev, remove svn dirs:
#find $jqBase/src/webapp/js/dijit -name "\.svn" | xargs rm -rf
#find $jqBase/src/webapp/js/dojo -name "\.svn" | xargs rm -rf
#find $jqBase/src/webapp/js/util -name "\.svn" | xargs rm -rf
#find $jqBase/src/webapp/js/dojox -name "\.svn" | xargs rm -rf