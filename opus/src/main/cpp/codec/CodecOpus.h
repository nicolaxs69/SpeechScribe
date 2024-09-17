/*
 * This file is based on code from the public repository:
 * Repository: [android-opus-codec](https://github.com/theeasiestway/android-opus-codec)
 * Author: Loboda Alexey
 * Original Creation Date: 21.05.2020
 */

#ifndef OPUS_CODECOPUS_H
#define OPUS_CODECOPUS_H

#include <cstdint>
#include <vector>
#include <opus.h>

class CodecOpus {

private:
    const char *TAG = "CodecOpus";
    OpusEncoder* encoder;
    OpusDecoder* decoder;

    int decoderNumChannels = -1;

    int checkForNull(const char *methodName, bool isEncoder);

public:
    int encoderInit(int sampleRate, int numChannels, int application);
    int encoderSetBitrate(int bitrate);
    int encoderSetComplexity(int complexity);
    std::vector<uint8_t> encode(uint8_t *bytes, int frameSize);
    std::vector<short> encode(short *shorts, int length, int frameSize);
    void encoderRelease();

    int decoderInit(int sampleRate, int numChannels);
    std::vector<uint8_t> decode(uint8_t *bytes, int length, int frameSize, int fec);
    std::vector<short> decode(short *shorts, int length, int frameSize, int fec);
    void decoderRelease();

    ~CodecOpus();
};

#endif //OPUS_CODECOPUS_H
