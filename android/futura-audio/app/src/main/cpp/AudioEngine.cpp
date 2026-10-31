#include "AudioEngine.h"

#include <android/log.h>

#include <algorithm>
#include <utility>

#define TAG "FuturaOboeEngine"

namespace {
constexpr int32_t kSampleRate = 44100;
}

AudioEngine::~AudioEngine() {
    stop();
}

bool AudioEngine::init() {
    if (mStream && mStream->getState() == oboe::StreamState::Started) {
        return true;
    }
    if (mStream) {
        mStream->close();
        mStream.reset();
    }

    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output)
        ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
        ->setSharingMode(oboe::SharingMode::Exclusive)
        ->setFormat(oboe::AudioFormat::Float)
        ->setChannelCount(mChannelCount)
        ->setSampleRate(kSampleRate)
        ->setDataCallback(this);

    oboe::Result result = builder.openStream(mStream);
    if (result != oboe::Result::OK) {
        __android_log_print(
            ANDROID_LOG_ERROR,
            TAG,
            "Failed to open stream. Error: %s",
            oboe::convertToText(result)
        );
        mStream.reset();
        return false;
    }

    result = mStream->requestStart();
    if (result != oboe::Result::OK) {
        __android_log_print(
            ANDROID_LOG_ERROR,
            TAG,
            "Failed to start stream. Error: %s",
            oboe::convertToText(result)
        );
        mStream->close();
        mStream.reset();
        return false;
    }

    return true;
}

void AudioEngine::setDataSource(const std::vector<float>& audioData) {
    if (audioData.size() % static_cast<size_t>(mChannelCount) != 0U) {
        __android_log_print(
            ANDROID_LOG_ERROR,
            TAG,
            "PCM buffer must contain interleaved stereo samples."
        );
        return;
    }

    mIsPlaying.store(false, std::memory_order_release);
    auto nextBuffer = std::make_shared<PcmBuffer>(audioData);
    std::atomic_store_explicit(
        &mAudioData,
        std::move(nextBuffer),
        std::memory_order_release
    );
}

void AudioEngine::play() {
    if ((!mStream || mStream->getState() != oboe::StreamState::Started) && !init()) {
        return;
    }

    const auto buffer = currentBuffer();
    if (buffer && !buffer->samples.empty()) {
        mIsPlaying.store(true, std::memory_order_release);
    }
}

void AudioEngine::pause() {
    mIsPlaying.store(false, std::memory_order_release);
}

void AudioEngine::stop() {
    mIsPlaying.store(false, std::memory_order_release);
    if (mStream) {
        mStream->requestStop();
        mStream->close();
        mStream.reset();
    }
}

bool AudioEngine::isPlaying() const {
    return mIsPlaying.load(std::memory_order_acquire);
}

int64_t AudioEngine::getCurrentPosition() const {
    const auto buffer = currentBuffer();
    if (!buffer) {
        return 0;
    }
    return buffer->readIndex.load(std::memory_order_acquire) / mChannelCount;
}

std::shared_ptr<AudioEngine::PcmBuffer> AudioEngine::currentBuffer() const {
    return std::atomic_load_explicit(&mAudioData, std::memory_order_acquire);
}

oboe::DataCallbackResult AudioEngine::onAudioReady(
    oboe::AudioStream* oboeStream,
    void* audioData,
    int32_t numFrames
) {
    auto* outputBuffer = static_cast<float*>(audioData);
    const int32_t outputChannels = oboeStream->getChannelCount();
    const int64_t outputSampleCount =
        static_cast<int64_t>(numFrames) * static_cast<int64_t>(outputChannels);

    const auto buffer = currentBuffer();
    if (!buffer ||
        buffer->samples.empty() ||
        outputChannels != mChannelCount ||
        !mIsPlaying.load(std::memory_order_acquire)) {
        std::fill_n(outputBuffer, outputSampleCount, 0.0F);
        return oboe::DataCallbackResult::Continue;
    }

    int64_t sampleIndex = buffer->readIndex.load(std::memory_order_relaxed);
    const int64_t totalSamples = static_cast<int64_t>(buffer->samples.size());

    for (int64_t i = 0; i < outputSampleCount; ++i) {
        if (sampleIndex < totalSamples) {
            outputBuffer[i] = buffer->samples[static_cast<size_t>(sampleIndex++)];
        } else {
            outputBuffer[i] = 0.0F;
            mIsPlaying.store(false, std::memory_order_release);
        }
    }

    buffer->readIndex.store(sampleIndex, std::memory_order_release);
    return oboe::DataCallbackResult::Continue;
}
