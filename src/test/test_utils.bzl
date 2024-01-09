def junit_test(test_class):
    native.java_test(
        name = test_class,
        test_class = test_class,
        runtime_deps = [
            "//src/test/java/io/ipfs/api:test-core",
        ],
        data = [
            "//src/test:test-resources",
        ],
    )
