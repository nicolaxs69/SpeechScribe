package com.theimpartialai.speechScribe.features.cloud

import android.content.Context
import android.util.Log
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.NewPasswordContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.theimpartialai.speechScribe.BuildConfig
import java.io.File

/**
 * Manages AWS S3 file uploads with Cognito authentication.
 *
 * Core responsibilities:
 * - AWS authentication via Cognito
 * - S3 file upload operations
 * - Credential management
 *
 * @property context Required for AWS operations
 */

class S3UploadManager(
    private val context: Context,
) {
    private val TAG = "S3UploadManager"
    private lateinit var transferUtility: TransferUtility

    init {
        TransferNetworkLossHandler.getInstance(context)
    }

    private val userPool = CognitoUserPool(
        context,
        BuildConfig.AWS_USER_POOL_ID,
        BuildConfig.AWS_CLIENT_ID,
        null,
        Regions.fromName(BuildConfig.AWS_REGION)
    )

    /**
     * Authenticates user with AWS Cognito
     * @param onAuthenticated Callback for successful authentication
     */
    private fun authenticatedUser(onAuthenticated: () -> Unit) {
        val username = BuildConfig.AWS_DEFAULT_USER
        val password = BuildConfig.AWS_DEFAULT_PASSWORD
        val newPassword = BuildConfig.AWS_DEFAULT_NEW_PASSWORD
        val user = userPool.getUser(username)

        user.getSessionInBackground(object : AuthenticationHandler {
            override fun onSuccess(userSession: CognitoUserSession?, newDevice: CognitoDevice?) {
                Log.d(TAG, "User authenticated successfully")
                val idToken = userSession?.idToken?.jwtToken
                initializeCredentialsProvider(idToken)
                onAuthenticated()
            }

            override fun getAuthenticationDetails(
                authenticationContinuation: AuthenticationContinuation?,
                userId: String?
            ) {
                val authenticationDetails = AuthenticationDetails(username, password, null)
                authenticationContinuation?.setAuthenticationDetails(authenticationDetails)
                authenticationContinuation?.continueTask()
            }

            override fun getMFACode(continuation: MultiFactorAuthenticationContinuation?) {
                Log.d(TAG, "MFA is not set up, continuing without MFA.")
                continuation?.continueTask()
            }

            override fun authenticationChallenge(continuation: ChallengeContinuation?) {
                if (continuation != null) {
                    val challengeName = continuation.challengeName
                    Log.d(TAG, "Authentication challenge: $challengeName")

                    when (challengeName) {
                        "NEW_PASSWORD_REQUIRED" -> {
                            val newPasswordContinuation = continuation as NewPasswordContinuation
                            newPasswordContinuation.setPassword(newPassword)
                            newPasswordContinuation.continueTask()
                        }

                        else -> {
                            continuation.continueTask()
                        }
                    }
                } else {
                    Log.e(TAG, "ChallengeContinuation is null")
                }
            }

            override fun onFailure(exception: java.lang.Exception?) {
                Log.e(TAG, "Authentication failed: ${exception?.localizedMessage}")
            }
        })
    }

    /**
     * Initializes AWS credentials using Cognito token
     * @param idToken JWT token from successful authentication
     */
    private fun initializeCredentialsProvider(idToken: String?) {
        val providerName =
            "cognito-idp.${BuildConfig.AWS_REGION}.amazonaws.com/${BuildConfig.AWS_USER_POOL_ID}"
        val logins = mapOf(
            providerName to idToken
        )

        val credentialsProvider = CognitoCachingCredentialsProvider(
            context,
            BuildConfig.AWS_IDENTITY_POOL_ID,
            Regions.fromName(BuildConfig.AWS_REGION)
        )

        credentialsProvider.logins = logins

        val s3Client = AmazonS3Client(credentialsProvider, Region.getRegion(Regions.US_EAST_2))

        transferUtility = TransferUtility.builder()
            .context(context)
            .s3Client(s3Client)
            .build()
    }

    /**
     * Uploads file to S3 bucket
     * @param filePath Local path to file
     * @param bucketName Target S3 bucket
     * @return TransferObserver to monitor upload
     */
    fun uploadFileToS3(file: File) {
        authenticatedUser {

            val uploadObserver = transferUtility.upload(
                BuildConfig.AWS_BUCKET_NAME,
                file.name,
                file
            )

            uploadObserver.setTransferListener(object : TransferListener {
                override fun onStateChanged(id: Int, state: TransferState?) {
                    when (state) {
                        TransferState.COMPLETED -> {
                            Log.d(TAG, "Upload completed.")
                        }

                        TransferState.FAILED -> {
                            Log.e(TAG, "Upload failed.")
                        }

                        else -> {}
                    }
                }

                override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                    val percentDone =
                        ((bytesCurrent.toDouble() / bytesTotal.toDouble()) * 100.0).toInt()
                    Log.d(TAG, "Upload progress: $percentDone%")
                }

                override fun onError(id: Int, ex: Exception?) {
                    Log.e(TAG, "Error during upload: ", ex)
                }
            })
        }
    }
}