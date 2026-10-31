#include <jni.h>

#include <memory>
#include <mutex>
#include <vector>

#include "AudioEngine.h"

namespace {
std::unique_ptr<AudioEngine> gAudioEngine;
std::mutex gEngineMutex;

AudioEngine* getOrCreateEngine() {
    if (!gAudioEngine) {
        gAudioEngine = std::make_unique<AudioEngine>();
    }
    return gAudioEngine.get();
}
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_futura_audio_engine_OboePlayer_nativeInit(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lock(gEngineMutex);
    return getOrCreateEngine()->init() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_futura_audio_engine_OboePlayer_nativeSetDataSource(
    JNIEnv* env,
    jobject,
    jfloatArray pcmData
) {
    if (pcmData == nullptr) {
        return;
    }

    std::lock_guard<std::mutex> lock(gEngineMutex);
    AudioEngine* engine = getOrCreateEngine();
    const jsize length = env->GetArrayLength(pcmData);
    if (length < 0) {
        return;
    }

    std::vector<float> data(static_cast<size_t>(length));
    if (length > 0) {
        env->GetFloatArrayRegion(pcmData, 0, length, data.data());
        if (env->ExceptionCheck()) {
            return;
        }
    }
    engine->setDataSource(data);
}

JNIEXPORT void JNICALL
Java_com_futura_audio_engine_OboePlayer_nativePlay(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lock(gEngineMutex);
    if (gAudioEngine) {
        gAudioEngine->play();
    }
}

JNIEXPORT void JNICALL
Java_com_futura_audio_engine_OboePlayer_nativePause(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lock(gEngineMutex);
    if (gAudioEngine) {
        gAudioEngine->pause();
    }
}

JNIEXPORT void JNICALL
Java_com_futura_audio_engine_OboePlayer_nativeStop(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lock(gEngineMutex);
    if (gAudioEngine) {
        gAudioEngine->stop();
    }
}

JNIEXPORT jboolean JNICALL
Java_com_futura_audio_engine_OboePlayer_nativeIsPlaying(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lock(gEngineMutex);
    return gAudioEngine && gAudioEngine->isPlaying() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jlong JNICALL
Java_com_futura_audio_engine_OboePlayer_nativeGetCurrentPosition(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lock(gEngineMutex);
    return gAudioEngine
        ? static_cast<jlong>(gAudioEngine->getCurrentPosition())
        : 0;
}

}
