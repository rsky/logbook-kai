# ビルド手順

以下のコマンドを実行して、ビルドします。

OS固有の実行ファイルは、ビルドした環境のCPUアーキテクチャに依存します。

## JARファイルのビルド

### 実行可能JARファイル

```
./gradlew shadowJar
```

ビルドが成功すると、`build/libs` ディレクトリに `logbook-kai-<version>-all.jar` が生成されます。

### 実行可能JARファイルを含むzipアーカイブ

```
./gradlew package
```

ビルドが成功すると、`build/distributions` ディレクトリに `logbook-kai-<version>.zip` が生成されます。

このアーカイブに含まれる `launch.bat`, `launch.sh` は、同じディレクトリにある `config`, `release` 等のディレクトリにデータを読み書きするように設定して `logbook-kai.jar` を実行します。

## macOS用アプリケーションのビルド

Xcode Command Line Toolsがインストールされている必要があります。

### macOS用アプリケーション

```
./gradlew macApp
```

### macOS用アプリケーションを含むディスクイメージ

```
./gradlew macDmg
```

ビルドが成功すると、`build/distributions` ディレクトリに `logbook-kai-<version>-macos-<platform>.dmg` が生成されます。

## Windows用アプリケーションのビルド

### Windows用アプリケーション

```
gradlew.bat winApp
```

ビルドが成功すると、`build\distributions` ディレクトリにディレクトリ `logbook-kai` が生成されます。

`logbook-kai` は実行ファイル `logbook-kai.exe` とJavaランタイム等を含みます。

### Windows用アプリケーションを含むzipアーカイブ

```
gradlew.bat winZip
```

ビルドが成功すると、`build\distributions` ディレクトリに `logbook-kai-<version>-windows-<platform>.zip` が生成されます。

このアーカイブには `winApp` の成果物一式が含まれます。

### Windows用インストーラ

Windows用インストーラ(msi)をビルドするには、WiX Toolset v3.x, v4.x, v5.x がインストールされている必要があります。

WiX 5.xでは拡張機能 `UI.wixext` および `Util.wixext` も必要です。 WiX v6.xやv7.xはサポートされていません。

```
gradlew.bat winMsi
```

ビルドが成功すると、`build\distributions` ディレクトリに `logbook-kai-<version>-windows-<platform>.msi` が生成されます。

#### dotnetコマンドでWiX 5.xをインストールする方法

WiX 5.xの最新版5.0.2を指定します。

```
dotnet tool install --global wix --version 5.0.2
wix extension add --global WixToolset.UI.wixext/5.0.2
wix extension add --global WixToolset.Util.wixext/5.0.2
```

WiXの他のバージョンが入っている場合はトラブルを防ぐため、更新または削除しておくことをお勧めします。

```
dotnet tool update --global wix --version 5.0.2
```

## アプリケーションのビルドに失敗する場合

JDKのアップデート直後に**Gradle経由での**jpackage実行でプロセスの起動に失敗する場合はPCを再起動すると解決する場合があります。<br />
（もしかすると再ログインでも良いのかもしれませんが、未確認です。）