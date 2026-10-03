#include <jni.h>
#include <string>
#include <vector>
#include <atomic>
#include <memory>
#include <mutex>
#include <android/log.h>

#include "llama.h"

#define TAG "LlamaCppAndroid"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

struct LlamaNativeContext {
    std::string model_path;
    int n_threads = 4;
    int n_ctx = 2048;
    
    llama_model * model = nullptr;
    llama_context * ctx = nullptr;
    const llama_vocab * vocab = nullptr;
    llama_sampler * smpl = nullptr;
    llama_batch_ext * batch = nullptr;

    std::atomic<bool> is_loaded{false};
    std::atomic<bool> is_cancelled{false};
    std::mutex mutex;

    void cleanup() {
        if (batch) {
            llama_batch_ext_free(batch);
            batch = nullptr;
        }
        if (smpl) {
            llama_sampler_free(smpl);
            smpl = nullptr;
        }
        if (ctx) {
            llama_free(ctx);
            ctx = nullptr;
        }
        if (model) {
            llama_model_free(model);
            model = nullptr;
        }
        vocab = nullptr;
        is_loaded = false;
    }
};

static std::unique_ptr<LlamaNativeContext> g_context = nullptr;
static std::mutex g_global_mutex;

static void batch_set_tokens(llama_batch_ext * batch, const llama_token * tokens, int32_t n_tokens, llama_pos pos_0) {
    llama_batch_ext_clear(batch);
    for (int32_t i = 0; i < n_tokens; ++i) {
        const int32_t idx = llama_batch_ext_add_token(batch, 0, tokens[i]);
        const llama_pos pos = pos_0 + i;
        llama_batch_ext_set_pos(batch, idx, &pos);
    }
    llama_batch_ext_set_output_logits(batch, n_tokens - 1, true);
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_example_backend_llama_LlamaCppNative_nativeInit(
    JNIEnv *env,
    jobject /* this */
) {
    std::lock_guard<std::mutex> lock(g_global_mutex);
    LOGI("Initializing llama.cpp native backend...");
    llama_backend_init();
    if (!g_context) {
        g_context = std::make_unique<LlamaNativeContext>();
    }
    LOGI("llama.cpp backend successfully initialized.");
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_example_backend_llama_LlamaCppNative_nativeLoadModel(
    JNIEnv *env,
    jobject /* this */,
    jstring model_path_jstr,
    jint n_threads,
    jint n_ctx
) {
    std::lock_guard<std::mutex> lock(g_global_mutex);
    if (!g_context) {
        llama_backend_init();
        g_context = std::make_unique<LlamaNativeContext>();
    }

    const char *model_path_cstr = env->GetStringUTFChars(model_path_jstr, nullptr);
    if (!model_path_cstr) {
        LOGE("Failed to get model path string");
        return JNI_FALSE;
    }

    std::string path(model_path_cstr);
    env->ReleaseStringUTFChars(model_path_jstr, model_path_cstr);

    std::lock_guard<std::mutex> ctx_lock(g_context->mutex);
    g_context->cleanup();

    g_context->model_path = path;
    g_context->n_threads = n_threads > 0 ? n_threads : 4;
    // Cap default context length on mobile RAM to prevent OOM
    int actual_ctx = n_ctx > 0 ? n_ctx : 2048;
    if (actual_ctx > 4096) actual_ctx = 4096;
    g_context->n_ctx = actual_ctx;
    g_context->is_cancelled = false;

    LOGI("Loading GGUF model: %s (threads: %d, ctx: %d)",
         g_context->model_path.c_str(), g_context->n_threads, g_context->n_ctx);

    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0; // Pure CPU on Android
    model_params.load_mode = LLAMA_LOAD_MODE_MMAP;

    g_context->model = llama_model_load_from_file(g_context->model_path.c_str(), model_params);
    if (!g_context->model) {
        LOGE("Failed to load model from file: %s", g_context->model_path.c_str());
        return JNI_FALSE;
    }

    g_context->vocab = llama_model_get_vocab(g_context->model);

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = g_context->n_ctx;
    ctx_params.n_batch = 512;
    ctx_params.n_threads = g_context->n_threads;
    ctx_params.n_threads_batch = g_context->n_threads;
    ctx_params.no_perf = false;

    g_context->ctx = llama_init_from_model(g_context->model, ctx_params);
    if (!g_context->ctx) {
        LOGE("Failed to initialize llama_context from model");
        llama_model_free(g_context->model);
        g_context->model = nullptr;
        return JNI_FALSE;
    }

    g_context->batch = llama_batch_ext_init(g_context->ctx);
    g_context->is_loaded = true;
    LOGI("GGUF model successfully loaded into memory!");
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_example_backend_llama_LlamaCppNative_nativeUnloadModel(
    JNIEnv *env,
    jobject /* this */
) {
    std::lock_guard<std::mutex> lock(g_global_mutex);
    if (g_context) {
        std::lock_guard<std::mutex> ctx_lock(g_context->mutex);
        LOGI("Unloading model: %s", g_context->model_path.c_str());
        g_context->cleanup();
        g_context->model_path.clear();
    }
}

JNIEXPORT jboolean JNICALL
Java_com_example_backend_llama_LlamaCppNative_nativeIsLoaded(
    JNIEnv *env,
    jobject /* this */
) {
    if (!g_context) return JNI_FALSE;
    return g_context->is_loaded.load() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_example_backend_llama_LlamaCppNative_nativeStopGeneration(
    JNIEnv *env,
    jobject /* this */
) {
    if (g_context) {
        LOGI("Stopping generation signal requested");
        g_context->is_cancelled = true;
    }
}

JNIEXPORT jint JNICALL
Java_com_example_backend_llama_LlamaCppNative_nativeTokenize(
    JNIEnv *env,
    jobject /* this */,
    jstring text_jstr
) {
    if (!g_context || !g_context->is_loaded.load() || !g_context->vocab) {
        return 0;
    }

    const char *text_cstr = env->GetStringUTFChars(text_jstr, nullptr);
    if (!text_cstr) return 0;

    std::string text(text_cstr);
    env->ReleaseStringUTFChars(text_jstr, text_cstr);

    int n_tokens = -llama_tokenize(g_context->vocab, text.c_str(), text.size(), nullptr, 0, true, true);
    if (n_tokens <= 0) {
        n_tokens = static_cast<int>(text.length() / 4) + 1;
    }
    return n_tokens;
}

JNIEXPORT void JNICALL
Java_com_example_backend_llama_LlamaCppNative_nativeGenerateStream(
    JNIEnv *env,
    jobject /* this */,
    jstring prompt_jstr,
    jfloat temperature,
    jint max_tokens,
    jobject callback
) {
    if (!g_context || !g_context->is_loaded.load() || !g_context->model || !g_context->ctx) {
        LOGE("nativeGenerateStream called but model is not loaded");
        return;
    }

    std::lock_guard<std::mutex> ctx_lock(g_context->mutex);
    g_context->is_cancelled = false;

    const char *prompt_cstr = env->GetStringUTFChars(prompt_jstr, nullptr);
    if (!prompt_cstr) {
        LOGE("Failed to get prompt string");
        return;
    }
    std::string user_prompt(prompt_cstr);
    env->ReleaseStringUTFChars(prompt_jstr, prompt_cstr);

    jclass callback_class = env->GetObjectClass(callback);
    if (!callback_class) {
        LOGE("Failed to get callback class");
        return;
    }
    jmethodID on_token_method = env->GetMethodID(callback_class, "onToken", "(Ljava/lang/String;)Z");
    if (!on_token_method) {
        LOGE("Failed to get onToken method");
        return;
    }

    const llama_vocab * vocab = g_context->vocab;
    llama_context * ctx = g_context->ctx;
    llama_model * model = g_context->model;

    // Apply chat template if available
    std::string formatted_prompt;
    const char * tmpl = llama_model_chat_template(model, nullptr);
    if (tmpl) {
        llama_chat_message msgs[1] = {
            { "user", user_prompt.c_str() }
        };
        int32_t needed_len = llama_chat_apply_template(tmpl, msgs, 1, true, nullptr, 0);
        if (needed_len > 0) {
            std::vector<char> buf(needed_len + 1);
            int32_t res = llama_chat_apply_template(tmpl, msgs, 1, true, buf.data(), buf.size());
            if (res > 0) {
                formatted_prompt = std::string(buf.data(), res);
                LOGI("Applied model chat template, formatted length: %zu", formatted_prompt.length());
            }
        }
    }

    if (formatted_prompt.empty()) {
        // Fallback standard chat formatting if no template or template failed
        formatted_prompt = "<|im_start|>user\n" + user_prompt + "<|im_end|>\n<|im_start|>assistant\n";
    }

    // Tokenize formatted prompt
    int n_prompt = -llama_tokenize(vocab, formatted_prompt.c_str(), formatted_prompt.size(), nullptr, 0, true, true);
    if (n_prompt <= 0) {
        // Fallback to raw user prompt if tokenizing formatted prompt had an issue
        formatted_prompt = user_prompt;
        n_prompt = -llama_tokenize(vocab, formatted_prompt.c_str(), formatted_prompt.size(), nullptr, 0, true, true);
    }

    if (n_prompt <= 0) {
        LOGE("Failed to tokenize prompt");
        return;
    }

    std::vector<llama_token> prompt_tokens(n_prompt);
    if (llama_tokenize(vocab, formatted_prompt.c_str(), formatted_prompt.size(), prompt_tokens.data(), prompt_tokens.size(), true, true) < 0) {
        LOGE("Failed to fill prompt tokens");
        return;
    }

    LOGI("Tokenized prompt into %d tokens. Beginning inference decode...", n_prompt);

    // Initialize sampler chain
    auto sparams = llama_sampler_chain_default_params();
    sparams.no_perf = false;
    llama_sampler * smpl = llama_sampler_chain_init(sparams);

    if (temperature > 0.05f) {
        llama_sampler_chain_add(smpl, llama_sampler_init_top_k(40));
        llama_sampler_chain_add(smpl, llama_sampler_init_top_p(0.9f, 1));
        llama_sampler_chain_add(smpl, llama_sampler_init_temp(temperature));
        llama_sampler_chain_add(smpl, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));
    } else {
        llama_sampler_chain_add(smpl, llama_sampler_init_greedy());
    }

    llama_batch_ext * batch = g_context->batch;
    if (!batch) {
        batch = llama_batch_ext_init(ctx);
        g_context->batch = batch;
    }

    // Feed prompt tokens in chunks of ctx_params.n_batch
    int32_t n_batch = 512;
    int32_t pos = 0;

    for (int32_t i = 0; i < n_prompt; i += n_batch) {
        if (g_context->is_cancelled.load()) {
            break;
        }

        int32_t n_chunk = std::min(n_batch, n_prompt - i);
        batch_set_tokens(batch, prompt_tokens.data() + i, n_chunk, pos);

        if (llama_process(ctx, LLAMA_PROCESS_TYPE_DECODE, batch) != 0) {
            LOGE("llama_process decode failed for prompt chunk at %d", i);
            llama_sampler_free(smpl);
            return;
        }

        pos += n_chunk;
    }

    if (g_context->is_cancelled.load()) {
        LOGI("Generation cancelled during prompt evaluation");
        llama_sampler_free(smpl);
        return;
    }

    // Generation loop
    int n_predict = max_tokens > 0 ? max_tokens : 1024;
    int n_decoded = 0;

    LOGI("Prompt ingested. Starting autoregressive generation (max: %d)...", n_predict);

    for (int iter = 0; iter < n_predict; ++iter) {
        if (g_context->is_cancelled.load()) {
            LOGI("Inference generation cancelled by user");
            break;
        }

        llama_token new_token_id = llama_sampler_sample(smpl, ctx, -1);

        if (llama_vocab_is_eog(vocab, new_token_id)) {
            LOGI("EOG token %d detected. Ending generation.", new_token_id);
            break;
        }

        char piece_buf[256];
        int n_piece = llama_token_to_piece(vocab, new_token_id, piece_buf, sizeof(piece_buf), 0, false);
        if (n_piece > 0) {
            std::string piece_str(piece_buf, n_piece);
            jstring token_jstr = env->NewStringUTF(piece_str.c_str());
            if (token_jstr) {
                jboolean should_continue = env->CallBooleanMethod(callback, on_token_method, token_jstr);
                env->DeleteLocalRef(token_jstr);
                if (!should_continue) {
                    LOGI("Callback requested stop");
                    break;
                }
            }
        }

        // Prepare next batch with the sampled token
        batch_set_tokens(batch, &new_token_id, 1, pos);
        pos += 1;
        n_decoded += 1;

        if (llama_process(ctx, LLAMA_PROCESS_TYPE_DECODE, batch) != 0) {
            LOGE("llama_process decode failed at step %d", iter);
            break;
        }
    }

    LOGI("Generation finished. Decoded %d tokens.", n_decoded);
    llama_sampler_free(smpl);
}

} // extern "C"
