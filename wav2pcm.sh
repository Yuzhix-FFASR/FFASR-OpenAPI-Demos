# !/bin/bash

if [ $# -lt 2 ]; then
    echo "too few arguments."
    echo "./wav2pcm.sh <wav_dir> <out_pcm_dir>"
    exit 1;
fi

wavDir=$1
if [ ! -d ${wavDir} ]; then
    echo "Can not find ${wavDir}."
    exit 1;
fi
pcmDir=$2

list=`ls $wavDir/*.wav`
mkdir -p ${pcmDir}
for file in ${list[@]}; do
    f=${file}
    name=`basename ${file}`
    out=${pcmDir}/${name/%wav/pcm}
    sox $f -b 16 -t raw $out || exit 1;
done

echo "All done."
