/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class org_cef_network_CefPostData_N */

#ifndef _Included_org_cef_network_CefPostData_N
#define _Included_org_cef_network_CefPostData_N
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     org_cef_network_CefPostData_N
 * Method:    N_Create
 * Signature: ()Lorg/cef/network/CefPostData_N;
 */
JNIEXPORT jobject JNICALL Java_org_cef_network_CefPostData_1N_N_1Create
  (JNIEnv *, jclass);

/*
 * Class:     org_cef_network_CefPostData_N
 * Method:    N_Dispose
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_cef_network_CefPostData_1N_N_1Dispose
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_cef_network_CefPostData_N
 * Method:    N_IsReadOnly
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_org_cef_network_CefPostData_1N_N_1IsReadOnly
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_cef_network_CefPostData_N
 * Method:    N_GetElementCount
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_org_cef_network_CefPostData_1N_N_1GetElementCount
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_cef_network_CefPostData_N
 * Method:    N_GetElements
 * Signature: (JLjava/util/Vector;)V
 */
JNIEXPORT void JNICALL Java_org_cef_network_CefPostData_1N_N_1GetElements
  (JNIEnv *, jobject, jlong, jobject);

/*
 * Class:     org_cef_network_CefPostData_N
 * Method:    N_RemoveElement
 * Signature: (JLorg/cef/network/CefPostDataElement;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_cef_network_CefPostData_1N_N_1RemoveElement
  (JNIEnv *, jobject, jlong, jobject);

/*
 * Class:     org_cef_network_CefPostData_N
 * Method:    N_AddElement
 * Signature: (JLorg/cef/network/CefPostDataElement;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_cef_network_CefPostData_1N_N_1AddElement
  (JNIEnv *, jobject, jlong, jobject);

/*
 * Class:     org_cef_network_CefPostData_N
 * Method:    N_RemoveElements
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_cef_network_CefPostData_1N_N_1RemoveElements
  (JNIEnv *, jobject, jlong);

#ifdef __cplusplus
}
#endif
#endif
