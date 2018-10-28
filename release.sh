# 根据当前分支新建本地分支，并切换到新分支
git checkout -b dev-1.1.1
# 推送当前本地分支并建立本地到上游（远端）仓的链接
git push --set-upstream origin dev-1.1.1
# 批量修改pom文件版本为相应RELEASE
sed -i "s/1.1.1-SNAPSHOT/1.1.1.RELEASE/g" pom.xml */pom.xml */*/pom.xml
# 编译打包RELEASE
mvn clean deploy
# 创建相应版本的RELEASE tag
git tag -a v1.1.1.RELEASE -m "1.1.1.RELEASE版本"
# 提交tag
git push --tags
# 重新checkout到master
git checkout master
# 将master提升版本号
sed -i "s/1.1.1.RELEASE/1.1.2-SNAPSHOT/g" pom.xml */pom.xml */*/pom.xml
# 添加所有修改的内容
git add .
# 提交本地master分支
git commit -m '切换到1.1.2-SNAPSHOT'
# 提交到远程master分支
git push origin master