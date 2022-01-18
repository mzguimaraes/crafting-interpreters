#include <stdlib.h>
#include <assert.h>

#include "chunk.h"
#include "memory.h"

void initLineArray(LineArray* array) {
    array->capacity = 0;
    array->pair_count = 0;
    array->lines = NULL;
}

void freeLineArray(LineArray* array) {
    FREE_ARRAY(int, array->lines, array->pair_count * 2);
    initLineArray(array);
}

int lastLine(LineArray* array) {
    assert(("Array must have entries stored", array->pair_count > 0));
    return array->lines[(array->pair_count * 2) - 1];
}

void writeLineArray(LineArray* array, int index, int line) {
    assert((
        "writeLineArray must be called with monotonically increasing indices",
         array->pair_count > 0 ? index >= lastLine(array) : true
    ));
    if (array-> pair_count == 0 || lastLine(array) != index) {
        if (array->capacity < (array->pair_count * 2) + 1) {
            // grow array
            int oldCapacity = array->capacity;
            array->capacity = GROW_CAPACITY(oldCapacity);
            array->lines = GROW_ARRAY(int, array->lines, oldCapacity, array->capacity);
        }
        // add new line-count pair
        array->lines[array->pair_count * 2] = line;
        array->lines[(array->pair_count * 2) + 1] = 1;
        array->pair_count++;
    } else {
        // increment last line-count pair
        array->lines[(array->pair_count * 2)]++;
    }
}

// [1, 1, 2, 1, 3, 1, 4, 1, 5, 1], 3 (pair_count = 5)
// [1, 3, 2, 5, 3, 1, 4, 10, 5, 8], 3 (pair_count = 5)
// [123, 2], 1 (pair_count = 1)
int indexToLine(LineArray* array, int index) {
    assert((
        "array must have line mappings stored", 
        array->lines != NULL && array->pair_count > 0
    ));

    int curr = 0;
    int idx = 0;
    while (curr < index && idx + 1 <= (array->pair_count * 2)) {
        curr += array->lines[idx + 1];
        idx += 2;
    }
    return array->lines[idx];
}

void initChunk(Chunk* chunk) {
    chunk->count = 0;
    chunk->capacity = 0;
    chunk->code = NULL;
    // chunk->lines = NULL;
    initLineArray(&chunk->lines);
    initValueArray(&chunk->constants);
}

void writeChunk(Chunk* chunk, uint8_t byte, int line) {
    if (chunk->capacity < chunk->count + 1) {
        int oldCapacity = chunk->capacity;
        chunk->capacity = GROW_CAPACITY(oldCapacity);
        chunk->code = GROW_ARRAY(uint8_t, chunk->code, oldCapacity, chunk->capacity);
        // chunk->lines = GROW_ARRAY(int, chunk->lines, oldCapacity, chunk->capacity);
    }

    chunk->code[chunk->count] = byte;
    // chunk->lines[chunk->count] = line;
    writeLineArray(&chunk->lines, chunk->count, line);
    chunk->count++;
}

void freeChunk(Chunk* chunk) {
    FREE_ARRAY(uint8_t, chunk->code, chunk->capacity);
    // FREE_ARRAY(int, chunk->lines, chunk->capacity);
    freeLineArray(&chunk->lines);
    freeValueArray(&chunk->constants);
    initChunk(chunk);
}

/**
 * @brief adds a constant to the Chunk's list of constant values.
 * 
 * @return index of the new constant in the chunk->constants array.
 */
int addConstant(Chunk* chunk, Value value) {
    writeValueArray(&chunk->constants, value);
    return chunk->constants.count - 1;
}

int getLine(Chunk* chunk, int index) {
    return indexToLine(&chunk->lines, index);
}