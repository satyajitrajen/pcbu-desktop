if(NOT COMMIT_HEADER_FILE OR NOT PCBU_ROOT)
    message(FATAL_ERROR "update_commit.cmake requires COMMIT_HEADER_FILE and PCBU_ROOT")
endif()

execute_process(
    COMMAND git rev-parse --short HEAD
    WORKING_DIRECTORY "${PCBU_ROOT}"
    OUTPUT_VARIABLE GIT_COMMIT_HASH
    OUTPUT_STRIP_TRAILING_WHITESPACE
    ERROR_QUIET
)
execute_process(
    COMMAND git rev-parse --abbrev-ref HEAD
    WORKING_DIRECTORY "${PCBU_ROOT}"
    OUTPUT_VARIABLE GIT_BRANCH
    OUTPUT_STRIP_TRAILING_WHITESPACE
    ERROR_QUIET
)
if(NOT GIT_COMMIT_HASH)
    set(GIT_COMMIT_HASH "unknown")
endif()
if(NOT GIT_BRANCH)
    set(GIT_BRANCH "unknown")
endif()

file(WRITE "${COMMIT_HEADER_FILE}"
"#ifndef PCBU_COMMIT_H
#define PCBU_COMMIT_H
#include <string_view>
constexpr std::string_view GIT_COMMIT_HASH = \"${GIT_COMMIT_HASH}\";
constexpr std::string_view GIT_BRANCH = \"${GIT_BRANCH}\";
#endif
")
