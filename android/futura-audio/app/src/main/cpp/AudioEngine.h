#pragma once

#include <oboe/Oboe.h>

#include <atomic>
#include <cstdint>
#include <memory>
#include <vector>

class AudioEngine final : public oboe::AudioStreamDataCallback {
public:
    AudioEngine() = default;
    ~AudioEngine() override;

    bool init();
    void setDataSource(const std::vector<float>& audioData);
    void play();
    void pause();
    void stop();
    bool isPlaying() const;
    int64_t getCurrentPosition() const;

    oboe::DataCallbackResult onAudioReady(
        oboe::AudioStream* oboeStream,
        void* audioData,
        int32_t numFrames
    ) override;

private:
    struct PcmBuffer {
        explicit PcmBuffer(std::vector<float> pcm) : samples(std::move(pcm)) {}

        const std::vector<float> samples;
        std::atomic<int64_t> readIndex{0};
    };

    std::shared_ptr<PcmBuffer> currentBuffer() const;

    std::shared_ptr<oboe::AudioStream> mStream;
    std::shared_ptr<PcmBuffer> mAudioData;
    std::atomic<bool> mIsPlaying{false};
    int32_t mChannelCount = 2;
};
