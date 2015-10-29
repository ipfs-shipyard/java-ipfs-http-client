JAVA_BUILD_OPTS = -g -source 1.8 -target 1.8 -cp .:$(CP)
CP = `find lib -name "*.jar" -printf %p:`
CP_SPACE = `ls lib/*.jar`

.PHONY: ipfs
ipfs: 
	rm -rf build
	mkdir -p build
	echo "Name: IPFS Java HTTP Client library" > def.manifest
	javac $(JAVA_BUILD_OPTS) -d build `find src/main/java -name \*.java`
	jar -cfm IPFS.jar def.manifest \
	    -C build org
	rm -f def.manifest

.PHONY: tests
tests: 
	rm -rf build
	mkdir -p build
	echo "Name: IPFS Java HTTP Client library Tests" > def.manifest
	echo "Class-Path: " $(CP_SPACE)>> def.manifest
	javac $(JAVA_BUILD_OPTS) -d build -cp $(CP) `find src -name \*.java`
	jar -cfm Tests.jar def.manifest \
	    -C build org
	rm -f def.manifest
	java -cp $(CP):Tests.jar org.junit.runner.JUnitCore org.ipfs.api.Test
