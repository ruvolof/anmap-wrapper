# ANmap Wrapper

Nmap wrapper for Android

This is not an official Nmap application. To know more about Nmap and its features visit the
official homepage of the project: [https://nmap.org](https://nmap.org)

## History

In 2016 I applied for a GSoC at Nmap with a bid for an Android port of Nmap. I built an ugly
prototype with a cross-compiled Nmap shared library, a C wrapper and a simple Android activity to
interact with it. The bid was not successful, and probably for good reasons.

In 2022, I thought it would still be nice to be able to run Nmap from time to time from my
smartphone. I took the original prototype, polished it a bit and packaged it. It's still ugly, but
it does its job. And maybe angry user feedback will motivate me to spend some time on it.

## How to install it

The application is available on
[Google Play](https://play.google.com/store/apps/details?id=com.werebug.anmapwrapper).

You can also install an APK from
the [release page](https://github.com/ruvolof/anmap-wrapper/releases) here on GitHub.
However, releases here on GitHub will generally lag behind those on Google Play.

## How to contribute

Contributions are welcome.

### How to cross-compile Nmap

Be aware that the script to compile Nmap is based on my development environment. You might need to
fix some paths for it to be working on your system.

```
cd app/src/main/cpp
./make_nmap.sh
```

The script will do the following:

1) Download latest stable Nmap source and openssl source.
2) Configure and compile openssl and Nmap for `armeabi-v7a` and `arm64-v8a`, `x86` and `x86_64`.
3) Import Nmap resource files (like `nmap-services`) to Android assets directory.
4) Import NSE scripts included with Nmap source to Android assets directory.

### How to build the APK if you can't cross-compile Nmap

1) Download an apk from the [release page](https://github.com/ruvolof/anmap-wrapper/releases).
2) Extract the content of the APK.
3) Copy the `lib` folder inside the apk to `app/src/main/cpp/libs`.
4) Copy the `assets` folder inside the apk to `app/src/main/assets`.
5) Build the APK.


