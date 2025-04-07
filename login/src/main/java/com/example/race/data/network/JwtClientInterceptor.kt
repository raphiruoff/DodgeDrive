package com.example.race.data.network

import io.grpc.*
import io.grpc.Metadata.ASCII_STRING_MARSHALLER

class JwtClientInterceptor(private val jwtTokenProvider: () -> String?) : ClientInterceptor {

    override fun <ReqT, RespT> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel
    ): ClientCall<ReqT, RespT> {
        val token = jwtTokenProvider()
        val headers = Metadata()
        if (token != null) {
            val key = Metadata.Key.of("Authorization", ASCII_STRING_MARSHALLER)
            headers.put(key, "Bearer $token")
        }

        return object : ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
            next.newCall(method, callOptions)
        ) {
            override fun start(responseListener: Listener<RespT>?, headersToSend: Metadata?) {
                headersToSend?.merge(headers)
                super.start(responseListener, headersToSend)
            }
        }
    }
}
