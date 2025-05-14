#!/usr/bin/env sh
sips macos-icon.png -z 16 16 --out macos/Logbook-Kai.iconset/icon_16x16.png
sips macos-icon.png -z 32 32 --out macos/Logbook-Kai.iconset/icon_32x32.png
sips macos-icon.png -z 64 64 --out macos/Logbook-Kai.iconset/icon_64x64.png
sips macos-icon.png -z 128 128 --out macos/Logbook-Kai.iconset/icon_128x128.png
sips macos-icon.png -z 256 256 --out macos/Logbook-Kai.iconset/icon_256x256.png
sips macos-icon.png -z 512 512 --out macos/Logbook-Kai.iconset/icon_512x512.png
cp macos/Logbook-Kai.iconset/icon_32x32.png macos/Logbook-Kai.iconset/icon_16x16@2x.png
cp macos/Logbook-Kai.iconset/icon_64x64.png macos/Logbook-Kai.iconset/icon_32x32@2x.png
cp macos/Logbook-Kai.iconset/icon_128x128.png macos/Logbook-Kai.iconset/icon_64x64@2x.png
cp macos/Logbook-Kai.iconset/icon_256x256.png macos/Logbook-Kai.iconset/icon_128x128@2x.png
cp macos/Logbook-Kai.iconset/icon_512x512.png macos/Logbook-Kai.iconset/icon_256x256@2x.png
cp macos-icon.png macos/Logbook-Kai.iconset/iocn_512x512@2x.png
iconutil -c icns -o macos/Logbook-Kai.icns macos/Logbook-Kai.iconset
