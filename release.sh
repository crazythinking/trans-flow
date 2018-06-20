sed -i "s/1.1.0-SNAPSHOT/1.1.0.RELEASE/g" pom.xml */pom.xml */*/pom.xml
# sed -i "s/trunk/tags\/3.1.RELEASE/g" pom.xml
#svn ci -m "prepare release 3.0.0.RELEASE"
# mvn clean deploy
#svn copy http://101.132.40.129/svn/repositories/smartstar/trunk/pg http://101.132.40.129/svn/repositories/smartstar/tags/pg-3.0.0.RELEASE -m "3.0.0.RELEASE"

# sed -i "s/tags\/1.0.1.RELEASE/trunk/g" pom.xml
sed -i "s/1.1.0.RELEASE/1.1.1-SNAPSHOT/g" pom.xml */pom.xml */*/pom.xml
#svn ci -m "prepare new 3.1.0-SNAPSHOT"
#svn copy http://101.132.40.129/svn/repositories/smartstar/trunk/pg http://101.132.40.129/svn/repositories/smartstar/branches/pg-3.1.0-SNAPSHOT -m "begining of 3.1.0-SNAPSHOT"
# mvn clean deploy

