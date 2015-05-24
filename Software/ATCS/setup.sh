#!/bin/bash -e

REBOOT = false
OPENCV = false

if [ $UID -ne 0 ]
then
	echo "Becoming root"
fi

mkdir $HOME/at/

#enable SPI
if [grep -q "blacklist spi-bcm2708" "/etc/modprobe.d/raspi-blacklist.conf"]
then
	echo "SPI Blacklisted, removing..."
	sed -t -- 's/blacklist spi-bcm2708/#blacklist spi-bcm2078/g' /etc/modprobe.d/raspi-blacklist.conf
	$REBOOT = true
else
	echo "SPI Whitelisted."
fi

if [grep -q "dtparam=spi=on" "/boot/config.txt"]
then
	echo "SPI boot module not scheduled, scheduling..."
	echo "dtparam=spi=on" >> /boot/config.txt
	$REBOOT = true
else
	echo "SPI boot module already scheduled to be loaded on boot."
fi

#enable camera
CAMERA_OUTPUT = $(vcgencmd get_camera)
if [$CAMERA_OUTPUT -ne "supported=1 detected=1"]
then
	echo "WARN. Camera error on $CAMERA_OUTPUT. Run \"sudo raspi-config\" to resolve."
fi

#install packages
unset DPKG_FLAGS

apt-get update
apt-get install $(sed 's/#.*//;/^$/d' ./raspbianPackages.txt)

#opencv
echo "WARN. The OpenCV fetch/compile operations will take at least several hours."
read -p "Continue? " -n 1 -r
if [[ $REPLY =~ ^[Yy]$ ]]
then
    apt-get update
    apt-get install $(sed 's/#.*//;/^$/d' ./raspbianPackages-OpenCV.txt)

	git clone git://github.com/Itseez/opencv.git -C $HOME/at/
	git checkout 2.4 -C $HOME/at/opencv
	mkdir $HOME/at/opencv/build
	cmake -C $HOME/at/opencv/build -D CMAKE_BUILD_TYPE=RELEASE -D CMAKE_INSTALL_PREFIX=/usr/local -D BUILD_EXAMPLES=OFF ..
	make -C $HOME/at/opencv/build
	cp $HOME/at/opencv/build/lib/libopencv-java2xx.so /usr/lib/jvm/jdk-8-oracle-arm-vfp-hflt/jre/lib/arm/
else
	echo "Skipping OpenCV deploy. Pass -opencv to this script to build."
fi