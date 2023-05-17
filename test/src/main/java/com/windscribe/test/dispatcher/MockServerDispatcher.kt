/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.test.dispatcher

import androidx.test.platform.app.InstrumentationRegistry
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import java.io.BufferedReader
import java.util.concurrent.TimeUnit

class MockServerDispatcher {
    class ResponseDispatcher : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            if (request.path == "/") {
                return createResponse("check_ip_success.txt")
            }

            if (request.path?.startsWith("/Session") == true) {
                return if (request.method == "POST") {
                    createResponse("login_success.json")
                } else {
                    createResponse("get_session_success.json")
                }
            }
            if (request.path?.startsWith("/Users") == true) {
                return if (request.method == "PUT") {
                    MockResponse()
                } else {
                    createResponse("register_success.json")
                }
            }

            if (request.path?.startsWith("/MobileBillingPlans") == true) {
                return createResponse("plans_success.json")
            }

            if (request.path?.startsWith("/Notifications") == true) {
                return createResponse("notification_success.json")
            }

            if (request.path?.startsWith("/PortMap") == true) {
                return createResponse("port_map_success.json")
            }

            if (request.path?.startsWith("/ServerConfigs") == true) {
                return createResponse("server_config_success.txt")
            }

            if (request.path?.startsWith("/ServerCredentials?type=openvpn") == true) {
                return createResponse("open_vpn_credentials_success.json")
            }

            if (request.path?.startsWith("/ServerCredentials?type=ikev2") == true) {
                return createResponse("ikev2_credentials_success.json")
            }

            if (request.path?.startsWith("/WgConfigs") == true) {
                return createResponse("wireguard_credentials_success.json")
            }

            if (request.path?.startsWith("/StaticIps") == true) {
                return createResponse("static_ip_success.json")
            }

            if (request.path?.startsWith("/serverlist") == true) {
                return createResponse("server_list_success.json")
            }

            return createResponse("error.json")
        }

        private fun createResponse(fileName: String): MockResponse {
            return MockResponse()
                .setBodyDelay(1, TimeUnit.SECONDS)
                .setResponseCode(201)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Cache-Control", "no-cache")
                .setBody(readFile(fileName))
        }

        private fun readFile(fileName: String): String {
            val inputStream = InstrumentationRegistry.getInstrumentation().context.assets.open(fileName)
            return inputStream.bufferedReader().use(BufferedReader::readText)
        }
    }

    class ApiError(
        private val error: String = "",
        private val code: Int = -1,
        private val httpError: Int = 200
    ) : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            return createApiError()
        }

        private fun createApiError(): MockResponse {
            return MockResponse()
                .setResponseCode(httpError)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Cache-Control", "no-cache")
                .setBody("{\"errorCode\": \"$code\" , \"errorMessage\": \"$error\" }")
        }
    }
}