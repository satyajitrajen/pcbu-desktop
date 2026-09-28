cmake_minimum_required(VERSION 3.22)
project(pcbu_common)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

find_package(Threads REQUIRED)
find_package(OpenSSL 3 REQUIRED)

add_compile_definitions("PCBU_DEBUG=$<CONFIG:Debug>")
add_compile_definitions(TARGET_ARCH="${TARGET_ARCH}")
if(WIN32)
    add_compile_definitions(WINDOWS _WIN32_WINNT=0x0A00)
    set(PLATFORM_LIBS
            crypt32
            Ws2_32
            Advapi32
            Iphlpapi
            Sensapi
            Netapi32
            Wtsapi32
            Bthprops
            ole32
            Shell32
            user32
    )
    set(PLATFORM_SRC
            src/connection/unlock/clients/BTUnlockClient.cpp
            src/connection/unlock/servers/BTUnlockServer.cpp
            src/connection/lock/LockServer.cpp
            src/connection/lock/LockServer.h
            src/platform/PlatformHelper.Win.cpp
            src/platform/BluetoothHelper.Win.cpp
            src/utils/RegistryUtils.cpp
            src/utils/RegistryUtils.h
    )
elseif(APPLE)
    add_compile_definitions(APPLE)
    enable_language(OBJCXX)
    set(PLATFORM_LIBS
            "-framework Foundation"
            "-framework CoreServices"
            "-framework SystemConfiguration"
            "-framework Carbon"
            "-framework IOKit"
            "-framework IOBluetooth"
            "-framework Security"
    )
    set(PLATFORM_SRC
            src/connection/unlock/clients/BTUnlockClient.Mac.mm
            src/connection/unlock/servers/BTUnlockServer.Mac.mm
            src/platform/PlatformHelper.Mac.cpp
            src/platform/BluetoothHelper.Mac.mm
    )
elseif(UNIX)
    find_package(PkgConfig REQUIRED)
    pkg_check_modules(libxcrypt IMPORTED_TARGET REQUIRED libxcrypt)

    add_compile_definitions(LINUX HAVE_CRYPT_H)
    set(PLATFORM_LIBS
            PkgConfig::libxcrypt
            bluetooth
    )
    set(PLATFORM_SRC
            src/connection/unlock/clients/BTUnlockClient.cpp
            src/connection/unlock/servers/BTUnlockServer.cpp
            src/platform/PlatformHelper.Linux.cpp
            src/platform/BluetoothHelper.Linux.cpp
    )
endif()

add_library(pcbu_common
        src/connection/SocketDefs.h
        src/connection/BaseConnection.cpp
        src/connection/BaseConnection.h
        src/connection/UDPBroadcaster.cpp
        src/connection/UDPBroadcaster.h
        src/connection/pairing/PairingServer.cpp
        src/connection/pairing/PairingServer.h
        src/connection/pairing/UDPPairingBroadcaster.cpp
        src/connection/pairing/UDPPairingBroadcaster.h
        src/connection/pairing/PairingStructs.h
        src/connection/unlock/BaseUnlockConnection.h
        src/connection/unlock/BaseUnlockConnection.cpp
        src/connection/unlock/UDPUnlockBroadcaster.cpp
        src/connection/unlock/UDPUnlockBroadcaster.h
        src/connection/unlock/clients/TCPUnlockClient.h
        src/connection/unlock/clients/TCPUnlockClient.cpp
        src/connection/unlock/clients/BTUnlockClient.h
        src/connection/unlock/servers/BTUnlockServer.h
        src/connection/unlock/servers/TCPUnlockServer.cpp
        src/connection/unlock/servers/TCPUnlockServer.h
        src/handler/KeyScanner.h
        src/handler/KeyScanner.cpp
        src/handler/UnlockHandler.h
        src/handler/UnlockHandler.cpp
        src/platform/PlatformHelper.h
        src/platform/NetworkHelper.cpp
        src/platform/NetworkHelper.h
        src/platform/BluetoothHelper.h
        src/shell/Shell.cpp
        src/shell/Shell.h
        src/storage/AppSettings.cpp
        src/storage/AppSettings.h
        src/storage/LoggingSystem.cpp
        src/storage/LoggingSystem.h
        src/storage/PairedDevicesStorage.cpp
        src/storage/PairedDevicesStorage.h
        src/storage/PairingMethod.h
        src/utils/CryptUtils.cpp
        src/utils/CryptUtils.h
        src/utils/StringUtils.cpp
        src/utils/StringUtils.h
        src/utils/Utils.cpp
        src/utils/Utils.h
        src/utils/AppInfo.cpp
        src/utils/AppInfo.h
        src/utils/RestClient.cpp
        src/utils/RestClient.h
        src/utils/LocaleHelper.cpp
        src/utils/LocaleHelper.h
        src/utils/I18n.cpp
        src/utils/I18n.h
        ${PLATFORM_SRC}
)
target_include_directories(pcbu_common PUBLIC
        src
        ${cpp_httplib_SOURCE_DIR}
)
target_link_libraries(pcbu_common PUBLIC
        Threads::Threads
        nlohmann_json::nlohmann_json
        spdlog::spdlog
        OpenSSL::Crypto
        OpenSSL::SSL
        Boost::system
        Boost::filesystem
        Boost::process
        Boost::asio
        ${PLATFORM_LIBS}
)

# Commit header
set(COMMIT_HEADER_FILE "${CMAKE_CURRENT_SOURCE_DIR}/src/generated/commit.h")
cmake_path(GET CMAKE_CURRENT_SOURCE_DIR PARENT_PATH PCBU_ROOT)
file(MAKE_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/src/generated)
add_custom_command(
    OUTPUT ${COMMIT_HEADER_FILE}
    COMMAND ${CMAKE_COMMAND}
            -DCOMMIT_HEADER_FILE=${COMMIT_HEADER_FILE}
            -DPCBU_ROOT=${PCBU_ROOT}
            -P ${CMAKE_CURRENT_SOURCE_DIR}/update_commit.cmake
    WORKING_DIRECTORY ${PCBU_ROOT}
    DEPENDS ${PCBU_ROOT}/.git/index ${CMAKE_CURRENT_SOURCE_DIR}/update_commit.cmake
    COMMENT "Updating commit hash"
    VERBATIM
)
add_custom_target(update_commit DEPENDS ${COMMIT_HEADER_FILE})
add_dependencies(pcbu_common update_commit)

# Json headers
function(embed_json TARGET JSON_FILE VARIABLE_PREFIX)
    get_filename_component(JSON_PATH ${JSON_FILE} ABSOLUTE)
    set(GENERATED_HEADER ${CMAKE_CURRENT_SOURCE_DIR}/src/generated/${VARIABLE_PREFIX}.h)
    file(MAKE_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/src/generated)
    add_custom_command(
        OUTPUT ${GENERATED_HEADER}
        COMMAND ${CMAKE_COMMAND}
                -DJSON_PATH=${JSON_PATH}
                -DGENERATED_HEADER=${GENERATED_HEADER}
                -DVARIABLE_PREFIX=${VARIABLE_PREFIX}
                -P ${CMAKE_CURRENT_SOURCE_DIR}/embed_json.cmake
        WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
        DEPENDS ${JSON_PATH} ${CMAKE_CURRENT_SOURCE_DIR}/embed_json.cmake
        COMMENT "Generating JSON header: ${GENERATED_HEADER}"
        VERBATIM
    )
    add_custom_target(generate_json_header_${VARIABLE_PREFIX} DEPENDS ${GENERATED_HEADER})
    add_dependencies(${TARGET} generate_json_header_${VARIABLE_PREFIX})
endfunction()
embed_json(pcbu_common res/en_US.json LANG_EN_US)
embed_json(pcbu_common res/de_DE.json LANG_DE_DE)
embed_json(pcbu_common res/zh_CN.json LANG_ZH_CN)
embed_json(pcbu_common res/pt_PT.json LANG_PT_PT)
embed_json(pcbu_common res/pt_BR.json LANG_PT_BR)
