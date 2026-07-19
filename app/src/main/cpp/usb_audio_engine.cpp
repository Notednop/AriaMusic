#include <jni.h>
#include <string>
#include <vector>
#include <thread>
#include <mutex>
#include <condition_variable>
#include <chrono>
#include <atomic>
#include <android/log.h>

#define LOG_TAG "iMikasa_NativeUSB"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Simple High-Performance Thread-Safe Ring Buffer (Circular Buffer)
class RingBuffer {
private:
    std::vector<uint8_t> buffer;
    size_t head = 0;
    size_t tail = 0;
    size_t size = 0;
    size_t capacity = 0;
    std::mutex mtx;
    std::condition_variable cv_read;
    std::condition_variable cv_write;

public:
    RingBuffer(size_t cap) : capacity(cap), buffer(cap) {}

    size_t write(const uint8_t* data, size_t len) {
        std::unique_lock<std::mutex> lock(mtx);
        size_t written = 0;
        while (written < len) {
            size_t available = capacity - size;
            if (available == 0) {
                // Buffer is full, wait or return written amount
                if (!cv_write.wait_for(lock, std::chrono::milliseconds(10), [this]() { return size < capacity; })) {
                    break; // timeout
                }
                available = capacity - size;
            }

            size_t to_write = std::min(len - written, available);
            size_t first_part = std::min(to_write, capacity - tail);
            std::copy(data + written, data + written + first_part, buffer.begin() + tail);
            tail = (tail + first_part) % capacity;

            if (first_part < to_write) {
                size_t second_part = to_write - first_part;
                std::copy(data + written + first_part, data + written + to_write, buffer.begin() + tail);
                tail = (tail + second_part) % capacity;
            }

            size += to_write;
            written += to_write;
            cv_read.notify_one();
        }
        return written;
    }

    size_t read(uint8_t* data, size_t len) {
        std::unique_lock<std::mutex> lock(mtx);
        size_t read_bytes = 0;
        while (read_bytes < len) {
            if (size == 0) {
                if (!cv_read.wait_for(lock, std::chrono::milliseconds(10), [this]() { return size > 0; })) {
                    break; // timeout, underrun!
                }
            }

            size_t to_read = std::min(len - read_bytes, size);
            size_t first_part = std::min(to_read, capacity - head);
            std::copy(buffer.begin() + head, buffer.begin() + head + first_part, data + read_bytes);
            head = (head + first_part) % capacity;

            if (first_part < to_read) {
                size_t second_part = to_read - first_part;
                std::copy(buffer.begin() + head, buffer.begin() + head + second_part, data + read_bytes + first_part);
                head = (head + second_part) % capacity;
            }

            size -= to_read;
            read_bytes += to_read;
            cv_write.notify_one();
        }
        return read_bytes;
    }

    size_t get_size() {
        std::lock_guard<std::mutex> lock(mtx);
        return size;
    }

    void clear() {
        std::lock_guard<std::mutex> lock(mtx);
        head = 0;
        tail = 0;
        size = 0;
    }
};

// Global variables for simulation
static RingBuffer* g_ringBuffer = nullptr;
static std::thread g_audioThread;
static std::atomic<bool> g_isPlaying(false);
static std::atomic<int> g_sampleRate(44100);
static std::atomic<int> g_bitDepth(16);
static std::atomic<int> g_bufferSize(256);
static std::atomic<int> g_underrunCount(0);

// Simulated USB Direct Isochronous endpoint transfers
void usbAudioThreadFunc() {
    LOGI("Native Audio Thread starting...");
    std::vector<uint8_t> dummyDacBuffer(4096, 0);

    while (g_isPlaying) {
        size_t bytesNeeded = g_bufferSize * (g_bitDepth / 8) * 2; // 2 channels
        if (bytesNeeded > dummyDacBuffer.size()) {
            dummyDacBuffer.resize(bytesNeeded);
        }

        size_t bytesRead = g_ringBuffer->read(dummyDacBuffer.data(), bytesNeeded);
        if (bytesRead < bytesNeeded) {
            // Underrun occurred!
            g_underrunCount++;
            LOGE("USB Native Ring Buffer Underrun! Required %zu, Got %zu. Attempting recovery...", bytesNeeded, bytesRead);
            // Auto recovery: Fill with silence and sleep briefly
            std::fill(dummyDacBuffer.begin() + bytesRead, dummyDacBuffer.begin() + bytesNeeded, 0);
            std::this_thread::sleep_for(std::chrono::milliseconds(5));
        }

        // Simulate USB Endpoint hardware transmission time
        // Calculate sleep time based on sample rate, channels, and buffer size
        double ms = (double)g_bufferSize / (double)g_sampleRate * 1000.0;
        std::this_thread::sleep_for(std::chrono::microseconds(static_cast<int>(ms * 1000.0)));
    }
    LOGI("Native Audio Thread stopping...");
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_example_audio_UsbAudioEngine_nativeInit(JNIEnv *env, jobject thiz, jint initial_capacity) {
    if (g_ringBuffer) {
        delete g_ringBuffer;
    }
    g_ringBuffer = new RingBuffer(initial_capacity);
    g_isPlaying = false;
    g_underrunCount = 0;
    LOGI("Native USB Audio Engine Initialized with capacity %d", initial_capacity);
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_example_audio_UsbAudioEngine_nativeRelease(JNIEnv *env, jobject thiz) {
    g_isPlaying = false;
    if (g_audioThread.joinable()) {
        g_audioThread.join();
    }
    if (g_ringBuffer) {
        delete g_ringBuffer;
        g_ringBuffer = nullptr;
    }
    LOGI("Native USB Audio Engine Released");
}

JNIEXPORT void JNICALL
Java_com_example_audio_UsbAudioEngine_nativeSetParameters(JNIEnv *env, jobject thiz, jint sample_rate, jint bit_depth, jint buffer_size) {
    g_sampleRate = sample_rate;
    g_bitDepth = bit_depth;
    g_bufferSize = buffer_size;
    LOGI("Native parameters updated: SampleRate=%d, BitDepth=%d, BufferSize=%d", sample_rate, bit_depth, buffer_size);
}

JNIEXPORT void JNICALL
Java_com_example_audio_UsbAudioEngine_nativeStart(JNIEnv *env, jobject thiz) {
    if (g_isPlaying) return;
    if (g_ringBuffer) {
        g_ringBuffer->clear();
    }
    g_isPlaying = true;
    g_audioThread = std::thread(usbAudioThreadFunc);
    LOGI("Native USB Audio Engine Started");
}

JNIEXPORT void JNICALL
Java_com_example_audio_UsbAudioEngine_nativeStop(JNIEnv *env, jobject thiz) {
    if (!g_isPlaying) return;
    g_isPlaying = false;
    if (g_audioThread.joinable()) {
        g_audioThread.join();
    }
    LOGI("Native USB Audio Engine Stopped");
}

JNIEXPORT jint JNICALL
Java_com_example_audio_UsbAudioEngine_nativeWrite(JNIEnv *env, jobject thiz, jbyteArray data, jint offset, jint length) {
    if (!g_ringBuffer) return 0;

    jbyte* pData = env->GetByteArrayElements(data, nullptr);
    if (!pData) return 0;

    size_t written = g_ringBuffer->write(reinterpret_cast<const uint8_t*>(pData + offset), length);

    env->ReleaseByteArrayElements(data, pData, JNI_ABORT);
    return static_cast<jint>(written);
}

JNIEXPORT jint JNICALL
Java_com_example_audio_UsbAudioEngine_nativeGetBufferUnderruns(JNIEnv *env, jobject thiz) {
    return g_underrunCount.load();
}

}
