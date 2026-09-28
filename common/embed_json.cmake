if(NOT JSON_PATH OR NOT GENERATED_HEADER OR NOT VARIABLE_PREFIX)
    message(FATAL_ERROR "embed_json.cmake requires JSON_PATH, GENERATED_HEADER, VARIABLE_PREFIX")
endif()

file(READ "${JSON_PATH}" JSON_CONTENT)
string(REPLACE "\r\n" "\n" JSON_CONTENT "${JSON_CONTENT}")
string(REGEX REPLACE "\n+$" "" JSON_CONTENT "${JSON_CONTENT}")

set(HEADER_CONTENT
"#ifndef PCBU_${VARIABLE_PREFIX}_H
#define PCBU_${VARIABLE_PREFIX}_H
#include <nlohmann/json.hpp>
inline const nlohmann::json ${VARIABLE_PREFIX}_DATA = nlohmann::json::parse(R\"json(
${JSON_CONTENT}
)json\");
#endif
")

file(WRITE "${GENERATED_HEADER}" "${HEADER_CONTENT}")
