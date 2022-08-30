# scale-teacher

## 简介 Description

本应用旨在帮助视障人士独立进行尺度（包括尺寸和角度）测量与学习，也支持明眼人正常使用。

This app aims to help visually impaired persons to measure sizes and angles, as well as learn the concept of them independently. Sighted persons can also use it.



## 技术基础 Technology Base

本应用主要基于ARCore，来获取手机的三维位置坐标和旋转信息。同时也依靠TTS（Text To Speech）来播报重要信息。

This app is mainly based on the ARCore, which offers the way to get the phone's coordinates in 3-demensional space and the information of rotation. It also relys on the TTS (Text To Speech) Service to tell some important messages.



## 安装使用 Installation & Use

在本仓库的 app/release 文件夹内有应用安装包。为保证绝大部分功能可用，请保证手机已经安装ARCore和TTS服务，并允许手机使用相机和发出振动。

The .apk file is in the *app/release* folder of this repository. To have the access to most of the function, please make sure that the phone has already installed the ARCore and TTS service, and permit the phone to use camera and vibrate.

部分手机即便安装ARCore也可能因为设备不支持而无法正常使用。支持ARCore的设备可以参考[ARCore-devices/arcore_devicelist.csv at master · rolandsmeenk/ARCore-devices (github.com)](https://github.com/rolandsmeenk/ARCore-devices/blob/master/arcore_devicelist.csv)。

Some phones can't use this app correctly even if the ARCore has been installed, which is because the hardware doesn't support it. To read the list of  the devices supporting ARCore, visit the link above.