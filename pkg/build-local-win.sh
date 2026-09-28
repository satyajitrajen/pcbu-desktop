#!/usr/bin/env bash
set -euo pipefail
export PATH="/c/Program Files (x86)/Microsoft Visual Studio/18/BuildTools/Common7/IDE/CommonExtensions/Microsoft/CMake/CMake/bin:/c/Users/satya/AppData/Local/Programs/Inno Setup 6:/usr/bin:/bin:$PATH"
export VCPKG_ROOT="D:/vcpkg"
export QT_BASE_DIR="D:/Qt/6.10.2"
export ARCH=x64
export PLATFORM=win
echo "cmake=$(command -v cmake)"
echo "iscc=$(command -v iscc || command -v ISCC || true)"
cmake --version | head -n 1
cd "$(dirname "$0")"
chmod +x build-desktop.sh
./build-desktop.sh
