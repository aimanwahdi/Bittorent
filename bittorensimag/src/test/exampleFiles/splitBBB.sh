#!/bin/sh

split -n 2 Big_Buck_Bunny_1080p.avi
dd if=/dev/zero of=xaa bs=1 count=1 seek=928670753
mkdir ../BBBBegin
mv xaa ../BBBBegin/Big_Buck_Bunny_1080p.avi
mkdir ../BBBEnd
mv xab ../BBBEnd/xab
cd ../BBBEnd
touch Big_Buck_Bunny_1080p.avi
dd if=/dev/zero of=Big_Buck_Bunny_1080p.avi bs=1 count=1 seek=464335376
cat xab >> Big_Buck_Bunny_1080p.avi
rm xab
