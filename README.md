# Camera

## 预览

<img src="/screenshot/screenshot1.png" width="33%"/><img src="/screenshot/screenshot2.png" width="33%" /><img src="/screenshot/screenshot3.png" width="33%;" />

<img src="/screenshot/screenshot4.png" width="33%;" /><img src="/screenshot/screenshot5.png" width="33%;" />

## 介绍

App 使用的是 Camera2 API，实现了简单的相机拍照、录像和相册预览的功能

图一：

- 通过右上方的按钮，可以更改拍摄的尺寸和模式，尺寸有3：4和9：16两种，模式有录像和拍照。
- 在拍照模式下，可以使用3：4或9：16的尺寸，录像仅支持9：16的尺寸
- 可以通过双指进行缩放画面
- 下方三个按钮从左至右依次为：相册、拍摄按钮和切换前后摄像头
- 相册暂时设定为只访问本应用的照片和录像，尝试过访问手机所有的照片和录像，但是因为使用的是RecyclerView，获取到照片后如果想要按照拍摄时间由新到旧的方式排序的话，会处于不断刷新的状态（可能是我照片比较多），目前没有想到比较好的处理方式

图二：

- 相册预览界面，不知道模拟机上为什么会有方向颠倒的问题，去文件管理器和自带相册里面看是正常的，在实机上看也是正常的
- 拍摄内容的存储位置设置的是 /storage/emulated/o/Pictures/MyCamera
- 在源代码的 util/FileUtil 中可以设置查找的文件地址，已经实现了文件递归查找的函数，只需要设置地址，就能得到该地址下的所有 jpg 和 mp4 文件（包括子文件夹）

图三：

- 视频的浏览界面，通过中间的播放按钮可以进行播放

图四：

- 图片的浏览界面，可以进行双指缩放和单指拖动（还没有实现边界限制）

图五：

- 视频播放器，使用的是 VideoView ，单击画面可以出现播放条

## 代码运行环境

Android Gradle Plugin Version：7.2.1

Gradle Version：7.3.3

Compile Sdk Version：32

通过根目录下的 camera.apk 可以进行安装