/*
 * Copyright (c) 2011-2016 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package reactor.ipc.netty.http.client;

import java.net.InetSocketAddress;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Flux;
import reactor.ipc.netty.ByteBufFlux;
import reactor.ipc.netty.NettyInbound;
import reactor.ipc.netty.NettyState;
import reactor.ipc.netty.http.HttpInbound;
import reactor.ipc.netty.http.multipart.MultipartInbound;

/**
 * An HttpClient Reactive read contract for incoming response. It inherits several
 * accessor
 * related to HTTP
 * flow : headers, params,
 * URI, method, websocket...
 *
 * @author Stephane Maldini
 * @since 2.5
 */
public interface HttpClientResponse extends HttpInbound, NettyState {



	/**
	 * a {@literal byte[]} inbound {@link Flux}
	 *
	 * @return a {@literal byte[]} inbound {@link Flux}
	 */
	default MultipartInbound receiveMultipart() {
		HttpClientResponse thiz = this;
		return new MultipartInbound() {
			@Override
			public Channel channel() {
				return thiz.channel();
			}

			@Override
			public Flux<ByteBufFlux> receiveParts() {
				return MultipartInbound.from(thiz);
			}

			@Override
			public NettyInbound onClose(Runnable onClose) {
				return thiz.onClose(onClose);
			}

			@Override
			public NettyInbound onReadIdle(long idleTimeout, Runnable onReadIdle) {
				return thiz.onReadIdle(idleTimeout, onReadIdle);
			}

			@Override
			public boolean isDisposed() {
				return thiz.isDisposed();
			}

			@Override
			public InetSocketAddress remoteAddress() {
				return thiz.remoteAddress();
			}
		};
	}

	/**
	 * Return the previous redirections or empty array
	 *
	 * @return the previous redirections or empty array
	 */
	String[] redirectedFrom();

	/**
	 *
	 * @return
	 */
	HttpHeaders responseHeaders();

	/**
	 * @return the resolved HTTP Response Status
	 */
	HttpResponseStatus status();
}
