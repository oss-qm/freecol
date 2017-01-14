
.PHONY:	all build clean

all:	build

build:
	rm -f freecol.jar debian/jh_build_stamp debian/stamp-ant-build
	fakeroot ./debian/rules binary

clean:
	find -name freecol.jar -delete
	find -name *.class -delete
