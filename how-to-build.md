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

WiX Toolset v3.xがインストールされている必要があります。v4.xやv5.xはサポートされていません。

WiX v3のインストーラは https://github.com/wixtoolset/wix3/releases からダウンロードできます。

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

```
gradlew.bat winMsi
```

ビルドが成功すると、`build\distributions` ディレクトリに `logbook-kai-<version>-windows-<platform>.msi` が生成されます。
