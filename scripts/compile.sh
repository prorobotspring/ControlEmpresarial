rm -Rf ../bin/*
chmod 755 run.sh
cp -p run.sh ../bin
javac -d ../bin -sourcepath ../src -classpath ../bin:../lib/classes12.jar:../lib/weblogic.jar:$CLASSPATH ../src/**/*.java