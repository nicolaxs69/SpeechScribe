package com.example.opus

import android.util.Log


/*
 * This file is based on code from the public repository:
 * Repository: [android-opus-codec](https://github.com/theeasiestway/android-opus-codec)
 * Author: Loboda Alexey
 * Original Creation Date: 21.05.2020
 */

class Opus {

    companion object {

        val TAG = "CodecOpus"

        init {
            try { System.loadLibrary("easyopus") }
            catch (e: Exception) { Log.e(TAG, "Couldn't load opus library: $e") }
        }
    }

    //
    // Encoder
    //

    fun encoderInit(sampleRate: Constants.SampleRate, channels: Constants.Channels, application: Constants.Application): Int {
        return encoderInit(sampleRate.value, channels.value, application.value)
    }
    private external fun encoderInit(sampleRate: Int, numChannels: Int, application: Int): Int

    fun encoderSetBitrate(bitrate: Constants.Bitrate): Int {
        return encoderSetBitrate(bitrate.value)
    }
    private external fun encoderSetBitrate(bitrate: Int): Int

    fun encoderSetComplexity(complexity: Constants.Complexity): Int {
        return encoderSetComplexity(complexity.value)
    }
    private external fun encoderSetComplexity(complexity: Int): Int

    fun encode(bytes: ByteArray, frameSize: Constants.FrameSize): ByteArray? {
        return encode(bytes, frameSize.value)
    }
    private external fun encode(bytes: ByteArray, frameSize: Int): ByteArray?

    fun encode(shorts: ShortArray, frameSize: Constants.FrameSize): ShortArray? {
        return encode(shorts, frameSize.value)
    }
    private external fun encode(shorts: ShortArray, frameSize: Int): ShortArray?
    external fun encoderRelease()

    //
    // Decoder
    //

    fun decoderInit(sampleRate: Constants.SampleRate, channels: Constants.Channels): Int {
        return decoderInit(sampleRate.value, channels.value)
    }
    private external fun decoderInit(sampleRate: Int, numChannels: Int): Int

    fun decode(bytes: ByteArray, frameSize: Constants.FrameSize, fec: Int = 0): ByteArray? {
        return decode(bytes, frameSize.value, fec)
    }
    private external fun decode(bytes: ByteArray, frameSize: Int, fec: Int): ByteArray?

    fun decode(shorts: ShortArray, frameSize: Constants.FrameSize, fec: Int = 0): ShortArray? {
        return decode(shorts, frameSize.value, fec)
    }

    private external fun decode(shorts: ShortArray, frameSize: Int, fec: Int): ShortArray?
    external fun decoderRelease()

    //
    // Utils
    //

    external fun convert(bytes: ByteArray): ShortArray?
    external fun convert(shorts: ShortArray): ByteArray?
    external fun stringFromJNI(): String
}
