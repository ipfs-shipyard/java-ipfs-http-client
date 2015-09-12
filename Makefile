JAVA_BUILD_OPTS = -g -source 1.8 -target 1.8 -cp .:$(CP)

.PHONY: ipfs
ipfs: 
	mkdir -p build
	echo "Name: IPFS Java HTTP Client library" > def.manifest
	echo "Build-Date: " `date` >> def.manifest
	echo "Class-Path: " $(CP_SPACE)>> def.manifest
	javac $(JAVA_BUILD_OPTS) -d build `find src -name \*.java`
	jar -cfm IPFS.jar def.manifest \
	    -C build org
	rm -f def.manifest
