#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL

Java_com_example_nativecr_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_nativecr_MainActivity_process(
        JNIEnv *env,
        jobject mainActivityInstance /* this */) {

    const jclass mainActivityClass = env->GetObjectClass(mainActivityInstance);
    const jmethodID jmethodId = env->GetMethodID(mainActivityClass, "processInKotlin",
                                                 "()Ljava/lang/String;");

    if (jmethodId == nullptr) {
        return env->NewStringUTF("Method not found");
    }

    const jobject result = env->CallObjectMethod(mainActivityInstance, jmethodId);
    const std::string java_msg = env->GetStringUTFChars((jstring) result, JNI_FALSE);
    const std::string c_msg = "Result from Kotlin: ";
    const std::string msg = c_msg + java_msg;


    return env->NewStringUTF(msg.c_str());
}
