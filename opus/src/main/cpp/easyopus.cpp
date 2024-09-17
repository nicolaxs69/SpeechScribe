/*
 * This file is based on code from the public repository:
 * Repository: [android-opus-codec](https://github.com/theeasiestway/android-opus-codec)
 * Author: Loboda Alexey
 * Original Creation Date: 13/09/2024
 */

#include <string>
#include <jni.h>
#include "codec/CodecOpus.h"
#include "utils/SamplesConverter.h"

CodecOpus codec;

// Encoding functions

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_opus_Opus_encoderInit(JNIEnv *env, jobject thiz, jint sample_rate,
                                       jint num_channels, jint application) {
    return codec.encoderInit(sample_rate, num_channels, application);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_opus_Opus_encoderSetBitrate(JNIEnv *env, jobject thiz, jint bitrate) {
    return codec.encoderSetBitrate(bitrate);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_opus_Opus_encoderSetComplexity(JNIEnv *env, jobject thiz, jint complexity) {
    return codec.encoderSetComplexity(complexity);
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_example_opus_Opus_encode___3BI(JNIEnv *env, jobject thiz, jbyteArray bytes,
                                        jint frame_size) {
    jbyte *nativeBytes = env->GetByteArrayElements(bytes, 0);
    std::vector<uint8_t> encodedData = codec.encode((uint8_t *) nativeBytes, frame_size);
    int encodedSize = encodedData.size();
    if (encodedSize <= 0) return nullptr;

    jbyteArray result = env->NewByteArray(encodedSize);
    env->SetByteArrayRegion(result, 0, encodedSize, (jbyte *) encodedData.data());
    env->ReleaseByteArrayElements(bytes, nativeBytes, 0);

    return result;
}

extern "C"
JNIEXPORT jshortArray JNICALL
Java_com_example_opus_Opus_encode___3SI(JNIEnv *env, jobject thiz, jshortArray shorts,
                                        jint frame_size) {
    jshort *nativeShorts = env->GetShortArrayElements(shorts, 0);
    jint length = env->GetArrayLength(shorts);

    std::vector<short> encodedData = codec.encode(nativeShorts, length, frame_size);
    int encodedSize = encodedData.size();
    if (encodedSize <= 0) return nullptr;

    jshortArray result = env->NewShortArray(encodedSize);
    env->SetShortArrayRegion(result, 0, encodedSize, encodedData.data());
    env->ReleaseShortArrayElements(shorts, nativeShorts, 0);

    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_opus_Opus_encoderRelease(JNIEnv *env, jobject thiz) {
    codec.encoderRelease();
}

// Decoding functions

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_opus_Opus_decoderInit(JNIEnv *env, jobject thiz, jint sample_rate,
                                       jint num_channels) {
    return codec.decoderInit(sample_rate, num_channels);
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_example_opus_Opus_decode___3BII(JNIEnv *env, jobject thiz, jbyteArray bytes,
                                         jint frame_size, jint fec) {
    jbyte *nativeBytes = env->GetByteArrayElements(bytes, 0);
    jint length = env->GetArrayLength(bytes);

    std::vector<uint8_t> encodedData = codec.decode((uint8_t *) nativeBytes, length, frame_size,
                                                    fec);
    int encodedSize = encodedData.size();
    if (encodedSize <= 0) return nullptr;

    jbyteArray result = env->NewByteArray(encodedSize);
    env->SetByteArrayRegion(result, 0, encodedSize, (jbyte *) encodedData.data());
    env->ReleaseByteArrayElements(bytes, nativeBytes, 0);

    return result;
}

extern "C"
JNIEXPORT jshortArray JNICALL
Java_com_example_opus_Opus_decode___3SII(JNIEnv *env, jobject thiz, jshortArray shorts,
                                         jint frame_size, jint fec) {
    jshort *nativeShorts = env->GetShortArrayElements(shorts, 0);
    jint length = env->GetArrayLength(shorts);

    std::vector<short> encodedData = codec.decode(nativeShorts, length, frame_size, fec);
    int encodedSize = encodedData.size();
    if (encodedSize <= 0) return nullptr;

    jshortArray result = env->NewShortArray(encodedSize);
    env->SetShortArrayRegion(result, 0, encodedSize, encodedData.data());
    env->ReleaseShortArrayElements(shorts, nativeShorts, 0);

    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_opus_Opus_decoderRelease(JNIEnv *env, jobject thiz) {
    codec.decoderRelease();
}

// Utility functions

extern "C"
JNIEXPORT jshortArray JNICALL
Java_com_example_opus_Opus_convert___3B(JNIEnv *env, jobject thiz, jbyteArray bytes) {
    uint8_t *nativeBytes = (uint8_t *) env->GetByteArrayElements(bytes, 0);
    jint length = env->GetArrayLength(bytes);

    std::vector<short> shorts = SamplesConverter::convert(&nativeBytes, length);
    int size = shorts.size();
    if (!size) return nullptr;

    jshortArray result = env->NewShortArray(size);
    env->SetShortArrayRegion(result, 0, size, shorts.data());
    env->ReleaseByteArrayElements(bytes, (jbyte *) nativeBytes, 0);

    return result;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_example_opus_Opus_convert___3S(JNIEnv *env, jobject thiz, jshortArray shorts) {
    short *nativeShorts = env->GetShortArrayElements(shorts, 0);
    jint length = env->GetArrayLength(shorts);

    std::vector<uint8_t> bytes = SamplesConverter::convert(&nativeShorts, length);
    int size = bytes.size();
    if (!size) return nullptr;

    jbyteArray result = env->NewByteArray(size);
    env->SetByteArrayRegion(result, 0, size, (jbyte *) bytes.data());
    env->ReleaseShortArrayElements(shorts, nativeShorts, 0);

    return result;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_opus_Opus_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}