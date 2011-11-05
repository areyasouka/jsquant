# Makefile for generating minified files

YUICOMPRESSOR_PATH=../../../src/flot/yuicompressor-2.4.2.jar

# if you need another compressor path, just copy the above line to a
# file called Makefile.local, customize it and you're good to go
-include Makefile.local

.PHONY: all

# we cheat and process all .js files instead of an exhaustive list
all: $(patsubst %.js,%.min.js,$(filter-out %.min.js,$(wildcard *.js)))

%.min.js: %.js
	java -jar $(YUICOMPRESSOR_PATH) $< -o $@
