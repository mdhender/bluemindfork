VERSION=17

export JAVA_HOME=/usr/local/lib/jdk${VERSION}
export PATH=$JAVA_HOME/bin:$PATH
export MAVEN_OPTS="$MAVEN_OPTS -Dmaven.repo.local=.m2-${VERSION}"

mkdir -p .m2-${VERSION}
rm -rf .m2-${VERSION}/.meta/p2-artifacts.properties
