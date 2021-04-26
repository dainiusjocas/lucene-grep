#!/usr/bin/env bash

# Test predefined analyzers
for FILE in test/resources/binary/analyzers/*;
do
  #echo "Testing analyzer:" $FILE
  ./lmgrep test/resources/test.txt --only-analyze --analysis="$(cat $FILE)" > /dev/null;
  status=$?;
  if [ $status -ne 0 ]
  then
    echo "Failed analysis with the predefined analyzer in :" $FILE
    exit 1
  fi
done

# Test char filters
for FILE in test/resources/binary/charfilters/*;
do
  #echo "Testing character filters:" $FILE
  ./lmgrep test/resources/test.txt --only-analyze --analysis="$(cat $FILE)" > /dev/null;
  status=$?;
  if [ $status -ne 0 ]
  then
    echo "Failed analysis with the character filter in :" $FILE
    exit 1
  fi
done

# Test tokenizers
for FILE in test/resources/binary/tokenizers/*;
do
  #echo "Testing tokenizers:" $FILE
  ./lmgrep test/resources/test.txt --only-analyze --analysis="$(cat $FILE)" > /dev/null;
  status=$?;
  if [ $status -ne 0 ]
  then
    echo "Failed analysis with tokenizer in :" $FILE
    exit 1
  fi
done

# Test token filters
for FILE in test/resources/binary/tokenfilters/*;
do
  # echo "Testing token filters:" $FILE
  ./lmgrep test/resources/test.txt --only-analyze --analysis="$(cat $FILE)" > /dev/null;
  status=$?;
  if [ $status -ne 0 ]
  then
    echo "Failed analysis with token filter in :" $FILE
    exit 1
  fi
done

echo "CONGRATS! All binary tests passed."