#ifndef clox_chunk_h
#define clox_chunk_h

#include "common.h"
#include "value.h"

typedef enum {
    OP_CONSTANT,
    OP_RETURN
} OpCode;

// lines field maps line numbers to indices in code field using run-length encoding
typedef struct {
    int pair_count;
    int capacity;
    int* lines;
} LineArray;

typedef struct {
    int count;
    int capacity;
    uint8_t* code;
    ValueArray constants;
    LineArray lines;
} Chunk;

void initLineArray(LineArray* array);
void freeLineArray(LineArray* array);
void writeLineArray(LineArray* array, int index, int line);
int indexToLine(LineArray* array, int index);

void initChunk(Chunk* chunk);
void freeChunk(Chunk* chunk);
void writeChunk(Chunk* chunk, uint8_t byte, int line);
int addConstant(Chunk* chunk, Value value);
int getLine(Chunk* chunk, int index);

#endif