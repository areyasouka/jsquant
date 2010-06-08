#!/bin/sh

# updates copy of Dojo source under WebContent/js/* for building release and javascriptDebug=true

cd `dirname $0`/../..
baseDir=$(pwd)
dojodevDir=../dojodev/
dojodevDir=../dojo-1.4/

# svn co http://svn.dojotoolkit.org/src/view/anon/all/trunk dojodev
cd $dojodevDir
svn update

cd $baseDir

rsync -rv --exclude=.svn --delete $dojodevDir/dijit/* WebContent/js/dijit/
rsync -rv --exclude=.svn --delete $dojodevDir/dojo/* WebContent/js/dojo/
rsync -rv --exclude=.svn --delete $dojodevDir/dojox/* WebContent/js/dojox/
rsync -rv --exclude=.svn --delete $dojodevDir/util/* WebContent/js/util/

