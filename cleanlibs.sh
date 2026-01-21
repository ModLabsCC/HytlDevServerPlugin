#!/bin/bash

# Define the target directory
TARGET_DIR="./build/libs"

# Check if the directory exists
if [ ! -d "$TARGET_DIR" ]; then
  echo "The directory $TARGET_DIR does not exist."
  exit 1
fi

# Delete all .jar files in the directory
find "$TARGET_DIR" -maxdepth 1 -type f -name "*.jar" -exec rm -f {} \;

# Confirm the operation
echo "All .jar files in $TARGET_DIR have been deleted."