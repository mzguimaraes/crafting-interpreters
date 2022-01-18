SHELL = /usr/bin/env bash
CC=gcc
CFLAGS=-g -I.
SOURCE_DIR=.
BUILD_DIR=$(SOURCE_DIR)/build
OUT_DIR=$(SOURCE_DIR)/out
NAME=clox

HEADERS := $(wildcard $(SOURCE_DIR)/*.h)
SOURCES := $(wildcard $(SOURCE_DIR)/*.c)
OBJECTS := $(addprefix $(BUILD_DIR)/$(NAME)/, $(notdir $(SOURCES:.c=.o)))

$(OUT_DIR)/$(NAME): $(OBJECTS)
	@ printf "%8s %-40s %s\n" $(CC) $@ "$(CFLAGS)"
	@ mkdir -p $(OUT_DIR)
	@ $(CC) $(CFLAGS) $^ -o $@

$(BUILD_DIR)/$(NAME)/%.o: $(SOURCE_DIR)/%.c $(HEADERS)
	@ printf "%8s %-40s %s\n" $(CC) $< "$(CFLAGS)"
	@ mkdir -p $(BUILD_DIR)/$(NAME)
	@ $(CC) -c $(CFLAGS) -o $@ $<

clean:
	rm -rf $(BUILD_DIR)
	rm -rf $(OUT_DIR)