#!/bin/bash -e

BASE=$(readlink -f $(dirname $0)/)
REBOOT=false

if [[ $UID -ne 0 ]]
then
	echo "Becoming root"
	exec sudo $0 $@
fi

mkdir -p $BASE/at/

#enable SPI
if grep -q 'blacklist spi-bcm2708' '/etc/modprobe.d/raspi-blacklist.conf'
then
	echo "SPI Blacklisted, removing..."
	sed -t -- 's/blacklist spi-bcm2708/#blacklist spi-bcm2078/g' /etc/modprobe.d/raspi-blacklist.conf
	REBOOT=true
else
	echo "SPI Whitelisted."
fi

if ! grep -q 'dtparam=spi=on' '/boot/config.txt'
then
	echo "SPI boot module not scheduled, scheduling..."
	echo "dtparam=spi=on" >> /boot/config.txt
	REBOOT=true
else
	echo "SPI boot module already scheduled to be loaded on boot."
fi

#enable camera
CAMERA_OUTPUT=$(vcgencmd get_camera)
if [ "$CAMERA_OUTPUT" != "supported=1 detected=1" ]
then
	echo "WARN. Camera error on $CAMERA_OUTPUT. Run \"sudo raspi-config\" to resolve."
fi

#install packages
unset DPKG_FLAGS

apt-get update
apt-get install -y $(sed 's/#.*//;/^$/d' $BASE/raspbianPackages.txt)

#opencv
echo "WARN. The OpenCV fetch/compile operations will take at least several hours."
read -p "Continue? " -n 1 -r
if [[ $REPLY =~ ^[Yy]$ ]]
then
	#apt-get update
	apt-get install -y $(sed 's/#.*//;/^$/d' $BASE/raspbianPackages-OpenCV.txt)

	cd $BASE/at/
	git clone git://github.com/Itseez/opencv.git
	cd opencv/
	git checkout 2.4 -C $BASE/at/opencv

	mkdir -p $BASE/at/opencv/build
	cmake -C $BASE/at/opencv/build -D CMAKE_BUILD_TYPE=RELEASE -D CMAKE_INSTALL_PREFIX=/usr/local -D BUILD_EXAMPLES=OFF ..
	make -C $BASE/at/opencv/build
	cp $HOME/at/opencv/build/lib/libopencv-java2xx.so /usr/lib/jvm/jdk-8-oracle-arm-vfp-hflt/jre/lib/arm/
else
	echo "Skipping OpenCV deploy. Pass -opencv to this script to build."
fi
